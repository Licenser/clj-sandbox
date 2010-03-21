(ns clj-sandbox.core-test
  (:use [net.licenser.sandbox] :reload-all)
  (:use [clojure.test]))


(defn run-in-sandbox [code]
  (let [s (create-sandbox (gensym) debug-tester 10)] 
    (s code)))

(defn run-in-sandbox-compiler [code]
  (let [s (create-sandbox-compiler (gensym) debug-tester 10)] 
    ((s code) {})))


(deftest fn-seq-test
  (is (= '() (fn-seq '(bla))))
  (is (= (list #'clojure.core/map) (fn-seq '(map))))
  (is (= '(loop* recur) (fn-seq '(loop [] (recur)))))
  (is (= (list 'new java.lang.String) (fn-seq '(java.lang.String. ""))))
  (is (= '(loop* def recur) (fn-seq '(loop [_ (def x 1)] (recur))))))

(deftest function-tester-test
  (is (= '(true) ((function-tester 'map) #'clojure.core/map)))
  (is (= '(true false) ((function-tester 'map 'reduce) #'clojure.core/map)))
  (is (= '(false) ((function-tester 'reduce) #'clojure.core/map))))

(deftest namespace-tester-test
  (is (= '(true) ((namespace-tester 'clojure.core) #'clojure.core/map)))
  (is (= '(true) ((namespace-tester 'clojure.core) #'clojure.core/reduce)))
  (is (= '(true) ((namespace-tester 'java.lang) java.lang.String))))

(deftest class-tester-test
  (is (= '(true) ((class-tester java.lang.String) java.lang.String))))

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