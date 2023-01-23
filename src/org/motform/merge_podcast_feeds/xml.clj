(ns org.motform.merge-podcast-feeds.xml
  (:require [clojure.data.xml     :as xml]
            [clojure.data.zip.xml :as zip-xml]
            [clojure.java.io      :as io]
            [clojure.zip          :as zip]
            [org.motform.merge-podcast-feeds.date :as date]))

(defn parse-xml-feed
  "Return `clojure.data.xml` representation of `feed-url`."
  [feed-url] ; TODO: Wrap this in `with-open`?
  (-> feed-url io/input-stream xml/parse))

(defn- episodes
  "Return seq with podcast episodes, i.e. all <item>."
  [xml]
  (map zip/node (zip-xml/xml-> (zip/xml-zip xml) :channel :item)))

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

(defn collect-and-sort-feeds
  "Return seq of <item> nodes from feed urls in `feeds`."
  [feeds]
  (->> feeds
       (map (comp episodes parse-xml-feed))
       (apply concat)
       (sort-by (comp date/parse-RFC1123 publication-date))))

(defn append-podcast-feeds
  "Append `feeds` to `xml` in order at rightmost position."
  [xml feeds]
  (-> xml
      zip/xml-zip
      zip-from-root-to-episode-insertion
      (insert-rightmost feeds)
      zip/root))

(defn hiccup-channel->xml-with-pubDate [channel]
  (xml/sexp-as-element
   [:rss (conj channel [:pubDate (date/RFC1123-now)])]))

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
