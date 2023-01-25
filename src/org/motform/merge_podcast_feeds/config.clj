(ns org.motform.merge-podcast-feeds.config
  "Call `parse-json-config` to parse and validate a configuration.

  The validation is in many ways total overkill and exists
  50% as an excuse for me to practice `spec`,
  50% as a way to provide useful errors to users.
  The perfect split, some might argue."
  (:refer-clojure :exclude [get-in])
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.spec.alpha :as s]))

(def *config
  (atom nil))

(defn get-in
  "Return the value at `ks` from parsed config.
  Return nil if the key is not present, or the not-found value if supplied."
  ([ks] (clojure.core/get-in @*config ks))
  ([ks not-found] (clojure.core/get-in @*config ks not-found)))

(defn- namespace-keyword
  "Version of `clojure.core/keyword` that works with keywords."
  [ns]
  (fn [k]
    (if (keyword? k)
      (keyword ns (name k))
      (keyword ns k))))

(defn parse-json-config
  "Parse `config-file`, assumes the file to be present
  and that it will be validated by spec."
  [config-file]
  (-> (json/read config-file :key-fn keyword)
      (update-keys (namespace-keyword "config"))
      (update :config/metadata update-keys (namespace-keyword "metadata"))
      (update-in [:config/metadata :metadata/itunes] update-keys (namespace-keyword "itunes"))))

(defn- exit-with-error-message
  "Exit process with `exit-code` and print `error-message`."
  [error-message & {:keys [exit-code why]}]
  (println error-message)
  (when why (println "\nExplanation:\n" why))
  #_(System/exit exit-code))

(defn read-and-validate-json-config
  "Try to read config at `json-path`.
  Will error with explanation on file errors or invalid configurations."
  [json-path]
  (try (with-open [config-file (io/reader json-path)]
         (let [config (parse-json-config config-file)
               valid? (s/valid? :config/valid config)]
           (if valid?
             (reset! *config config)
             (exit-with-error-message
              "Error: Config includes incorrect metadata.\n"
              :why (s/explain :config/valid config)
              :exit-code -10))))

       (catch java.io.FileNotFoundException e
         (exit-with-error-message
          (str "Error: Unable to read file at path: \"" json-path "\"\n"
               "Path is incorrect or file does not exist.")
          :why e
          :exit-code -1))

       (catch Exception e
         (exit-with-error-message
          "Error: Unable to read config."
          :why e
          :exit-code -2))))

(comment
  (read-and-validate-json-config "resources/json/example_config.json")
  :comment)
