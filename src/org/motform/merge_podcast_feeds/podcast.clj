(ns org.motform.merge-podcast-feeds.podcast
  "Namespace for assembling the podcast feed.
  `make-channel!` eats the metadata portion of the configuration file
  and makes the <channel> as hiccup, ready to be used when assembling
  the final, merged feed with `assemble-feed!`.

  `web-feed` returns the indented string representation, while `output-test-feed`
  can be used for debugging config files and feed links.

  This namespace assumes all config validation is taken care of by `org.motform.merge-podcast-feeds.config`."
  (:require [chime.core :as chime]
            [clojure.data.xml     :as xml]
            [clojure.data.zip.xml :as zip-xml]
            [clojure.java.io      :as io]
            [clojure.zip          :as zip]
            [org.motform.merge-podcast-feeds.config :as config]
            [org.motform.merge-podcast-feeds.castopod :as castopod])
  (:import (java.time ZonedDateTime Instant Duration ZoneId)
           (java.time.format DateTimeFormatter)))

(def ^:private *state
  (atom #:state{:channel  nil
                :xml-feed nil
                :str-feed nil}))

;;; Utility

(defn- concatv
  "Return a vector representing the concatenation of the elements in the supplied colls."
  [x y]
  (into [] (concat x y)))

;; Date handling

(defn- parse-RFC1123-date
  "Return comparable `ZonedDateTime` instance from parsed RFC1123 date string."
  [^String rfc1123-date]
  (let [formatter (DateTimeFormatter/RFC_1123_DATE_TIME)]
    (ZonedDateTime/parse rfc1123-date formatter)))

(defn RFC1123-date
  "Return string current time formatted per RFC1123."
  []
  (let [formatter (DateTimeFormatter/RFC_1123_DATE_TIME)
        now       (ZonedDateTime/now)]
    (. formatter format now)))

(defn- instant->RFC1123 [instant]
  (.. (DateTimeFormatter/RFC_1123_DATE_TIME)
      (withZone (ZoneId/systemDefault))
      (format instant)))

;;; XML wrangling

(defn- parse-xml-feed-and-return-items
  "Parse xml at `feed-url` and return seq with podcast items, i.e. all <item>."
  [feed-url]
  (with-open [stream (io/input-stream feed-url)]
    (let [xml (xml/parse stream)
          items (zip-xml/xml-> (zip/xml-zip xml) :channel :item)]
      (map zip/node items))))

(defn- zip-from-root-to-episode-insertion
  "Goes from the root of `xml-zip`, assumed to be <channel>
  to the loc where that is, episodes are expeceted."
  [xml-zip]
  (-> xml-zip zip/down zip/rightmost))

(defn- insert-rightmost
  "Insert `items` at the loc of `xml-zip` in list order."
  [xml-zip items]
  (reduce
   (fn [xml-zip item] (zip/insert-right xml-zip item))
   (-> xml-zip zip/down zip/rightmost)
   items))

(defn- publication-date
  "Return the podcast item `:pubDate`, a string in RFC-1123 format."
  [item]
  (-> (zip/xml-zip item)
      (zip-xml/xml1-> :pubDate)
      zip/node
      :content
      first))

(defn- collect-and-sort-feeds
  "Return seq of <item> nodes from feed urls in `feeds`."
  [feeds]
  (->> feeds
       (map parse-xml-feed-and-return-items)
       (apply concat)
       (sort-by (comp parse-RFC1123-date publication-date))))

(defn- append-podcast-feeds
  "Append `feeds` to `xml` in order at rightmost position."
  [xml feeds]
  (-> xml
      zip/xml-zip
      zip-from-root-to-episode-insertion
      (insert-rightmost feeds)
      zip/root))

(defn hiccup-channel->xml-with-pubDate
  "Return map XML of feed, contaning a hiccup formatted <channel>
  in an <rss> tag containing the most common podcasting xml namespaces.

  TODO:
  It might be a better idea to include the map of namespaces as an argument,
  but the current state of the program (230125) does not support those anyway."
  [channel]
  (xml/sexp-as-element
   [:rss
    {:version "2.0"
     :xmlns/itunes  "http://www.itunes.com/dtds/podcast-1.0.dtd"
     :xmlns/atom    "http://www.w3.org/2005/Atom"
     :xmlns/podcast "https://github.com/Podcastindex-org/podcast-namespace/blob/main/docs/1.0.md"
     :xmlns/content "http://purl.org/rss/1.0/modules/content/"}
    (conj channel [:pubDate (RFC1123-date)])]))

;;; <rss> and <channel> assembly

;; TODO: Register these based on user config?
;;       They are curently mirroring the namespaces used in `hiccup-channel->xml-with-pubDate`.
(xml/alias-uri 'atom    "http://www.w3.org/2005/Atom")
(xml/alias-uri 'content "http://purl.org/rss/1.0/modules/content/")
(xml/alias-uri 'itunes  "http://www.itunes.com/dtds/podcast-1.0.dtd")
(xml/alias-uri 'podcast "https://github.com/Podcastindex-org/podcast-namespace/blob/main/docs/1.0.md")

(defn- xmlns-alias-keyword
  "Qualify `k` in namespace registered by `xml/alias-uri`.
  Shoutout to @delaguardo in clojurians for helping out."
  [xmlns-alias k]
  (let [xmlns (name (ns-name (get (ns-aliases 'org.motform.merge-podcast-feeds.podcast) xmlns-alias)))]
    (keyword xmlns (name k))))

(def itunes-tag (partial xmlns-alias-keyword 'itunes))

(defn- itunes-categories
  "Return hiccup representation of nested itunes categories."
  [categories]
  (for [[category sub-category] categories]
    [::itunes/category {:text category}
     (when sub-category
       [::itunes/category {:text sub-category}])]))

;; TODO XMLNS
(defn- itunes-tags
  "Return hiccup representation of the itunes tags from user config."
  [itunes-metadata]
  (reduce-kv
   (fn [tags tag v]
     (let [conj-fn (if (= :itunes/categories tag) concatv conj)
           tag' (case tag
                  :itunes/categories (itunes-categories v)
                  :itunes/image     [::itunes/image {:href (:href v)}]
                  :itunes/owner     [::itunes/owner
                                     [::itunes/name (:name v)]
                                     [::itunes/email (:email v)]]
                  [(itunes-tag tag) v])]
       (conj-fn tags tag')))
   [] itunes-metadata))

(defn- unqualify-key
  "Return `k` as unqualified keyword."
  [k]
  (keyword (name k)))

(defn- metadata-tags
  "Return hiccup representation of the metadata tags from user config."
  [metadata]
  (reduce-kv
   (fn ([tags tag v]
        (let [tag' (if (= tag :metadata/image)
                     [:image ; <image> is the only non-namespaced "complex" tag
                      [:url (:url v)]
                      [:title (:title v)]
                      [:link (:link v)]]
                     [(unqualify-key tag) v])]
          (conj tags tag'))))
   [] metadata))

(defn- atom-link
  "Return hiccup representation of <atom/link>."
  [feed-url]
  [::atom/link
   [:href feed-url
    :rel  "self"
    :type "application/rss+xml"]])

(defn- ->hiccup-channel
  "Return hiccup representation of `<rss><channel> ... </channel></rss>`.
  To be consumed by `clojure.data.xml/sexp-as-element`.
  It will most likely be done by `org.motform.merge-podcast-feeds.xml/hiccup-feed->xml-with-pubDate`.

  It is assumed that a config file is stable given the lifetime of the program.
  As such, the caller will have to perform `sexp-as-element` themselves, preferably
  after adding <lastBuildDate>."
  [metadata]
  (-> [:channel]
      (concatv (metadata-tags (dissoc metadata :metadata/itunes :metadata/atom)))
      (conj (atom-link (:metadata/atom metadata)))
      (concatv (itunes-tags (get  metadata :metadata/itunes)))
      (conj [:generator "https://github.com/motform/merge-podcast-feeds"])))

(defn- feed-urls
  "Return the podcast feed urls from the configured feeds or from castopod."
  []
  (if-let [feeds (config/get-in [:config/feeds])]
    feeds
    (castopod/podcast-feed-urls)))

;;; Main functions

(defn make-channel! []
  (let [metadata (config/get-in [:config/metadata])
        channel  (->hiccup-channel metadata)]
    (swap! *state assoc :state/channel channel)))

(defn assemble-feed!
  "TODO: Document how the feed is updated here."
  []
  (let [channel     (get @*state :state/channel)
        feeds       (collect-and-sort-feeds (feed-urls))
        xml-no-feed (hiccup-channel->xml-with-pubDate channel)
        xml         (append-podcast-feeds xml-no-feed feeds)]
    (swap! *state assoc
           :state/xml-feed xml
           :state/str-feed (xml/indent-str xml))))

(defn output-test-feed []
  (let [path (config/get-in [:config/xml-file-path])]
    (with-open [output-file (io/writer path)]
      (xml/indent (get @*state :state/xml-feed) output-file))))

(defn web-feed []
  (get @*state :state/str-feed))

;;; Polling

(def ^:private every-10-minutes
  (chime/periodic-seq (Instant/now) (Duration/ofMinutes 10)))

(defn poll-for-feed-updates [period]
  (chime/chime-at period #(do (println "[" (instant->RFC1123 %) "] Polling feeds." "")
                              (assemble-feed!))))
(comment
  (poll-for-feed-updates every-10-minutes))

