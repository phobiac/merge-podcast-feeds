(ns org.motform.merge-podcast-feeds.websub
  (:require [hato.client :as http]
            [com.brunobonacci.mulog :as u]
            [org.motform.merge-podcast-feeds.config  :as config]
            [org.motform.merge-podcast-feeds.podcast :as podcast]))

(def route-name "/update-podcast-feeds")

(def client
  (http/build-http-client {:redirect-policy :always
                           :connect-timeout 10000}))

(defn send-websub-discovery-request [url]
  (http/get url {:http-client client}))

(defn send-websub-subscription-request [url]
  (let [host-url (config/get-in [:config/host-url])]
    (http/post url {:http-client client
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

(def url-regex ; Source: https://stackoverflow.com/a/6041965
  (re-pattern #"(http|https):\/\/([\w_-]+(?:(?:\.[\w_-]+)+))([\w.,@?^=%&:\/~+#-]*[\w@?^=%&\/~+#-])"))

(defn url-from-link-when-rel= [rel link] ; TODO: Document that `link` is a strange tuple.
  (let [rel-regex (re-pattern (str #"rel=\"" rel "\""))]
    (when-let [target (some #(when (re-find rel-regex %) %) link)]
      (first (re-find url-regex target))))) 


(defn websub-discovery-link [{{:strs [link]} :headers}]
  {:self (url-from-link-when-rel= "self" link)
   :hub  (url-from-link-when-rel= "hub"  link)})

(comment

  (def feed "https://pod.alltatalla.se/@omvarldar/feed.xml")

  (send-websub-subscription-request feed)
  (send-websub-subscription-request "https://websub.rocks/blog/100/cjlZtuGihB78l8qe4bya")

  (def r (send-websub-discovery-request "https://websub.rocks/blog/100/cjlZtuGihB78l8qe4bya"))

  (require '[clojure.inspector :as inspector])
  (clojure.inspector/inspect-tree r)

  :comment)
