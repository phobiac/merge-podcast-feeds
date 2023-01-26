(ns org.motform.merge-podcast-feeds.castopod
  "Castopod integrations, https://castopod.org/"
  (:require [clojure.data.json :as json]
            [hato.client :as http]
            [org.motform.merge-podcast-feeds.config :as config]))

(def api
  {:podcasts "/api/rest/v1/podcasts"
   :podcast "/api/rest/v1/podcasts/"})

(defn url [endpoint]
  {:pre [(contains? (set (keys api)) endpoint)]}
  (let [base-url (config/get-in [:config/castopod :base-url])]
    (str base-url (api endpoint))))

(def client
  (http/build-http-client {:redirect-policy :always
                           :connect-timeout 10000}))

(defn podcasts []
  (let [response (http/get (url :podcasts) {:http-client client})]
    (json/read-str (:body response) :key-fn keyword)))

(defn podcast [podcast-id]
  (let [url      (str (url :podcast) podcast-id)
        response (http/get url {:http-client client})]
    (json/read-str (:body response) :key-fn keyword)))

(defn podcast-feed-urls []
  (let [podcasts (podcasts)]
    (mapv :feed_url podcasts)))

(defn podcast-feed-url [podcast-id]
  (let [podcast (podcast podcast-id)]
    (:feed_url podcast)))
