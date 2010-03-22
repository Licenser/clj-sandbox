(ns clj-sandbox.core-test
  (:use [net.licenser.sandbox] :reload-all)
  (:use [clojure.test]))


(defn run-in-sandbox [code]
  (let [s (new-sandbox :tester debug-tester :timeout 10)] 
    (s code)))

(defn run-in-sandbox-compiler [code]
  (let [s (new-sandbox-compiler :tester debug-tester :timeout 10)] 
    ((s code) {})))


(deftest tester-test
  (let [lists (list (whitelist (function-matcher 'print)))
	tester (apply new-tester lists)]
    (is (= lists (tester)))))
 

(deftest extend-tester-test
  (let [tester (new-tester (whitelist (function-matcher 'print)))]
    (is (= true (tester '(print 1) 'user)))
    (is (= false (tester '(println 1) 'user)))
    (let [extended-tester (extend-tester tester (whitelist (function-matcher 'println)))]
    (is (= true (extended-tester '(print 1) 'user)))
    (is (= true (extended-tester '(println 1) 'user))))))
  
(deftest fn-seq-test
  (is (= '() (fn-seq '(bla))))
  (is (= (list #'clojure.core/map) (fn-seq '(map))))
  (is (= '(loop* recur) (fn-seq '(loop [] (recur)))))
  (is (= (list 'new java.lang.String) (fn-seq '(java.lang.String. ""))))
  (is (= '(loop* def recur) (fn-seq '(loop [_ (def x 1)] (recur))))))

(deftest function-matcher-test
  (is (= '(true) ((function-matcher 'map) #'clojure.core/map)))
  (is (= '(true false) ((function-matcher 'map 'reduce) #'clojure.core/map)))
  (is (= '(false) ((function-matcher 'reduce) #'clojure.core/map))))

(deftest namespace-matcher-test
  (is (= '(true) ((namespace-matcher 'clojure.core) #'clojure.core/map)))
  (is (= '(true) ((namespace-matcher 'clojure.core) #'clojure.core/reduce)))
  (is (= '(true) ((namespace-matcher 'java.lang) java.lang.String))))

(deftest class-matcher-test
  (is (= '(true) ((class-matcher java.lang.String) java.lang.String))))

(deftest loop-timeouts-test
  (is (isa? java.util.concurrent.TimeoutException (try
        (run-in-sandbox "(loop [] (recur))")
        (catch Exception e (type e)))))
  (is (isa? java.util.concurrent.TimeoutException (try
    (run-in-sandbox-compiler "(loop [] (recur))")
    (catch Exception e (type e))))))

(deftest seq-timeouts-test
  (is (isa? java.util.concurrent.TimeoutException (try
        (run-in-sandbox "(range 1 10000000)")
        (catch Exception e (type e)))))
  (is (isa? java.util.concurrent.TimeoutException (try
    (run-in-sandbox-compiler "(range 1 10000000)")
    (catch Exception e (type e))))))