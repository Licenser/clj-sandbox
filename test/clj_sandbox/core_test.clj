(ns clj-sandbox.core-test
  (:use [net.licenser.sandbox]
	[net.licenser.sandbox matcher tester] :reload-all)
  (:use [clojure.test]))


(defn run-in-sandbox [code]
  (let [s (stringify-sandbox (new-sandbox :tester debug-tester :timeout 50))]
    (s code)))

(defn run-in-sandbox-compiler [code]
  (let [s (stringify-sandbox (new-sandbox-compiler :tester debug-tester :timeout 50))]
    ((s code) {})))

(defn run-in-stringwriter-compiler [code]
  (let [s (stringify-sandbox 
	   (new-sandbox-compiler
	    :tester
	    (extend-tester secure-tester (whitelist (function-matcher '*out* 'println)))
	    :object-tester 
	    (extend-tester 
	     default-obj-tester 
	     (whitelist 
	      (class-matcher java.io.StringWriter)))))]
    ((s code) {})))

(deftest stringwriter-test
  (is (= "3" (run-in-stringwriter-compiler "(with-out-str (println 3))"))))

(deftest math-cos-test
  (is (= 0.8775825618903728 (run-in-sandbox-compiler "(Math/cos 0.5)"))))

(deftest eval-map-test
  (is (= {:x (quote y)} (run-in-sandbox-compiler "{:x 'y}")))
  (is (= nil (run-in-sandbox-compiler "({:x 'y} :y)")))
  (is (= 'y (run-in-sandbox-compiler "({:x 'y} :x)"))))

(deftest tester-test
  (let [lists (list (whitelist (function-matcher 'print)))
	tester (apply new-tester lists)]
    (is (= lists (tester)))))

(deftest extend-tester-test
  (let [tester (new-tester (whitelist (function-matcher 'print)))]
    (is (= true (tester '(print 1) 'user)))
    (is (= true (tester '1 'user)))
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
  (is (= '(true) ((class-matcher String) java.lang.String)))
  (is (= '(true) ((class-matcher String) java.lang.String)))
  (is (= '(false) ((class-matcher Float) java.lang.String)))
  (is (= '(true false) ((class-matcher String Float) java.lang.String)))
  (is (= '(false false) ((class-matcher String Float) java.lang.Thread)))
  (is (= '() ((class-matcher java.lang.String) (resolve '+)))))

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
