(ns net.licenser.sandbox)


; Thanks to hiredman for this code snippet!
(defn s-seq 
  "Convertns a form into a sequence."
  [form]
  (tree-seq seq? #(let [a (macroexpand %)] (or (and (seq? a) a) (list a))) form))


(defn fn-seq 
  "Converst a form into a sequence of functions."
  [form]
  (filter ifn? (s-seq form)))

(defn function-whitelist-tester
  "Creates a tester that whitelists functions."
  [& functions]
  (fn [form]
    (some (partial = form) functions)))

(defn function-backlist-tester
  "Creates a tester that whitelists functions."
  [& functions]
  (fn [form]
    (not (some (partial = form) functions))))

(defn namespace-tester
  "Creates a tester that whitelists functions."
  [& functions]
  (fn [form]
    (map (partial = (namespace form)) functions)))

(defn whitelist
  [& testers]
    (fn [form]
      (some true? (flatten (map #(% form) testers)))))

(defn blacklist
  [& testers]
    (fn [form]
      (not (some false? (flatten (map #(% form) testers))))))


(defn new-sandbox-tester
  [& definitions]
  (fn [form]
    (every? true? (map (fn [function] (every? #(% function) definitions)) (fn-seq form)))))