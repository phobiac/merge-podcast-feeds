(ns org.motform.merge-podcast-feeds.core
  (:require [mount.core :as mount]
            [org.motform.merge-podcast-feeds.specs]
            [org.motform.merge-podcast-feeds.config  :as config]
            [org.motform.merge-podcast-feeds.podcast :as podcast]
            [org.motform.merge-podcast-feeds.server])
  (:gen-class))

(defn output-test-xml [& {json-config-path :config}]
  (config/read-and-validate-json-config json-config-path)
  (podcast/make-channel!)
  (podcast/assemble-feed!)
  (podcast/output-test-feed)
  (println "Successfully emitted xml at:" (config/get-in [:config/xml-file-path])))

(defn -main
  "The main entry point, parses the config and serves the podcast feed
  at the slug and port specified by the config."
  [& args]
  (mount/start-with-args args))


(comment
  (output-test-xml :config "resources/json/example_config.json")
  (mount/start-with-args {:config "resources/json/example_config.json"})
  (config/read-and-validate-json-config "resources/json/example_config.json")

  :comment)
