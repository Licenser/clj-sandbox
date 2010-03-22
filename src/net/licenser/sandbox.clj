(ns net.licenser.sandbox
  (:use [clojure.contrib.def :only [defnk]])
  (:require [clojure.contrib.seq-utils :as su])
  (:import (java.util.concurrent FutureTask TimeUnit TimeoutException)))

(def 
 #^{:doc "Default timeout for the sandbox. It can be changed by the sandbox creators."}
 *default-sandbox-timeout* 5000)

; Thanks to hiredman for this code snippet!
(defn s-seq 
  "Convertns a form into a sequence."
  [form]
  (tree-seq #(and (coll? %) (not (empty? %))) 
	    #(let [a (macroexpand %)] 
	       (or (and (coll? a) 
			(seq a)) 
		   (list a))) 
	    form))

; Weeh thanks to bsteuber for advice on resolve!
(defn fn-seq 
  "Converts a form into a sequence of functions."
  [form]
  (remove nil? (map (fn [s]
	 (if (some (partial = s) '(fn* let* def loop* recur new))
	   s
	   (resolve s)))
       (filter symbol? (s-seq form)))))

(defn function-tester
  "Creates a tester that whitelists functions."
  [& functions]
  (fn [form]
    (if (= (type form) clojure.lang.Var)
      (map (partial = (:name (meta form))) functions)
      (map (partial = form) functions))))
  

(defn namespace-tester
  "Creates a tester that whitelists all functions within a namespace."
  [& namespaces]
  (fn [form]
    (cond
     (= (type form) clojure.lang.Var)
       (map (partial = (ns-name (:ns (meta form)))) namespaces)
     (= (type form) java.lang.Class)
       (map (partial = (symbol (second (re-find #"^class (.*)\.\w+$" (str form))))) namespaces)
     true
      '())))

 (defn class-tester
   "Creates a tester than whitelists a Java class."
   [& classes]
   (fn [form]
     (if (= (type form) java.lang.Class)
       (map (partial isa? form) classes)
       '())))

(defn combine-tests
  "Combines two testers."
     [& tests]
     (fn [form]
      (map #(% form) tests)))

(defn whitelist
  "Creates a whitelist of testers. Testers take a var and unless 
  they return true the test will fail."
  ([test & tests]
     {:type :whitelist
      :tests (apply combine-tests test tests)})
  ([test]
      {:type :whitelist
      :tests test}))

(defn blacklist
  "Creates a blacklist of testers. Testers take a var and if they 
  return true the blacklist will fail the test."
  ([test & tests]
     {:type :blacklist
      :tests (apply combine-tests test tests)})
  ([test]
      {:type :blacklist
      :tests test}))

(def variable-functions
     (function-tester 'def))

(def general-functions
     (function-tester '= '== 'case 'if 'comment 'complement 'let 'constantly 'do 
		      'loop* 'loop 'let* 'recur 'fn* 'fn? 'hash 'identical? 'macroexpand 
		      'name 'not= 'partial 'trampoline 'new))

(def string-functions
     (function-tester 'subs 'str))

(def regexp-functions
     (function-tester 're-find 're-groups 're-matcher 're-matches 're-pattern 're-seq))

(def meta-functions
     (function-tester 'meta 'with-meta))

(def logic-functions
     (function-tester 'and 'or 'false? 'not 'true?))

(def math-functions 
     (combine-tests 
      (function-tester '* '/ '+ '- '< '> '<= '>= 'compare 'dec 'even? 'inc 'max 'min 
		       'mod 'neg? 'odd? 'pos? 'quot 'rand 'rand-int 'rem 'zero?) 
      (namespace-tester 'clojure.contrib.math)))

(def list-functions
     (combine-tests 
      (function-tester 'map 'reduce 'count 'doall 'dorun 'doseq)
      (function-tester 'conj 'concat 'cons  'cycle 'interleave 'interpose 'into  
		       'partition 'reverse 'rseq 'seq 'sequence 'sort 'sort-by 
		       'split-at 'split-with 'subseq 'subvec 'tree-seq 'zipmap)
      (function-tester 'vec 'vector 'vector?)
      (function-tester 'distinct 'drop 'drop-last 'drop-while 'empty 'filter 'not-any? 
		       'not-empty 'not-every? 'remove 'replace )
      (function-tester 'repeat 'iterate 'list 'list* 'repeatedly 'replicate 'range)
      (function-tester 'ffirst 'first 'fnext 'last 'next 'nfirst 'nnext 'nth 'nthnext 
		       'peek 'pop 'rest 'second 'take 'take-last 'take-nth 'take-while)
      (function-tester 'contains? 'counted? 'empty? 'every? 'reversible? 'seq? 'some 'sorted?)))


(def set-functions
     (function-tester 'disj 'dissoc 'assoc 'find 'get 'get-in 'hash-set 'hash-map 
		      'key 'keys 'merge 'merge-with 'select-keys 'set 'set? 'update-in 
		      'sorted-map 'sorted-map-by 'sorted-set))


(defn new-sandbox-tester
  "Creates a new tester combined from a set of black and whitelists."
  [& definitions]
  (let [{wl :whitelist bl :blacklist} (reduce #(assoc %1 (:type %2) 
						      (conj (get %1 (:type %2)) 
							    (:tests %2))) {} definitions)]
    (fn [form nspace]
      (every?
       true? 
       (map 
	(fn [f] 
	  (and  
	   (some true? (su/flatten (map #(% f) (conj wl (namespace-tester nspace)))))
	   (not (some true? (su/flatten (map #(% f) bl))))))
	(fn-seq form))))))

(def 
 #^{:doc "A tester that should cover most of the basic functions that seem 
          non-dangerouse enough - at least I think so. No promises!"}
 secure-tester
     (new-sandbox-tester 
      (whitelist general-functions)
      (whitelist math-functions)
      (whitelist list-functions)
      (whitelist set-functions)
      (whitelist logic-functions)
      (whitelist string-functions)
      (whitelist regexp-functions)))


; Java Sandbox Stuff, this is from the clojurebot code
; http://github.com/hiredman/clojurebot/blob/master/src/hiredman/sandbox.clj

(defn enable-security-manager []
      (System/setSecurityManager (SecurityManager.)))

(defn empty-perms-list []
      (doto (java.security.Permissions.)
        (.add (RuntimePermission. "accessDeclaredMembers"))))


(defn domain [perms]
     (java.security.ProtectionDomain.
       (java.security.CodeSource. nil
                                  (cast java.security.cert.Certificate nil))
       perms))
 
(defn context [dom]
      (java.security.AccessControlContext. (into-array [dom])))
 
(defn priv-action [thunk]
      (proxy [java.security.PrivilegedAction] [] (run [] (thunk))))
 
(defn sandbox
  [thunk context]
      (java.security.AccessController/doPrivileged
        (priv-action thunk)
        context))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def 
 #^{:doc "A tester that passes everything, convinent for trying and debugging but 
         not secure."} 
 debug-tester (constantly true))

(def *default-sandbox-tester* secure-tester)


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

(defnk create-sandbox-compiler
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
   :tester *default-sandbox-tester*
   :timeout *default-sandbox-timeout*
   :context (-> (empty-perms-list) domain context)]
  (binding [*ns* (create-ns namespace)]
       (refer 'clojure.core))
     (fn sandbox-compiler [code & locals]
	 (let [form (read-string code)]
	   (if (tester form namespace)
	     (binding [*ns* (create-ns namespace)]
	       (dorun (map (partial ns-unmap namespace) locals))
     	       (dorun (map (partial intern namespace) locals))
	       (fn [bindings & values]
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
			  (let [r (eval form)]
			    (if (coll? r) (doall r) r))
			  (finally (pop-thread-bindings))))) context)) timeout)))
	     (throw (SecurityException. "Code did not pass sandbox guidelines"))))))

(defnk create-sandbox
  "Creates a sandbox that evaluates the code string that it gets passed."
  [:namespace (gensym "net.licenser.sandbox.box")
   :tester *default-sandbox-tester*
   :timeout *default-sandbox-timeout*
   :context (-> (empty-perms-list) domain context)]
  (fn sandbox-executor [code]
    (let [form (read-string code)]
      (if (tester form namespace)
	(thunk-timeout 
	 (fn timeout-box [] 
	   (sandbox 
	    (fn sandbox-jvm-runnable-code []
	      (let [r (eval form)]
		(if (coll? r) (doall r) r))) context)) timeout)
	(throw (SecurityException. "Code did not pass sandbox guidelines"))))))
