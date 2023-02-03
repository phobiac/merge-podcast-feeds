(ns org.motform.merge-podcast-feeds.config
  "Call `parse-json-config` to parse and validate a configuration.

  The validation is in many ways total overkill and exists
  50% as an excuse for me to practice `spec`,
  50% as a way to provide useful errors to users.
  The perfect split, some might argue.

  Assumes that the config lives as \"resources/json/config.json\",
  configurable via `*config-path*`. "
  (:refer-clojure :exclude [get-in])
  (:require [clojure.data.json  :as json]
            [clojure.java.io    :as io]
            [clojure.spec.alpha :as s]
            [mount.core         :as mount]))

(defonce ^:private *config
  (atom nil))

(defonce ^:dynamic *config-path*
  "json/config.json")

(defn get-in
  "Return the value at `ks` from parsed config.
  Return nil if the key is not present, or the not-found value if supplied."
  ([ks] (get-in ks nil))
  ([ks not-found] (clojure.core/get-in @*config ks not-found)))

(defn- namespace-keyword
  "Version of `clojure.core/keyword` that works with keywords."
  [ns]
  (fn [k]
    (if (keyword? k)
      (keyword ns (name k))
      (keyword ns k))))

(defn- ?update-in
  "Updates value in `m` with `f` if `ks` is non-nil."
  [m ks f]
  (if (clojure.core/get-in m ks)
    (update-in m ks f)
    m))

(defn parse-json-config
  "Consume and parse `reader`, returning config as a map.
  Assumes the file to be present and that it will be validated by spec."
  [reader]
  (-> (json/read reader :key-fn keyword)
      (update-keys (namespace-keyword "config"))
      (update :config/metadata update-keys (namespace-keyword "metadata"))
      (?update-in [:config/logging :loggers] set)
      (update-in [:config/metadata :metadata/itunes] update-keys (namespace-keyword "itunes"))))

(defn- exit-with-error-message
  "Exit process with `exit-code` and print `error-message`."
  [error-message & {:keys [exit-code reason]}]
  (println error-message)
  (when reason (println "\nReason:\n" reason))
  (System/exit exit-code))

(defn read-and-validate-json-config
  "Try to read config at `json-path`.
  Will error with explanation on file errors or invalid configurations."
  [json-path]
  (println "Reading config at" json-path)
  (try (with-open [reader (io/reader (io/resource json-path))]
         (let [config (parse-json-config reader)
               valid? (s/valid? :config/valid config)]

           (when-not valid?
             (exit-with-error-message
              "Error: Config includes incorrect metadata.\n"
              :reason (s/explain :config/valid config)
              :exit-code -3))

           (when (and (config :config/castopod)
                      (config :config/feeds))
             (exit-with-error-message
              "Error: Config includes both \"feeds\" and \"castopod\" keys. You can only use one or the other."
              :exit-code -4))

           (when-not (or (config :config/castopod)
                         (config :config/feeds))
             (exit-with-error-message
              "Error: Config does not include \"feeds\" or \"castopod\" keys. You have to specify a podcast feed source."
              :exit-code -5))

           (reset! *config config)))

       (catch java.io.FileNotFoundException e
         (exit-with-error-message
          (str "Error: Unable to read file at path: \"" json-path "\"\n"
               "Path is incorrect or file does not exist.")
          :reason e
          :exit-code -1))

       (catch Exception e
         (exit-with-error-message
          "Error: Unable to read config."
          :reason e
          :exit-code -2))))

(mount/defstate config
  :start (read-and-validate-json-config *config-path*)
  :stop  (reset! *config nil))

(comment
  (mount/start-with-args [{:config "resources/json/example_config.json"}]
                         #'org.motform.merge-podcast-feeds.config/config)
  (read-and-validate-json-config "resources/json/example_config.json")
  :comment)
