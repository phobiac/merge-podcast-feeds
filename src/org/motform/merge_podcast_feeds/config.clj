(ns org.motform.merge-podcast-feeds.config
  (:require [clojure.data.json :as json]
            [clojure.spec.alpha :as s]
            [clojure.string :as str]))

(defn parse-json-config
  [json-path]
  {:post [(s/valid? :json/config %)]}
  (-> json-path slurp (json/read-str :key-fn (partial keyword "json"))))

(s/def :podcast-feed/extension
  (fn [feed]
    (and #(str/starts-with? feed "https://")
         (some #(str/ends-with? feed %) #{".rss" ".xml"}))))

(s/def :json/feeds
  (s/and vector? (s/+ (s/cat :feed :podcast-feed/extension))))

(s/def :json/config
  (s/keys :req [:json/feeds]))

(comment
  (def config
    #:json
     {:feeds
      ["https://pod.alltatalla.se/@rekreation/feed.rss"
       "https://pod.alltatalla.se/@omvarldar/feed.xml"],
      :nested #:json{:object "keys"}})

  (s/explain :json/config config)
  :comment)
