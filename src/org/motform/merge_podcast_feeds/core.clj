(ns org.motform.merge-podcast-feeds.core
  (:require [mount.core :as mount]
            [com.brunobonacci.mulog :as u]
            [org.motform.merge-podcast-feeds.specs]
            [org.motform.merge-podcast-feeds.config  :as config]
            [org.motform.merge-podcast-feeds.podcast :as podcast]
            [org.motform.merge-podcast-feeds.server])
  (:gen-class))

(u/start-publisher! {:type :console})

;; TODO: Make this work
(defn output-test-xml [& args]
  (mount/start-with-args args
                    #'org.motform.merge-podcast-feeds.config/config
                    #'org.motform.merge-podcast-feeds.podcast/podcast)
  (podcast/output-test-feed)
  (u/log :test/success
         :message  "Successfully emitted xml"
         :xml-path (config/get-in [:config/xml-file-path])))

(defn -main
  "The main entry point, parses the config and serves the podcast feed
  at the slug and port specified by the config."
  [& args]
  (mount/start-with-args args))


(comment
  (output-test-xml [{:config "resources/json/example_config.json"}])
  (mount/start-with-args [{:config "resources/json/example_config.json"}])
  (config/read-and-validate-json-config "resources/json/example_config.json")

  :comment)
