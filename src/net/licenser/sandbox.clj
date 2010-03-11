(ns net.licenser.sandbox
  (:require [clojure.contrib.seq-utils :as su]))



(def 
 #^{:doc "Default timeout for the sandbox. It can be altert in the sandbox creators."}
 *default-sandbox-timeout* 5000)

; Thanks to hiredman for this code snippet!
(defn s-seq 
  "Convertns a form into a sequence."
  [form]
  (tree-seq #(coll? %) #(let [a (macroexpand %)] (or (and (coll? a) (seq a)) (list a))) form))

; Weeh thanks to bsteuber for advice on resolve!
(defn fn-seq 
  "Converst a form into a sequence of functions."
  [form]
  (remove nil? (map (fn [s]
	 (if (some (partial = s) '(fn* let* def loop*))
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
    (if (= (type form) clojure.lang.Var)
      (map (partial = (ns-name (:ns (meta form)))) namespaces)
      '())))

(defn combine-tests
  "Combines two testers."
     [& tests]
     (fn [form]
      (map #(% form) tests)))

(defn whitelist
  "Creates a whitelist of testers. Testers take a var and unless they return true the test will fail."
  ([test & tests]
     {:type :whitelist
      :tests (apply combine-tests test tests)})
  ([test]
      {:type :whitelist
      :tests test}))

(defn blacklist
  "Creates a blacklist of testers. Testers take a var and if they return true the blacklist will fail the test."
  ([test & tests]
     {:type :blacklist
      :tests (apply combine-tests test tests)})
  ([test]
      {:type :blacklist
      :tests test}))

(def variable-functions
     (function-tester 'def))

(def general-functions
     (function-tester '= '== 'case 'if 'comment 'complement 'let 'constantly 'do 'loop* 'lop 'let* 'recur 'fn* 'fn? 'hash 'identical? 'macroexpand 'name 'not= 'partial 'trampoline))

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
      (function-tester '* '/ '+ '- '< '> '<= '>= 'compare 'dec 'even? 'inc 'max 'min 'mod 'neg? 'odd? 'pos? 'quot 'rand 'rand-int 'rem 'zero?) 
      (namespace-tester 'clojure.contrib.math)))

(def list-functions
     (combine-tests 
      (function-tester 'map 'reduce 'count 'doall 'dorun 'doseq)
      (function-tester 'conj 'concat 'cons  'cycle 'interleave 'interpose 'into  'partition 'reverse 'rseq 'seq 'sequence 'sort 'sort-by 'split-at 'split-with 'subseq 'subvec 'tree-seq 'zipmap)
      (function-tester 'vec 'vector 'vector?)
      (function-tester 'distinct 'drop 'drop-last 'drop-while 'empty 'filter 'not-any? 'not-empty 'not-every? 'remove 'replace )
      (function-tester 'repeat 'iterate 'list 'list* 'repeatedly 'replicate 'range)
      (function-tester 'ffirst 'first 'fnext 'last 'next 'nfirst 'nnext 'nth 'nthnext 'peek 'pop 'rest 'second 'take 'take-last 'take-nth 'take-while)
      (function-tester 'contains? 'counted? 'empty? 'every? 'reversible? 'seq? 'some 'sorted?)))


(def set-functions
     (function-tester 'disj 'dissoc 'assoc 'find 'get 'get-in 'hash-set 'hash-map 'key 'keys 'merge 'merge-with 'select-keys 'set 'set? 'update-in 'sorted-map 'sorted-map-by 'sorted-set))


(defn new-sandbox-tester
  "Creates a new tester combined from a set of black and whitelists."
  [& definitions]
  (let [{wl :whitelist bl :blacklist} (reduce #(assoc %1 (:type %2) (conj (get %1 (:type %2)) (:tests %2))) {} definitions)]
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
 #^{:doc "A tester that should cover most of the basic functions which seem undangerouse enough - at least I think so. No guarnatee for that!"}
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

(def 
 #^{:doc "A tester that passes everything, convinent for trying and debugging but not secure."} 
 debug-tester (constantly true))

(def *default-sandbox-tester* secure-tester)

(defn create-sandbox-compiler
  "Creates a sandbox that returns rerunable code, you can pass locals which will be passed to the 'compiled' function in the same order as defined before 'compilation'."
  ([nspace tester timeout context]
     (binding [*ns* (create-ns nspace)]
       (refer 'clojure.core))
     (fn [code & locals]
	 (let [form (read-string code)]
	   (if (tester form nspace)
	     (binding [*ns* (create-ns nspace)]
	       (dorun (map (partial ns-unmap nspace) locals))
     	       (dorun (map (partial intern nspace) locals))
	       (fn [bindings & values]
		 (dorun (map (partial intern nspace) locals values))
		 (sandbox 
		  (fn []
		    (let [f (future 
			     (let [] 
			       (push-thread-bindings 
				(assoc (apply hash-map 
					      (su/flatten 
					       (map 
						(fn [[k v]] 
						  [(resolve k) v]) 
						(seq bindings))))
				  (var *ns*) (create-ns nspace)))
			       (try 
				(eval form)
				(finally (pop-thread-bindings)))))]
		      (.get f timeout java.util.concurrent.TimeUnit/MILLISECONDS)))
		  context)))
	   (throw (SecurityException. "Code did not pass sandbox guidelines"))))))
  ([nspace tester timeout]
     (create-sandbox-compiler nspace tester timeout (context (domain (empty-perms-list)))))
  ([nspace tester]
     (create-sandbox-compiler nspace tester *default-sandbox-timeout*))
  ([nspace]
     (create-sandbox-compiler nspace *default-sandbox-tester*))
  ([]
     (create-sandbox-compiler (gensym "net.licenser.sandbox"))))    

(defn create-sandbox
  "Creates a sandbox that evaluates the code string that it gets passed."
  ([nspace tester timeout context]
       (fn [code]
	 (let [form (read-string code)]
	   (if (tester form nspace)
	     (sandbox (fn []
			  (let [f (future (eval form))]
			    (.get f timeout java.util.concurrent.TimeUnit/MILLISECONDS)))
			context)
	   (throw (SecurityException. "Code did not pass sandbox guidelines"))))))
  ([nspace tester timeout]
     (create-sandbox nspace tester timeout (context (domain (empty-perms-list)))))
  ([nspace tester]
     (create-sandbox nspace tester *default-sandbox-timeout*))
  ([nspace]
     (create-sandbox nspace *default-sandbox-tester*))
  ([]
     (create-sandbox (gensym "net.licenser.sandbox.box"))))
