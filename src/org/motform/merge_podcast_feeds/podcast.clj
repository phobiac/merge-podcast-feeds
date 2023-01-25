(ns org.motform.merge-podcast-feeds.podcast
  "Namespace for assembling the metadata entries in <channel>.
  `config->channel` eats the metadata portion of the configuration file
  which it silently assumes to be validated by `org.motform.merge-podcast-feeds.config`.

  It deals with the hiccup formatted data, whereas `org.motform.merge-podcast-feeds.xml`
  deals with `clojure.data.xml`s xml maps and zippers.

  The process is specefic to the prescribed format of the config file.
  I decided against listening to those nasty generalisation instincts.
  As such, hard-coded if's abound."
  (:require [clojure.data.xml :as xml]
            [org.motform.merge-podcast-feeds.config :as config]
            [org.motform.merge-podcast-feeds.xml    :as motform.xml]))

(def *state
  (atom #:state{:feed    nil
                :channel nil}))

(defn feed []
  (get @*state :state/feed))

(defn- concatv
  "Return a vector representing the concatenation of the elements in the supplied colls."
  [x y]
  (into [] (concat x y)))

;; TODO: Register these based on user config?
;; They are curently mirroring the namespaces used in
;;`org.motform.merge-podcast-feeds.xml/hiccup-channel->xml-with-pubDate`
(xml/alias-uri 'atom    "http://www.w3.org/2005/Atom")
(xml/alias-uri 'content "http://purl.org/rss/1.0/modules/content/")
(xml/alias-uri 'itunes  "http://www.itunes.com/dtds/podcast-1.0.dtd")
(xml/alias-uri 'podcast "https://github.com/Podcastindex-org/podcast-namespace/blob/main/docs/1.0.md")

(defn- xmlns-alias-keyword
  "Qualify `k` in namespace registered by `xml/alias-uri`.
  Shoutout to @delaguardo in clojurians for helping out."
  [xmlns-alias k]
  (let [xmlns (name (ns-name (get (ns-aliases *ns*) xmlns-alias)))]
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

(defn ->hiccup-channel
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

;; TODO: Take into account the feed updating, depending on how we do it
(defn assemble-feed! []
  (let [feed-urls   (config/get-in [:config/feeds])
        channel     (get @*state :state/channel)
        feeds       (motform.xml/collect-and-sort-feeds feed-urls)
        xml-no-feed (motform.xml/hiccup-channel->xml-with-pubDate channel)
        xml         (motform.xml/append-podcast-feeds xml-no-feed feeds)]
    (swap! *state :state/feed xml)))

(defn make-channel! []
  (let [metadata (config/get-in [:config/metadata])
        channel  (->hiccup-channel metadata)]
    (swap! *state assoc :state/channel channel)))

(defn output-test-feed []
  (let [path (config/get-in [:config/xml-file-path])]
    (motform.xml/emit-indent-xml (feed) path)))
