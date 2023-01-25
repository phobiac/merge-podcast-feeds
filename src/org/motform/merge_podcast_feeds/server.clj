(ns org.motform.merge-podcast-feeds.server
  (:require [reitit.ring              :as ring]
            [ring.middleware.defaults :refer [api-defaults]]
            [ring.adapter.jetty       :as jetty]
            [ring.middleware.params   :as params]
            [reitit.ring.middleware.defaults :as defaults]
            [org.motform.merge-podcast-feeds.config  :as config]
            [org.motform.merge-podcast-feeds.podcast :as podcast]))

(defn app [slug]
  (ring/ring-handler
   (ring/router
    [slug
     {:name ::feed
      :doc  "The primary route serving the merged podcast feed."
      :get  (fn [_]
              {:status 200
               :body   (podcast/web-feed)})}]
    {:data
     {:middleware [defaults/ring-defaults-middleware
                   params/wrap-params]
      :defaults api-defaults}})
   (ring/create-default-handler)))

(defn start! []
  (let [port (config/get-in [:config/port])
        slug (config/get-in [:config/slug])
        app  (app slug)]
    (jetty/run-jetty app {:port port :join? false})
    (println "Server running on port:" port)
    (println "Serving podcast feed at:" slug)))

(comment
  (start!)
  :comment)

