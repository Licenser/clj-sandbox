(ns net.licenser.sandbox
  (:use [clojure.contrib.def :only [defnk]]
	(net.licenser.sandbox matcher safe-fns tester jvm))
  (:require [clojure.contrib.seq-utils :as su])
  (:import (java.util.concurrent FutureTask TimeUnit TimeoutException)))

(def 
 #^{:doc "Default timeout for the sandbox. It can be changed by the sandbox creators."}
 *default-sandbox-timeout* 5000)

(def default-obj-tester
  (new-object-tester
    (new-tester
      (whitelist (class-matcher java.lang.String)))))

(defn tree-map [mapper branch? children root]
  (lazy-seq (map (fn zmap-mapper [e] (if (branch? e) (tree-map mapper branch? children (children e)) (mapper e))) root)))

(declare dot)

(defn find-method [obj method args]
  (.getMethod (class obj) method (into-array (map class args))))

(defn dot-maker [obj-tester] 
  (fn dot [object method & args]
    (if (obj-tester object method)
      (.invoke (find-method object method args) object (to-array args))
      (throw (SecurityException. (str "Tried to call: " method " on " object " which is not allowed."))))))

;;;;;;;; Thanks to hiredman's and  Chousuke as I get it right for this piece of code.
;;;;;;;; Sadly it seems future does not work as a timeout
(defn thunk-timeout [thunk seconds]
      (let [task (FutureTask. thunk)
            thr (Thread. task)]
        (try
          (.start thr)
          (.get task seconds TimeUnit/MILLISECONDS)
          (catch TimeoutException e
                 (.cancel task true)
                 (.stop thr (Exception. "Thread stopped!")) 
		 (throw (TimeoutException. "Execution Timed Out"))))))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn enable-security-manager []
      (System/setSecurityManager (SecurityManager.)))

(def 
 #^{:doc "A tester that should cover most of the basic functions that seem 
          non-dangerous enough - at least I think so. No promises!"}
 secure-tester (->> safe-functions vals (remove nil?) (map whitelist) (apply new-tester)))

(defn add-reader-to-sandbox
  "This function can be used to tell a sandbox how to conuse the code that it
  passed to it.

  Usage: (add-reader-to-sandbox sandbox read-string)"
  [sandbox reader-fn]
  (fn [code & params]
    (binding [*read-eval* false]
      (apply sandbox (reader-fn code) params))))

(defn stringify-sandbox
  "This function can be used to make a sandbox take strings instead of the code form.

  Usage: (stringify-sandbox (new-sandbox-compiler))"
  [sandbox]
  (add-reader-to-sandbox sandbox read-string))

(defn expand-and-quote [form]
  (let [f (macroexpand form)]
    (if (= (first f) '.)
      (let [[_ obj m & args] f]
        (concat (list '. obj (str m)) args))
      f)))

(defn dot-replace [form]
  (if (coll? form)
    (tree-map #(if (= % '.) 'dot %) coll?
      expand-and-quote
      (expand-and-quote form))
    form))

(defnk new-sandbox-compiler
  "Creates a sandbox that returns rerunable code. You can pass locals 
   which will be passed to the 'compiled' function in the same order 
   as defined before 'compilation'. The compiled code is a function that 
   takes one or more parameters. The first parameter is a hash map of 
   bindings like {'*out* my-writer} that will beind within the execution.
   Every following value is mapped to a local value given at compile time 
   in the same order so:
  (def my-writer (java.io.StringWriter.))
  (def code (compiler \"(println a)\" a))
  (code {'*out my-writer} 1) ; will write 1 into my-writer instead of the
  standard output."
  [:namespace (gensym "net.licenser.sandbox.box")
   :tester secure-tester
   :timeout *default-sandbox-timeout*
   :context (-> (empty-perms-list) domain context)
   :object-tester default-obj-tester]
  (binding [*ns* (create-ns namespace)]
       (refer 'clojure.core))
     (fn sandbox-compiler [form & locals]
       (let [form (dot-replace form)]
         (if (tester form namespace)
	   (binding [*ns* (create-ns namespace)]
	     (dorun (map (partial ns-unmap namespace) locals))
	     (dorun (map (partial intern namespace) locals))
	     (fn sandbox-entry-point
	       ([bindings & values]
		  (dorun (map (partial intern namespace) locals values))
		  (thunk-timeout 
		   (fn timeout-box [] 
		     (sandbox 
		      (fn sandboxed-code []
			(let [] 
			  (push-thread-bindings 
			   (assoc (apply hash-map 
					 (su/flatten 
					  (map 
					   (fn jvm-sandbox-runable-code [[k v]] 
					     [(resolve k) v]) 
					   (seq bindings))))
			     (var *ns*) (create-ns namespace)))
			  (try 
			   (let [r 
				 (binding [*read-eval* false
					   *ns* (create-ns namespace)
					   dot (dot-maker object-tester)]
				   (eval '(def dot net.licenser.sandbox/dot)) 
				   (eval form))]
			     (if (coll? r) (doall r) r))
			   (finally (pop-thread-bindings))))) context)) timeout))
	       ([] (sandbox-entry-point {}))))
	   (throw (SecurityException. (str "Code did not pass sandbox guidelines: " (pr-str (find-bad-forms tester namespace form)))))))))
     

(defnk new-sandbox
  "Creates a sandbox that evaluates the code string that it gets passed."
  [:namespace (gensym "net.licenser.sandbox.box")
   :tester secure-tester
   :timeout *default-sandbox-timeout*
   :context (-> (empty-perms-list) domain context)
   :object-tester default-obj-tester]
  (fn sandbox-executor [form]
    (let [form (dot-replace form)]
      (if (tester form namespace)
	(thunk-timeout 
	 (fn timeout-box [] 
	   (sandbox 
	    (fn sandbox-jvm-runnable-code []
	      (let [r (binding [*read-eval* false *ns* (create-ns namespace) dot (dot-maker object-tester)] (refer 'clojure.core) (eval '(def dot net.licenser.sandbox/dot)) (eval form))]
		(if (coll? r) (doall r) r))) context)) timeout)
	(throw (SecurityException. (str "Code did not pass sandbox guidelines:" (pr-str (find-bad-forms tester namespace form)))))))))
