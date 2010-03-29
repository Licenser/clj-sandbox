(defproject clj-sandbox "0.2.9-SNAPSHOT-HOTFIX"
  :namespaces [net.licenser.sandbox net.licenser.sandbox.matcher net.licenser.sandbox.tester net.licenser.sandbox.jvm net.licenser.sandbox.save-fns]
  :description "Clojure library for sandboxed execution"
  :dependencies [[org.clojure/clojure "1.1.0"]
                 [org.clojure/clojure-contrib "1.1.0"]]
  :dev-dependencies [
	[lein-clojars "0.5.0-SNAPSHOT"]
        [swank-clojure/swank-clojure "1.1.0"]
	[leiningen/lein-swank "1.1.0"]])
