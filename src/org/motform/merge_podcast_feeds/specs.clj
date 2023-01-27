(ns org.motform.merge-podcast-feeds.specs
  "The validation is based of the list found at:
  https://help.apple.com/itc/podcasts_connect/#/itcb54353390

  iTunes tags are required, as they form standard podcast metadata."
  (:require [clojure.edn        :as edn]
            [clojure.java.io    :as io]
            [clojure.spec.alpha :as s]
            [clojure.string     :as str])
  (:import (java.net URL)
           (java.io PushbackReader)))

;; Assuming that https://www.rssboard.org/rss-language-codes is correct:
(s/def :rss/language-code
  #(re-matches #"(\w\w)(-\w\w)?" %))

;; Source: https://emailregex.com/
(s/def :email/email
  #(re-matches #"(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\x01-\x08\x0b\x0c\x0e-\x1f\x21\x23-\x5b\x5d-\x7f]|\\[\x01-\x09\x0b\x0c\x0e-\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\x01-\x08\x0b\x0c\x0e-\x1f\x21-\x5a\x53-\x7f]|\\[\x01-\x09\x0b\x0c\x0e-\x7f])+)\])" %))

(s/def :url/url
  (s/and string?
         (fn [url]
           (try (boolean (URL. url))
                (catch Exception _ false)))))

(s/def :url/href :url/url)

(s/def :url/podcast-feed
  (fn [feed]
    (some #(str/ends-with? feed %) #{".rss" ".xml"})))

(s/def :podcast/feed (s/and :url/url :url/podcast-feed))

(s/def :config/feeds
  (s/and vector? seq (s/+ (s/cat :feed :podcast/feed))))

;; Metadata tags
(s/def :metadata/copyright   string?)
(s/def :metadata/description string?)
(s/def :metadata/image       (s/keys :req-un [:metadata/title :metadata/link :url/url]))
(s/def :metadata/language    :rss/language-code)
(s/def :metadata/link        :url/url)
(s/def :metadata/name        string?)
(s/def :metadata/title       string?)
(s/def :metadata/atom        :url/url)

;; iTunes tags
(def itunes-categories
  (with-open [reader (io/reader "resources/edn/itunes_categories.edn")]
    (edn/read (PushbackReader. reader))))

(defn valid-itunes-category? [[category sub-category]]
  (when-let [sub-categories (get itunes-categories category)]
    (if (empty? sub-categories) true ; Some categories don't have sub-categories, yet are still valid
        (contains? sub-categories sub-category))))

(s/def :itunes/author     string?)
(s/def :itunes/block      #{"Yes"})
(s/def :itunes/category   valid-itunes-category?)
(s/def :itunes/categories (s/and vector? seq (s/+ (s/cat :category :itunes/category))))
(s/def :itunes/complete   #{"Yes"})
(s/def :itunes/explicit   #{"clean" "yes" "no" "true" "false" true false})
(s/def :itunes/image      (s/keys :req-un [:url/href]))
(s/def :itunes/owner      (s/keys :req-un [:metadata/name :email/email]))
(s/def :itunes/subtitle   string?)
(s/def :itunes/summary    string?)
(s/def :itunes/type       #{"episodic" "serial"})

;; Aggregates
(s/def :metadata/itunes
  (s/keys :req [:itunes/categories
                :itunes/explicit
                :itunes/image]
          :opt [:itunes/author
                :itunes/block
                :itunes/complete
                :itunes/owner
                :itunes/subtitle
                :itunes/summary
                :itunes/type
                :itunes/new-feed-url]))

(s/def :config/metadata
  (s/keys :req [:metadata/title
                :metadata/description
                :metadata/language
                :metadata/atom
                :metadata/itunes]
          :opt [:metadata/copyright
                :metadata/link
                :metadata/image]))

(s/def :config/slug (s/and string? #(str/starts-with? % "/")))
(s/def :config/xml-file-path string?)
(s/def :config/port (s/and number? (complement zero?) #(> 65535 %)))
(s/def :config/poll-rate-in-seconds (s/and number? pos?))

(s/def :castopod/base-url :url/url)

(s/def :config/castopod
  (s/keys :req-un [:castopod/base-url]))

(s/def :config/valid
  (s/keys :req [:config/metadata
                :config/port
                :config/slug]
          :opt [:config/castopod
                :config/feeds
                :config/host-url
                :config/poll-rate-in-seconds
                :config/xml-file-path]))

(comment
  (s/valid? :podcast/feed "https://hello-sailor.xml")  ; t
  (s/valid? :podcast/feed "https://hello-sailor.cave") ; f
  (s/valid? :podcast/feed "hello-sailor.xml")          ; f

  (valid-itunes-category? ["Government"])
  (valid-itunes-category? ["Pizza"])
  (valid-itunes-category? ["Arts" "Books"])

  )
