(ns org.motform.merge-podcast-feeds.core
  (:require [clojure.data.xml :as xml]
            [clojure.data.zip.xml :as zip-xml]
            [clojure.java.io :as io]
            [clojure.zip :as zip]
            [org.motform.merge-podcast-feeds.podcast :as podcast])
  (:import (java.time ZonedDateTime)
           (java.time.format DateTimeFormatter)))

(defn parse-xml-feed
  "Return `clojure.data.xml` representation of `feed-url`."
  [feed-url] ; TODO: Wrap this in `with-open`?
  (-> feed-url io/input-stream xml/parse))

(defn episodes
  "Return seq with podcast episodes, i.e. all <item>."
  [xml]
  (map zip/node (zip-xml/xml-> (zip/xml-zip xml) :channel :item)))

(defn zip-from-root-to-episode-insertion
  "Goes from the root of `xml-zip`, assumed to be <channel>
  to the loc where that is, episodes are expeceted."
  [xml-zip]
  (-> xml-zip zip/down zip/rightmost))

(defn insert-rightmost
  "Insert `items` at the loc of `xml-zip` in list order."
  [xml-zip items]
  (reduce
   (fn [xml-zip item] (zip/left (zip/insert-right xml-zip item)))
   (-> xml-zip zip/down zip/rightmost)
   items))

(defn emit-test-xml
  "Spit `xml` to resources/xml/test.xml"
  [xml]
  (with-open [output-file (io/writer "resources/xml/test.xml")]
    (xml/indent xml output-file)))

(defn emit-xml
  "Spit an `xml` to `filename`, should be passed with file extension."
  [xml filename]
  (with-open [output-file (io/writer (str "resources/xml/" filename))]
    (xml/emit xml output-file)))

(defn publication-date
  "Return the podcast item `:pubDate`, a string in RFC-1123 format."
  [item]
  (-> (zip/xml-zip item)
      (zip-xml/xml1-> :pubDate)
      zip/node
      :content
      first))

(defn parse-RFC1123-date
  "Return comparable `ZonedDateTime` instance from parsed RFC1123 date string."
  [^String rfc1123-date]
  (let [formatter (DateTimeFormatter/RFC_1123_DATE_TIME)]
    (ZonedDateTime/parse rfc1123-date formatter)))

(defn RFC1123-now
  "Return string of current now formatted in RFC1123."
  []
  (let [formatter (DateTimeFormatter/RFC_1123_DATE_TIME)
        now       (ZonedDateTime/now)]
    (. formatter format now)))

(comment
  (require '[clojure.inspector :as inspect])

  (def feeds
    (->> ["https://pod.alltatalla.se/@rekreation/feed.xml"
          "https://pod.alltatalla.se/@omvarldar/feed.xml"]
         (map (comp episodes parse-xml-feed))
         (apply concat)
         (sort-by (comp parse-RFC1123-date publication-date))))

  ;; Test parse and emit
  (-> (podcast/preamble-&-metadata (RFC1123-now))
      zip/xml-zip
      zip-from-root-to-episode-insertion
      (insert-rightmost feeds)
      zip/root
      emit-test-xml)
  )
