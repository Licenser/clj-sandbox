(ns net.licenser.sandbox
  (:use [clojure.contrib.def :only [defnk]]
	(net.licenser.sandbox matcher safe-fns tester jvm)
	[clojure.contrib.seq-utils :only [flatten]])
  (:import [java.util.concurrent FutureTask TimeUnit TimeoutException ExecutionException]))

(def 
 #^{:doc "Default timeout for the sandbox. It can be changed by the sandbox creators."}
 *default-sandbox-timeout* 5000)

(defn tree-map [mapper branch? children root]
  (let [r (lazy-seq (map (fn zmap-mapper [e] (if (branch? e) (tree-map mapper branch? children (children e)) (mapper e))) root))]
    (cond 
     (vector? root)
     (vec r)
     (associative? root)
     (reduce (fn [m [k v]] (assoc m k v)) {} r)
     (set? root)
     (set r)
     :else 
     r)))

(declare dot)

(defn dot-maker [obj-tester] 
  (fn dot [object method & args]
    (if (obj-tester object method)
      (if (instance? java.lang.Class object)
       (clojure.lang.Reflector/invokeStaticMethod object method (to-array args))
       (clojure.lang.Reflector/invokeInstanceMethod object method (to-array args)))
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
		 (throw (TimeoutException. "Execution timed out.")))
	  (catch ExecutionException e
	    (.cancel task true)
	    (.stop thr (Exception. "Thread stopped!")) 
	    (throw (SecurityException. "Exception in sandboxed code." (.getCause e))))
	  (catch Exception e
	    (.cancel task true)
	    (.stop thr (Exception. "Thread stopped!")) 
	    (throw (SecurityException. "Exception in sandboxed code." e))))))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn enable-security-manager []
      (System/setSecurityManager (SecurityManager.)))

(def 
 #^{:doc 
"A tester that should cover most of the basic functions that seem 
non-dangerous enough - at least we think so. No promises!"}
 secure-tester (->> safe-functions vals (remove nil?) (map whitelist) (apply new-tester)))


(def default-obj-tester
 #^{:doc 
"A tester that should cover most of the basic objects that seem 
non-dangerous enough - at least we think so. No promises!
Also some objects that are known to be dangerous."}
 (apply new-object-tester
	(concat
	 (map whitelist (vals save-objects))
	 (map blacklist (vals bad-objects)))))


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
	(if (and (seq? m) (empty? args))
	  (concat (list '. obj (str (first m))) (rest m))
	  (concat (list '. obj (str m)) args)))
      f)))

(defn dot-replace [form]
  (if (coll? form)
    (tree-map #(if (= % '.) 'dot %) coll?
      expand-and-quote
      (expand-and-quote form))
    form))


(def state-tester
     (new-tester (whitelist (constantly '(true)))
		 (blacklist (function-matcher 'def 'def* 
					      'ensure 'ref-set 'alter 'commute 
					      'swap! 'compare-and-set! ))))

(defn has-state? [form]
     (not (state-tester form nil)))


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
  standard output.
  If :remember-state >= 1 the namespace will be torn down with every time
  the code is executed and rebuild with the history of state changing
  functions. State changing means having def, def*, alter, swap! or dosync
  in it."
  [:namespace (gensym "net.licenser.sandbox.box")
   :tester secure-tester
   :timeout *default-sandbox-timeout*
   :context (-> (empty-perms-list) domain context)
   :object-tester default-obj-tester
   :initial []
   :remember-state 0]
  (if (and
       (= "1.2.0" (clojure-version))
       (not (empty? initial)))
    (binding [*out* *err*]
      (println "WARNING: Clojure 1.2 introduced behaviour that breams the :initial functionality, we are working on a fix but at this moment it is NOT working properly!")))
  (let [history (atom [])]
    (fn sandbox-compiler [form & locals]
      (let [form (dot-replace form)]
	(if (tester form namespace)
	  (fn sandbox-entry-point
	    ([bindings & values]
	       (binding [*ns* (create-ns namespace)]
		 (refer 'clojure.core)
		 (dorun (map (partial intern namespace) locals values))
		 (thunk-timeout 
		  (fn timeout-box [] 
		    (sandbox 
		     (fn sandboxed-code []
		       (push-thread-bindings 
			(assoc (apply hash-map 
				      (flatten 
				       (map (fn jvm-sandbox-runable-code [[k v]]  [(resolve k) v]) (seq bindings))))
			  (var *ns*) (create-ns namespace)))
		       (if (or (not (empty? initial))
			       (not (zero? remember-state)))
			 (do
			   (binding [*ns* (create-ns namespace)]
			     (refer 'clojure.core))
			   (doseq [d (concat initial @history)]
			     (try
			       (binding [*read-eval* false *ns* (find-ns namespace) dot (dot-maker object-tester)]
				 (eval '(def dot net.licenser.sandbox/dot)) (eval d))
			       (catch Exception e
				 (swap! history #(remove (partial = d) %)))))))
		       (try
			 (let [r 
			       (binding [*read-eval* false
					 *ns* (find-ns namespace)
					 dot (dot-maker object-tester)]
				 (eval '(def dot net.licenser.sandbox/dot)) 
				 (eval form))]
			   (if (and  (not (zero? remember-state)) (has-state? form))
			     (do
			       (if (>= (count @history) remember-state)
				 (swap! history #(conj (rest %) form))
				 (swap! history conj form))
			       (remove-ns namespace)))
			   (if (coll? r) (doall r) r))
			 (finally (pop-thread-bindings)))) context)) timeout)))
	    ([] (sandbox-entry-point {})))
	  (throw (SecurityException. (str "Code did not pass sandbox guidelines: " (pr-str (find-bad-forms tester namespace form))))))))))
  

(defnk new-sandbox
  "Creates a sandbox that evaluates the code string that it gets passed.
   The given namespace will be keeped unless :remember-state is given a
   number greater then zero. If :remember-state >= 1 the namespace will
   be torn down for every call of the sandbox, but functions like def,
   alter, dosync will be 'rememberd' and rerun on the next sandbox call.
   Only remember-state commands are keeped in history, if one command in
   the history causes an exception it is removed. If initial is given it
   is run prior to any code on each run."
  [:namespace (gensym "net.licenser.sandbox.box")
   :tester secure-tester
   :timeout *default-sandbox-timeout*
   :context (-> (empty-perms-list) domain context)
   :object-tester default-obj-tester
   :initial []
   :remember-state 0]
  (if (and
       (= "1.2.0" (clojure-version))
       (not (empty? initial)))
    (binding [*out* *err*]
      (println "WARNING: Clojure 1.2 introduced behaviour that breams the :initial functionality, we are working on a fix but at this moment it is NOT working properly!")))
  (let [history (atom [])]
    (fn sandbox-executor [form]
      (let [form (dot-replace form)]
	(if (tester form namespace)
	  (thunk-timeout 
	   (fn timeout-box [] 
	     (sandbox 
	      (fn sandbox-jvm-runnable-code []
		(if (or (not (empty? initial))
			(not (zero? remember-state)))
		  (do
		    (binding [*ns* (create-ns namespace)]
			(refer 'clojure.core))
		    (doseq [d (concat initial @history)]
		      (try
			(let [r (binding [*read-eval* false *ns* (find-ns namespace) dot (dot-maker object-tester)] (eval '(def dot net.licenser.sandbox/dot)) (eval d))]
			  (if (coll? r) (doall r) r))
			(catch Exception e
			  (swap! history #(remove (partial = d) %)))))))
		(let [r (binding [*read-eval* false *ns* (create-ns namespace) dot (dot-maker object-tester)] (refer 'clojure.core) (eval '(def dot net.licenser.sandbox/dot)) (eval form))]
		  (if (and  (not (zero? remember-state)) (has-state? form))
		    (do
		      (if (>= (count @history) remember-state)
			(swap! history #(conj (rest %) form))
			(swap! history conj form))
		      (remove-ns namespace)))
		    (if (coll? r) (doall r) r))) context)) timeout)
	  (throw (SecurityException. (str "Code did not pass sandbox guidelines:" (pr-str (find-bad-forms tester namespace form))))))))))