(ns org.motform.merge-podcast-feeds.server
  (:require [com.brunobonacci.mulog   :as u]
            [mount.core               :as mount]
            [reitit.ring              :as ring]
            [ring.middleware.defaults :refer [api-defaults]]
            [ring.adapter.jetty       :as jetty]
            [ring.middleware.params   :as params]
            [reitit.ring.middleware.defaults :as defaults]
            [org.motform.merge-podcast-feeds.config  :as config]
            [org.motform.merge-podcast-feeds.podcast :as podcast]
            [org.motform.merge-podcast-feeds.websub  :as websub]))

(defn app [slug]
  (ring/ring-handler
   (ring/router
    [[slug
      {:name :podcast/feed
       :doc  "The primary route serving the merged podcast feed."
       :get  (fn [_]
               {:status 200
                :body   (podcast/web-feed)})}]

     [websub/route-name
      {:name :podcast/update
       :doc  "Requests the server to update and re-assemble the podcast feed.
              The `:get` response is intended for WebSub verification, while
              the `:post` response prompts feed re-assembly. This can be called
              manually, or via WebSub."
       :get  websub/handle-verification
       :post websub/handle-update-request}]]

    {:data
     {:middleware [defaults/ring-defaults-middleware
                   params/wrap-params]
      :defaults api-defaults}})
   (ring/create-default-handler)))

(defn start!
  "Start and return web server, based on user config.."
  []
  (let [port   (config/get-in [:config/port])
        slug   (config/get-in [:config/slug])
        app    (app slug)
        server (jetty/run-jetty app {:port port :join? false})]
    (u/log ::start
           :message "Server started successfully."
           :port port
           :feed slug)
    server))

(mount/defstate server
  :start (start!)
  :stop  (.stop server))

(comment
  (start!)
  :comment)

