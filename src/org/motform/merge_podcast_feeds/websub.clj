(ns org.motform.merge-podcast-feeds.websub
  "INCOMPLETE
  This implementation is guided by:
   https://indieweb.org/How_to_publish_and_consume_WebSub
   https://websub.rocks/"
  (:require [chime.core             :as chime]
            [com.brunobonacci.mulog :as u]
            [hato.client            :as http]
            [mount.core             :as mount]
            [org.motform.merge-podcast-feeds.config  :as config]
            [org.motform.merge-podcast-feeds.podcast :as podcast])
  (:import (java.time Instant)))

(def *subscription-requests (atom #{}))

(def route-name "/update-podcast-feeds")

(defn topic
  "The topic, in WebSub parlace, is the URL of the feed that we are publishing."
  []
  (str (config/get-in [:config/host-url]) (config/get-in [:config/slug])))

(defn update-url []
  (str (config/get-in [:config/host-url]) route-name))

(def client
  (http/build-http-client {:redirect-policy :always
                           :connect-timeout 10000}))

;;; Subscription 

(def url-regex ; Source: https://stackoverflow.com/a/6041965
  (re-pattern #"(http|https):\/\/([\w_-]+(?:(?:\.[\w_-]+)+))([\w.,@?^=%&:\/~+#-]*[\w@?^=%&\/~+#-])"))

(defn- url-from-link-when-rel=
  "NOTE: only works with 'single line' Link headers, like:
  Link: <https://pubsubhubbub.superfeedr.com/>; rel=\"hub\", <https://kylewm.com/>; rel=\"self\"

  TODO: Make it work with separate Links."
  [rel link]
  (let [rel-regex (re-pattern (str "rel=\"" rel "\""))]
    (when-let [target (some #(when (re-find rel-regex %) %) link)]
      (first (re-find url-regex target)))))

(defn- send-websub-discovery-request [url]
  (http/get url {:http-client client}))

(defn- websub-discovery-link
  "Return map of `:self` and `:hub` from rel= websub responses."
  [{{:strs [link]} :headers :as response}]
  (let [links {:self (url-from-link-when-rel= "self" link)
               :hub  (url-from-link-when-rel= "hub"  link)}]
    (if (not-any? nil? (vals links)) links
        (u/log :websub.disscovery/fail :resonse response))))

(defn- send-websub-subscription-request
  "`self` should be the value of `rel=self` aquired in the discovery request.
   `hub` should be the value of `rel=hub` aquired in the discovery request.
   The return of `websub-discovery-link` handles this correctly."
  [{:keys [hub self]}]
  (swap! *subscription-requests conj self)
  (http/post hub {:http-client client
                  :form-params {"hub.mode"     "subscribe"
                                "hub.topic"    self
                                "hub.callback" (update-url)}}))
                                        ; NOTE: We don't use unique callbacks, as any callback will trigger a complete feed rebuild. Could be something to change in the future.

(defn subscribe-to-websub-url
  "Start the machinery requride to subscribe to websub feed at `url`.
   Will schedule periodical re-subscription if successful. 

   Will fail and stop the process if `websub-discovery-link` returns nil.
   The error is reported by `websub-discovery-link`."
  [url]
  (u/log :websub.subscribe/start :websub/topic url)
  (when-let [discovery-links (websub-discovery-link (send-websub-discovery-request url))]
    (send-websub-subscription-request discovery-links))) ; This will cause the hub to GET "/update-podcast-feeds", calling `handle-verification`

(defn schedule-websub-re-subscription
  "Schedule a re-subscription to `topic` after `lease-seconds`."
  [lease-seconds topic]
  (let [lease (.. (Instant/now) (plusSeconds (parse-long lease-seconds)))]
    (u/log :websub.resub/schedule :websub/topic topic :websub/lease lease)
    (chime/chime-at [lease]
                    (fn [_] (subscribe-to-websub-url topic))
                    {:error-handler #(u/log :websub.resub/chime-fail
                                            :websub/topic topic
                                            :websub/lease lease
                                            :exception    %)})))

(defn handle-verification
  "Ring handler that respons do the final websub verification.
   Returns a ring-style response map.
   Used in `org.motform.merge-podcast-feeds.server`."
  [{{challenge     "hub.challenge"
     mode          "hub.mode"
     topic         "hub.topic"
     lease-seconds "hub.lease_seconds"} :query-params
    :as response}]
  (cond (not= mode "subscribe")
        (do (u/log :websub.verification/fail
                   :reason (str "Mode " mode " is not \"subscribe\".")
                   :websub/response response)
            {:status 404})

        (not (contains? @*subscription-requests topic))
        (do (u/log :websub.verification/fail
                   :reason (str "Topic " topic " not a known subscription request")
                   :websub/response response
                   :websub/subscription-requests @*subscription-requests)
            {:status 404})

        :else
        (do (u/log :websub.verification/success)
            (schedule-websub-re-subscription lease-seconds topic)
            {:status 200 :body challenge})))

;;; Publishing

(defn link-header
  "Return Link header to broadcast websub avaliability of feed."
  []
  (let [hub (config/get-in [:config/websub :hub])]
    (str "<" hub ">; rel=\"hub\", <" (topic) ">; rel=\"self\"")))

(defn notify-hub-of-update []
  (let [hub (config/get-in [:config/websub :hub])]
    (http/post hub {:form-params {"hub.mode" "publish"
                                  "hub.url"  (topic)}})))

(defn handle-update-request
  "Ring handler that responds to a POST to the `route-name`."
  [{{:strs [rel=self rel=hub referer]} :headers}]
  (u/log :websub.update/requested :websub/referer referer :websub/self rel=self :websub/hub rel=hub)

  ;; NOTE: `handle-update-request` is the only place in the websub
  ;; version that re-assembles the feed, so I don't think we
  ;; need to update the hub anywhere else.
  (podcast/assemble-feed!)
  (notify-hub-of-update)

  (u/log :websub.update/success)
  {:status 204})

;; There is no real "state" here per say.
;; However, I found that most WebSub hubs don't listen 
;; before you send them an inital update. So... we do that.
;; It might lead to some false positives, but I think that
;; most podcast apps will deal?
;; – LLA 230317
(mount/defstate websub
  :start #(when (config/get-in [:config/websub])
            (notify-hub-of-update)))

(comment

  ;; Test subscriptions using websub.rocks: https://websub.rocks/subscriber/100
  (subscribe-to-websub-url "https://websub.rocks/blog/100/kstyerUQfglvXL7ZwhNK")

  ;; Test publishing using websub.rocks: https://websub.rocks/publisher
  (notify-hub-of-update)

  "https://3a33-176-10-230-222.eu.ngrok.io/feed/podcast"

  :comment)
