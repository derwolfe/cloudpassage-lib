(defproject cloudpassage-lib "0.1.0-SNAPSHOT"
  :description "Clojure interface for the CloudPassage API"
  :url "https://github.com/RackSec/cloudpassage-lib"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]]
  :plugins [[lein-cljfmt "0.3.0"]
            [lein-pprint "1.1.1"]
            [lein-cloverage "1.0.7-SNAPSHOT"]]
  :min-lein-version "2.0.0"
  :main ^:skip-aot cloudpassage-lib.core
  :uberjar-name "cloudpassage-lib.jar"
  :profiles {:uberjar {:aot :all}
             :dev {:dependencies [[pjstadig/humane-test-output "0.7.1"]]
                   :injections [(require 'pjstadig.humane-test-output)
                                (pjstadig.humane-test-output/activate!)]}})
