(ns org.motform.merge-podcast-feeds.logging
  (:require [mount.core :as mount]
            [com.brunobonacci.mulog :as u]
            [org.motform.merge-podcast-feeds.config :as config]))

(defn publisher
  "Build a publisher to be consumed by `u/start-publisher!`
  Defaults to console logging."
  []
  (if-let [logging (config/get-in [:config/logging])]
    (let [{:keys [loggers file-path]} logging]
      (cond-> {:type :multi :publishers []}
        (contains? loggers "console")
        (update :publishers conj {:type :console})
        (contains? loggers "file")
        (update :publishers conj {:type :simple-file :filename file-path})))
    {:type :console}))

(mount/defstate logger
  :start (u/start-publisher! (publisher))
  :stop  (logger))
