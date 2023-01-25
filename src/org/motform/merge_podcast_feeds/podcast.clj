(ns org.motform.merge-podcast-feeds.podcast
  "Namespace for assembling the metadata entries in <channel>.
  `config->channel` eats the metadata portion of the configuration file
  which it silently assumes to be validated by `org.motform.merge-podcast-feeds.config`.

  It deals with the hiccup formatted data, whereas `org.motform.merge-podcast-feeds.xml`
  deals with `clojure.data.xml`s xml maps and zippers.

  The process is specefic to the prescribed format of the config file.
  I decided against listening to those nasty generalisation instincts.
  As such, hard-coded if's abound."
  (:require [clojure.data.xml :as xml]))

(defn- concatv
  "Return a vector representing the concatenation of the elements in the supplied colls."
  [x y]
  (into [] (concat x y)))

;; TODO: Register these based on user config
;; They are curently mirroring the namespaces used in
;;`org.motform.merge-podcast-feeds.xml/hiccup-channel->xml-with-pubDate`
(xml/alias-uri 'atom    "http://www.w3.org/2005/Atom")
(xml/alias-uri 'content "http://purl.org/rss/1.0/modules/content/")
(xml/alias-uri 'itunes  "http://www.itunes.com/dtds/podcast-1.0.dtd")
(xml/alias-uri 'podcast "https://github.com/Podcastindex-org/podcast-namespace/blob/main/docs/1.0.md")

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
                  :itunes/author    [::itunes/author v]
                  :itunes/block     [::itunes/block v]
                  :itunes/categories (itunes-categories v)
                  :itunes/complete  [::itunes/complete v]
                  :itunes/explicit  [::itunes/explicit v]
                  :itunes/image     [::itunes/image {:href (:href v)}]
                  :itunes/owner     [::itunes/owner
                                     [::itunes/name (:name v)]
                                     [::itunes/email (:email v)]]
                  :itunes/subtitle  [::itunes/subtitle v]
                  :itunes/summary   [::itunes/summary v]
                  :itunes/type      [::itunes/type v])]
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

(defn config->hiccup-channel
  "Return hiccup representation of `<rss><channel> ... </channel></rss>`.
  To be consumed by `clojure.data.xml/sexp-as-element`.
  It will most likely be done by `org.motform.merge-podcast-feeds.xml/hiccup-feed->xml-with-pubDate`.

  It is assumed that a config file is stable given the lifetime of the program.
  As such, the caller will have to perform `sexp-as-element` themselves, preferably
  after adding <lastBuildDate>."
  [{:config/keys [metadata]}]
  (-> [:channel]
      (concatv (metadata-tags (dissoc metadata :metadata/itunes :metadata/atom)))
      (conj (atom-link (:metadata/atom metadata)))
      (concatv (itunes-tags (get  metadata :metadata/itunes)))
      (conj [:generator "https://github.com/motform/merge-podcast-feeds"])))
