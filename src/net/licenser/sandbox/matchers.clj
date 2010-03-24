(ns net.licenser.sandbox.matchers)

(defn function-matcher
  "Creates a tester that whitelists functions."
  [& functions]
  (fn [form]
    (if (= (type form) clojure.lang.Var)
      (map (partial = (:name (meta form))) functions)
      (map (partial = form) functions))))

(defn namespace-matcher
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

(defn class-matcher
  "Creates a tester than whitelists a Java class."
  [& classes]
  (fn [form]
    (if (= (type form) java.lang.Class)
      (map (partial isa? form) classes)
      '())))

(defn combine-matchers
  "Combines two testers."
     [& tests]
     (fn [form]
      (map #(% form) tests)))