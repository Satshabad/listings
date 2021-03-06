(defproject listings "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [compojure "1.3.1"]
                 [ring/ring-defaults "0.1.2"]]
  :plugins [[lein-ring "0.8.13"]]
  :ring {:handler listings.handler/app}
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [org.clojure/java.jdbc "0.3.5"]
                        [csv-map "0.1.2"]
                        [org.clojure/tools.logging "0.3.1"]
                        [org.xerial/sqlite-jdbc "3.7.2"]
                        [cheshire "5.5.0"]]}})
