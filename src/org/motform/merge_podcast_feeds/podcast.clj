(ns org.motform.merge-podcast-feeds.podcast
  "Namespace for assembling the metadata entries in <channel>.
  `config->channel` eats the metadata portion of the configuration file
  which it silently assumes to be validated by `org.motform.merge-podcast-feeds.config`.

  The process is specefic to the prescribed format of the config file.
  I decided against listening to those nasty generalisation instincts.
  As such, hard-coded if's abound.")

(defn- concatv [x y]
  (into [] (concat x y)))

(defn- itunes-categories [categories]
  (for [[category sub-category] categories]
    [:itunes/category {:text category}
     (when sub-category
       [:itunes/category {:text sub-category}])]))

;; TODO XMLNS
(defn- itunes-tags [itunes-metadata]
  (reduce-kv
   (fn [tags tag v]
     (let [conj-fn (if (= :itunes/categories tag) concatv conj)
           tag' (case tag
                  :itunes/categories (itunes-categories v)
                  :itunes/image [:itunes/image {:href (:href v)}]
                  :itunes/owner [:itunes/owner
                                 [:itunes/name (:name v)]
                                 [:itunes/email (:email v)]]
                  [tag v])]
       (conj-fn tags tag')))
   [] itunes-metadata))

(defn- metadata-tags [metadata]
  (reduce-kv
   (fn ([tags tag v]
        (let [tag' (if (= tag :metadata/image)
                     [:image ; <image> is the only non-namespaced "complex" tag
                      [:url (:url v)]
                      [:title (:title v)]
                      [:link (:link v)]]
                     [tag v])]
          (conj tags tag'))))
   [] metadata))

(defn- atom-link [feed-url]
  [:atom/link ; TODO xmlns
   [:href feed-url
    :rel  "self"
    :type "application/rss+xml"]])

(defn config->hiccup-channel
  "Return hiccup formatted `<rss><channel> ... </channel></rss>` structure
  to be consumed by `clojure.data.xml/sexp-as-element` .
  It will most likely be done by `org.motform.merge-podcast-feeds.xml/hiccup-feed->xml-with-pubDate`.

  It is assumed that a config file is stable given the lifetime of the program.
  As such, the caller will have to perform `sexp-as-element` themselves, preferably
  after adding <lastBuildDate>."
  [{:config/keys [metadata]}]
  (-> [:channel]
      (concatv (metadata-tags (dissoc metadata :metadata/itunes :metadata/atom)))
      (conj (atom-link (:metadata/atom metadata)))
      (concatv (itunes-tags   (get  metadata :metadata/itunes)))
      (conj [:generator "https://github.com/motform/merge-podcast-feeds"])))
