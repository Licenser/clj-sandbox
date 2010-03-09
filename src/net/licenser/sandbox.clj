(ns net.licenser.sandbox
  (:require [clojure.contrib.seq-utils :as su]))


; Thanks to hiredman for this code snippet!
(defn s-seq 
  "Convertns a form into a sequence."
  [form]
  (tree-seq seq? #(let [a (macroexpand %)] (or (and (seq? a) a) (list a))) form))


(defn fn-seq 
  "Converst a form into a sequence of functions."
  [form]
  (filter ifn? (s-seq form)))

(defn function-tester
  "Creates a tester that whitelists functions."
  [& functions]
  (fn [form]
    (some true? (map (partial = form) functions))))

(defn namespace-tester
  "Creates a tester that whitelists functions."
  [& namespaces]
  (fn [form]
    (if (namespace form)
      (map (partial = (symbol (namespace form))) namespaces)
      (map (partial = (namespace form)) namespaces))))

(defn combine-tests
     [& tests]
     (fn [form]
      (some true? (su/flatten (map #(% form) tests)))))

(defn whitelist
  ([test & tests]
     {:type :whitelist
      :tests (apply combine-tests test tests)})
  ([test]
      {:type :whitelist
      :tests test}))

(defn blacklist
  ([test & tests]
     {:type :blacklist
      :tests (apply combine-tests test tests)})
  ([test]
      {:type :blacklist
      :tests test}))

(def variable-functions
     (function-tester 'def))

(def general-functions
     (function-tester '= '== 'case 'if 'comment 'complement 'let 'constantly 'do 'loop*  'lop 'let* 'recur 'fn* 'fn? 'hash 'identical? 'macroexpand 'name 'not= 'partial 'trampoline))

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
  [& definitions]
  (let [{wl :whitelist bl :blacklist} (reduce #(assoc %1 (:type %2) (conj (get %1 (:type %2)) (:tests %2))) {} definitions)]
    (fn [form]
      (every?
       true? 
       (map 
	(fn [f] 
	  (and  
	   (some true? (map #(% f) wl))
	   (not (some true? (map #(% f) bl)))
	   )) (fn-seq form))))))