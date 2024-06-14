(ns namenu.deps-diff.core
  (:require [clojure.data :as data]
            [clojure.edn :as edn]
            [clojure.set :as set]
            [clojure.spec.alpha :as s]
            [namenu.deps-diff.output :as output]
            [namenu.deps-diff.spec :as spec]))

(defn parse-resolved-tree
  "Takes tools.deps resolved output filename as an input.
  Returns a map of [name -> ::spec/coord]"
  [file]
  (let [deps-tree (edn/read-string (slurp (str file)))]
    (letfn [(walk [m]
              (mapcat (fn [[k v]]
                        (into [[k (dissoc v :children)]]
                              (walk (:children v))))
                      m))]
      (->> (walk (:children deps-tree))
           (keep (fn [[k v]]
                   (if (:include v)
                     [k (s/conform ::spec/coord (:coord v))])))
           (into {})))))

(defmulti print-result! (fn [_ opts] (keyword (:format opts))))

(defmethod print-result! :default
  [v _]
  (prn v))

(defmethod print-result! :markdown
  [data _]
  (output/markdown data))

(defmethod print-result! :cli
  [data _]
  (output/cli data))

(defn diff*
  "Returns a map of :removed, :added and :modified dependencies.
  Each key is a dependency name and the value is a map of :from and :to versions"
  [deps-from deps-to]
  (let [key-set (comp set keys)
        [removed-deps added-deps common-deps] (data/diff (key-set deps-from) (key-set deps-to))
        removed (map (fn [k] [k {:from (get deps-from k)}]) removed-deps)
        added (map (fn [k] [k {:to (get deps-to k)}]) added-deps)
        modified-keys (keys (set/difference (set (select-keys deps-to common-deps))
                                            (select-keys deps-from common-deps)))
        modified (map (fn [k] [k {:from (get deps-from k)
                                  :to   (get deps-to k)}])
                      modified-keys)]
    {:removed  (into (sorted-map) removed)
     :added    (into (sorted-map) added)
     :modified (into (sorted-map) modified)}))
