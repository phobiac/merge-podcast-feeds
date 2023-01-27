(ns org.motform.merge-podcast-feeds.websub
  (:require [hato.client :as http]
            [com.brunobonacci.mulog :as u]
            [org.motform.merge-podcast-feeds.config  :as config]
            [org.motform.merge-podcast-feeds.podcast :as podcast]))

(def route-name "/update-podcast-feeds")

(def client
  (http/build-http-client {:redirect-policy :always
                           :connect-timeout 10000}))

(defn send-websub-subscription-request [url]
  (let [host-url (config/get-in [:config/host-url])]
    (http/post {:http-client client
                :form-params {"hub.mode"     "subscribe"
                              "hub.topic"    url
                              "hub.callback" (str host-url route-name)}})))

(defn handle-verification [{{challenge "hub.challenge"} :query-params}]
  {:status 200
   :body challenge})

(defn handle-update-request [{{:strs [rel=self rel=hub referer]} :headers}]
  (u/log :update/requested
         :source (or rel=self referer) ; TODO: Double check this
         :websub/hub rel=hub)
  (podcast/assemble-feed!)
  (u/log :update/success)
  {:status 204})
