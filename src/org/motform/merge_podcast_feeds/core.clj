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
  (podcast/output-test-feed))

(defn -main
  "The primary entry point, "
  [& {json-config-path :config}]
  (config/read-and-validate-json-config json-config-path)
  (podcast/make-channel!)
  (podcast/assemble-feed!)
  (server/start!))

(comment
  (-main :config "resources/json/example_config.json")
  (output-test-xml :config "resources/json/example_config.json")
  :comment)
