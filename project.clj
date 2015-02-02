(defproject server "0.1.0-SNAPSHOT"
  :description "SSH public key server"
  :url "https://github.com/zalando/ssh-access-granting-service"
  :license {:name "Apache License"
            :url "http://www.apache.org/licenses/"}
  :scm {:url "git@github.com:zalando/ssh-access-granting-service.git"}
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 ; lifecycle management
                 [com.stuartsierra/component "0.2.2"]
                 [compojure "1.3.1"]
                 [ring/ring-defaults "0.1.2"]
                 [environ "1.0.0"]
                 ; REST APIs
                 [metosin/compojure-api "0.16.4"]
                 [metosin/ring-http-response "0.5.2"]
                 [metosin/ring-swagger-ui "2.0.17"]
                 ; logging
                 [org.clojure/tools.logging "0.2.4"]
                 [org.slf4j/slf4j-api "1.7.7"]
                 [org.slf4j/jul-to-slf4j "1.7.7"]
                 [org.slf4j/jcl-over-slf4j "1.7.7"]
                 ; LDAP
                 [org.clojars.pntblnk/clj-ldap "0.0.9"]

                 ]
  :plugins [[lein-ring "0.8.13"]
            [lein-environ "1.0.0"]
            [lein-cloverage "1.0.2"]]

  :aliases {"cloverage" ["with-profile" "test" "cloverage"]}

  :ring {:handler server.handler/app}
  :main server.handler
  :aot :all
  :uberjar-name "ssh-access-granting-service.jar"
  :profiles {
             :log {:dependencies [[org.apache.logging.log4j/log4j-core "2.1"]
                                  [org.apache.logging.log4j/log4j-slf4j-impl "2.1"]]}
             :dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                                  [ring-mock "0.1.5"]]}
             :test [:log {:dependencies [[org.clojars.runa/conjure "2.1.3"]
                                         [midje "1.6.3"]
                                         [clj-http "1.0.1"]
                                         [ring-mock "0.1.5"]]}]
             :repl [:no-log]})
