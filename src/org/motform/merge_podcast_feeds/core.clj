(ns org.motform.merge-podcast-feeds.core
  (:require [org.motform.merge-podcast-feeds.config  :as config]
            [org.motform.merge-podcast-feeds.podcast :as podcast]
            [org.motform.merge-podcast-feeds.server  :as server]
            [org.motform.merge-podcast-feeds.specs])
  (:gen-class))

(defn output-test-xml [& {json-config-path :config}]
  (config/read-and-validate-json-config json-config-path)
  (podcast/make-channel!)
  (podcast/assemble-feed!)
  (podcast/output-test-feed)
  (println "Successfully emitted xml at:" (config/get-in [:config/xml-file-path])))

(defn -main
  "The primary entry point, parses the config and serves the podcast feed
  at the slug and port specified by the config."
  [& {json-config-path :config}]
  (config/read-and-validate-json-config json-config-path)
  (podcast/make-channel!)
  (podcast/assemble-feed!)
  (server/start!))

(comment
  (-main           :config "resources/json/example_config.json")
  (output-test-xml :config "resources/json/example_config.json")
  :comment)
