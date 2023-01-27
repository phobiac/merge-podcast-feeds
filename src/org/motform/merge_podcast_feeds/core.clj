(ns org.motform.merge-podcast-feeds.core
  (:require [mount.core :as mount]
            [org.motform.merge-podcast-feeds.specs]
            [org.motform.merge-podcast-feeds.config  :as config]
            [org.motform.merge-podcast-feeds.logging]
            [org.motform.merge-podcast-feeds.podcast :as podcast]
            [org.motform.merge-podcast-feeds.server])
  (:gen-class))


;; TODO: Make this work
(defn output-test-xml [& args]
  (mount/start-with-args args
                    #'org.motform.merge-podcast-feeds.config/config
                    #'org.motform.merge-podcast-feeds.logging/logger
                    #'org.motform.merge-podcast-feeds.podcast/podcast)
  (podcast/output-test-feed)
  (println "Successfully emitted xml at" (config/get-in [:config/xml-file-path])))

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
