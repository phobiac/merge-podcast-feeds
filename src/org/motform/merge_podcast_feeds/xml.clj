(ns org.motform.merge-podcast-feeds.xml
  (:require [clojure.data.xml     :as xml]
            [clojure.data.zip.xml :as zip-xml]
            [clojure.java.io      :as io]
            [clojure.zip          :as zip]
            [org.motform.merge-podcast-feeds.date :as date]))

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

(defn collect-and-sort-feeds
  "Return seq of <item> nodes from feed urls in `feeds`."
  [feeds]
  (->> feeds
       (map parse-xml-feed-and-return-items)
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
    (conj channel [:pubDate (date/RFC1123-now)])]))

(defn emit-indent-xml
  "Spit `xml` to resources/xml/test.xml"
  [xml path]
  (with-open [output-file (io/writer path)]
    (xml/indent xml output-file)))

(defn emit-xml
  "Spit an `xml` to `filename`, should be passed with file extension."
  [xml path]
  (with-open [output-file (io/writer path)]
    (xml/emit xml output-file)))
