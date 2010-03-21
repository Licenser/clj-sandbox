(ns clj-sandbox.core-test
  (:use [net.licenser.sandbox] :reload-all)
  (:use [clojure.test]))


(defn run-in-sandbox [code]
  (let [s (create-sandbox (gensym) debug-tester 10)] 
    (s code)))

(defn run-in-sandbox-compiler [code]
  (let [s (create-sandbox-compiler (gensym) debug-tester 10)] 
    ((s code) {})))


(deftest fn-seq-test ;; FIXME: write
  (is (= '() (fn-seq '(bla))))
  (is (= '(loop* recur) (fn-seq '(loop [] (recur)))))
  (is (= '(loop* def recur) (fn-seq '(loop [_ (def x 1)] (recur))))))
    
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