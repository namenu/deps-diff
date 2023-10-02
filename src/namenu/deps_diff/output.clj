(ns namenu.deps-diff.output
  (:require [clj-commons.ansi :as ansi]))

(defn make-ver [[type coord]]
  (case type
    :mvn (:mvn/version coord)
    :local (:local/root coord)
    :git (cond-> coord
                 :git/tag
                 :git/sha)))

(defn make-row [[dep ver]]
  (str "| `" dep "` | " (make-ver ver) " |"))

(defn markdown
  [{:keys [removed added modified]} _]
  (let [table-header ["| Artifact            | version |"
                      "| ------------------- | ------  |"]
        lines        (concat
                       (when (seq removed)
                         (concat ["![Static Badge](https://img.shields.io/badge/Removed-red)"]
                                 table-header
                                 (map make-row removed)
                                 [""]))
                       (when (seq added)
                         (concat ["![Static Badge](https://img.shields.io/badge/Added-green)"]
                                 table-header
                                 (map make-row added)
                                 [""]))
                       (when (seq modified)
                         (concat ["![Static Badge](https://img.shields.io/badge/Modified-blue)"]
                                 table-header
                                 (map make-row modified))))]
    (println (str/join "\n" lines))))

(defn cli [{:keys [removed added modified]}]
  (println "Comparing between base and target:")
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
             (make-ver ver)]))
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
             (make-ver ver)]))
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
             (make-ver ver)]))
        modified))


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
         "2.0"])))