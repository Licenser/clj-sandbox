(ns net.licenser.sandbox.tester
  (:use (net.licenser.sandbox matcher safe-fns))
  (:require [clojure.contrib.seq-utils :as su]
	    [clojure.set :as set]))

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
   (if (some (partial = s) '(fn* let* def loop* recur new .))
     s
     (resolve s)))
       (filter symbol? (s-seq form)))))


  
(defn whitelist
  "Creates a whitelist of testers. Testers take a var and unless 
  they return true the test will fail."
  ([test & tests]
     {:type :whitelist
      :tests (apply combine-matchers test tests)})
  ([test]
      {:type :whitelist
      :tests test}))

(defn blacklist
  "Creates a blacklist of testers. Testers take a var and if they 
  return true the blacklist will fail the test."
  ([test & tests]
     {:type :blacklist
      :tests (apply combine-matchers test tests)})
  ([test]
      {:type :blacklist
      :tests test}))


(defn run-list [l t]
  (su/flatten (map #(% t) l)))

(defn new-tester
  "Creates a new tester combined from a set of black and whitelists. 
  Usage: (new-tester (whitelist (function-matcher 'println)))
  This returns a tester that takes 2 arguments a function, and a namespace."
  [& definitions]
  (let [{wl :whitelist bl :blacklist} (reduce #(assoc %1 (:type %2) 
                  (conj (get %1 (:type %2)) 
                  (:tests %2))) {} definitions)]
    (fn 
      ([]
   definitions)
      ([form nspace]
   (let [forms (if (= (type form) clojure.lang.Var) (list form) (fn-seq form))]
     (if (empty? forms)
       true
       (let [r (map 
                (fn [f] 
                  (and  
                   (some true? (run-list f (conj wl (namespace-matcher nspace))))
                   (not-any? true? (run-list f bl))))
                forms)]
         (and (not (empty? r)) (every? true? r)))))))))

(defn new-object-tester
  "Creates a new tester combined from a set of black and whitelists. 
  Usage: (new-tester (whitelist (function-matcher 'println)))
  This returns a tester that takes 2 arguments a function, and a namespace."
  [& definitions]
  (let [{wl :whitelist bl :blacklist} (reduce #(assoc %1 (:type %2) 
                  (conj (get %1 (:type %2)) 
                  (:tests %2))) {} definitions)]
    (fn 
      ([]
         definitions)
      ([object method]
         (let [method (symbol method)]
           (and
            (not-any? true? (run-list object bl))
            (not-any? true? (run-list method bl))
            (or
             (some true? (run-list object wl))
             (some true? (su/flatten method wl))))
           )))))

(defn extend-tester    "Extends a tester with more definitions."
  [tester & definitions]
  (apply new-tester (concat (tester) definitions)))

(defn combine-testers
  [& testers]
  (apply new-tester (apply concat (map #(%) testers))))

(defn i-want
  [& forms]
  (let [[good bad] (split-with (partial not= :but-not) forms)
	good (su/flatten (map fn-seq good))
	bad (set/difference (set good) (set (su/flatten (map fn-seq good))))]
    (new-tester (whitelist (apply function-matcher (map #(:name (meta %)) good))) (blacklist (apply function-matcher (map #(:name (meta %)) bad))))))

(defn find-bad-forms
  "Just a helper function to detect the forms that failed the test."
  [tester ns  form]
  (filter #(not (tester % ns)) (fn-seq form)))

(def 
 #^{:doc "A tester that passes everything, convinent for trying and debugging but 
         not secure."}
 debug-tester (constantly true))
