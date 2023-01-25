(ns org.motform.merge-podcast-feeds.server
  (:require [reitit.ring :as ring]
            [reitit.ring.middleware.defaults :as defaults]
            [ring.middleware.defaults :refer [api-defaults]]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.params :as params]
            [org.motform.merge-podcast-feeds.config  :as config]
            [org.motform.merge-podcast-feeds.podcast :as podcast]))

(def app
  (let [slug (config/get-in [:config/slug])]
    (ring/ring-handler
    (ring/router
     [slug
      {:name ::feed
       :doc  "The primary route serving the merged podcast feed."
       :get  (fn [_]
               {:status 200
                :body   (podcast/feed)})}]
     {:data
      {:middleware [defaults/ring-defaults-middleware
                    ;; defaults/defaults-middleware ;; requires muuntaja and malli coercion?
                    params/wrap-params]
       :defaults api-defaults}})
    (ring/create-default-handler))))

(defn start! []
  (let [port (config/get-in [:config/port])]
    (jetty/run-jetty #'app {:port port :join? false})
    (println (str "Server running at port"  port "."))))

(comment
  (start!)
  :comment)
