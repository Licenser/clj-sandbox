(ns net.licenser.sandbox.matcher)

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
  (dorun (map #(if (= % Object) (throw (RuntimeException. "You silly wabbit have 'Object' in a class matcher. This will make /every/ object pass the tester so defeats the prupose of the matcher, I don't let you do that since I'm evelish! Also Rayne made me hunt this for a whole night untill he noted 'oh perhaps it is cause I use Object'... Meh!"))) classes))
  (fn [form]
    (if (= (type form) java.lang.Class)
      (map (partial isa? form) classes)
      '())))

(defn partial-namespace-matcher
  "Creates a tester that whitelists all functions within a namespace and all namespaces under it."
  [& namespaces]
  (fn [form]
    (cond
     (= (type form) clojure.lang.Var)
     (let [ns (str (ns-name (:ns (meta form))))]
       (map #(zero? (.indexOf ns (str %))) namespaces))
     (= (type form) java.lang.Class)
     (let [ns (second (re-find #"^class (.*)\.\w+$" (str form)))]
       (map #(zero? (.indexOf ns (str %))) namespaces))
     true
      '())))

(defn combine-matchers
  "Combines two testers."
     [& tests]
     (fn [form]
      (map #(% form) tests)))