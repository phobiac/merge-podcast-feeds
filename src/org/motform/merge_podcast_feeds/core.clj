(ns org.motform.merge-podcast-feeds.core
  (:require [org.motform.merge-podcast-feeds.config  :as config]
            [org.motform.merge-podcast-feeds.podcast :as podcast]
            [org.motform.merge-podcast-feeds.xml     :as xml])
  (:gen-class))

(defn test-merge [{json-config-path :config}]
  (let [config     (config/parse-json-config json-config-path)
        feeds       (xml/collect-and-sort-feeds (:config/feeds config))
        channel     (podcast/config->hiccup-channel config)
        xml-no-feed (xml/hiccup-channel->xml-with-pubDate channel)
        xml         (xml/append-podcast-feeds xml-no-feed feeds)]
    (xml/emit-test-xml xml)
    ;; (println)
    ;; (println (clojure.data.xml/indent-str xml))
    :ok!))

(defn -main
  "This will be the main entry point, tba."
  [& args])

(comment
  (test-merge {:config "resources/json/example_config.json"})

  (config/parse-json-config "resources/json/example_config.json")
  :end)

