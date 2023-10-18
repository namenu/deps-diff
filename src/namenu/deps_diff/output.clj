(ns namenu.deps-diff.output
  (:require [clj-commons.ansi :as ansi]
            [clojure.string :as str]))

(defn make-ver [[type coord]]
  (case type
    :mvn (:mvn/version coord)
    :local (:local/root coord)
    :git (cond-> coord
                 :git/tag
                 :git/sha)))

(def assets-url
  {:removed  "https://img.shields.io/badge/Removed-red"
   :added    "https://img.shields.io/badge/Added-green"
   :modified "https://img.shields.io/badge/Modified-blue"})

(defn ver-string
  "Creates a version string from a map of :from and :to version data.

  Handles the case where only :to or :from is precent (because of adding/removing a dependency)."
  [{:keys [from to] :as ver}]
  (cond
    (and (some? from) (some? to)) (str (make-ver from) " -> " (make-ver to))
    (some? to) (make-ver to)
    (some? from) (make-ver from)))

(defn make-row [operation [dep ver]]
  (str "| ![](" (get assets-url operation) ") | `" dep "` | " (ver-string ver) " |"))

(defn markdown
  [{:keys [removed added modified]}]
  (let [table-header ["| Operation | Artifact  | Version |"
                      "| --------- | --------- | ------- |"]
        lines        (concat
                       table-header
                       (map #(make-row :removed %) removed)
                       (map #(make-row :added %) added)
                       (map #(make-row :modified %) modified))]
    (println (str/join "\n" lines))))

(defn equal? [{:keys [removed added modified]}]
  (and (empty? removed)
       (empty? added)
       (empty? modified)))

(defn cli [{:keys [removed added modified] :as data}]
  (if (equal? data)
    (println "No changes detected.")
    (do
      (run! (fn [[name ver]]
              (ansi/pcompose
                [{:font  :red
                  :width 10} "Removed"]
                "  "
                [{:font  :white
                  :width 60
                  :pad   :right}
                 name
                 "  "]
                [:yellow
                 (ver-string ver)]))
            removed)

      (run! (fn [[name ver]]
              (ansi/pcompose
                [{:font  :green
                  :width 10} "Added"]
                "  "
                [{:font  :white
                  :width 60
                  :pad   :right}
                 name
                 "  "]
                [:yellow
                 (ver-string ver)]))
            added)

      (run! (fn [[name ver]]
              (ansi/pcompose
                [{:font  :blue
                  :width 10} "Modified"]
                "  "
                [{:font  :white
                  :width 60
                  :pad   :right}
                 name
                 "  "]
                [:yellow
                 (ver-string ver)]))
            modified))))

(comment
  (do (ansi/pcompose
        [{:font  :white
          :width 80} "org.codehaus.plexus/plexus-sec-dispatcher"
         "  "]
        [{:font  :red
          :width 8
          :pad   :right} "Removed"]
        "  "
        [{:font :yellow}
         "1.0.0"])
      (ansi/pcompose
        [{:font  :white
          :width 80} "org.ow2.asm/asm"
         "  "]
        [{:font  :green
          :width 8
          :pad   :right} "Added"]
        "  "
        [{:font :yellow}
         "1.2.3.4"])
      (ansi/pcompose
        [{:font  :white
          :width 80} "org.eclipse.jetty/jetty-client"
         "  "]
        [{:font  :blue
          :width 8
          :pad   :right} "Modified"]
        "  "
        [{:font :yellow}
         "1.0 -> 2.0"])))
