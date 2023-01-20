(ns org.motform.merge-podcast-feeds.core
  (:require [org.motform.merge-podcast-feeds.config  :as config]
            [org.motform.merge-podcast-feeds.date    :as date]
            [org.motform.merge-podcast-feeds.podcast :as podcast]
            [org.motform.merge-podcast-feeds.xml     :as xml])
  (:gen-class))

(defn test-merge
  "Read json config and emit a test merge under `resources/xml/text.xml`.
  Can be run from the command line using `clj -X:test-merge` {:config path-to-config}"
  [{json-config-path :config}]
  (let [config      (config/parse-json-config json-config-path)
        feeds       (-> config :json/feeds xml/collect-and-sort-feeds)
        metadata    (podcast/preamble-&-metadata (date/RFC1123-now))
        output-feed (xml/append-podcast-feeds metadata feeds)]
    (xml/emit-test-xml output-feed)
    :ok!))

(defn -main
  "This will be the main entry point, tba."
  [& args])

(comment
  (test-merge {:config "resources/json/example_config.json"})

  (config/parse-json-config "resources/json/example_config.json")
  :end)

