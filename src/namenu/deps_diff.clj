(ns namenu.deps-diff
  (:require [clojure.data :as data]
            [clojure.edn :as edn]
            [clojure.set :as set]
            [clojure.spec.alpha :as s]
            [namenu.deps-diff.output :as output]
            [namenu.deps-diff.spec :as spec]))

(defn- parse-resolved-tree [file]
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
    {:removed  (into {} removed)
     :added    (into {} added)
     :modified (into {} modified)}))

(defn diff
  "
  opts
    :base - tree shaped edn of base deps.edn
    :target - tree shaped edn of target deps.edn
    :format - #{:edn, :markdown, :cli}
  "
  [{:keys [base target format] :as opts}]
  (assert (s/valid? ::spec/ref base))
  (assert (s/valid? ::spec/ref target))
  (assert (s/valid? ::spec/format format))

  (let [deps-from (parse-resolved-tree base)
        deps-to   (parse-resolved-tree target)

        d         (diff* deps-from deps-to)
        exit-code (if (output/equal? d) 0 1)]
    (print-result! d opts)
    (System/exit exit-code)))

(comment
  ;; git show e0f4689c07bc652492bf03eba7edac20ab2bee0f:test/resources/base.edn > base.edn
  ;; clojure -X namenu.deps-diff/diff base.edn deps.edn

  (diff*
    (parse-resolved-tree "test-resources/__base.edn")
    (parse-resolved-tree "test-resources/__target.edn"))

  (make-ver
    (s/conform ::spec/coord
               {:git/url       "https://github.com/green-labs/superlifter.git",
                :git/sha       "e0df5b36b496c485c75f38052a71b18f02772cc0",
                :deps/manifest :deps,
                :deps/root     "/Users/namenu/.gitlibs/libs/superlifter/superlifter/e0df5b36b496c485c75f38052a71b18f02772cc0",
                :dependents    ['green-labs/gosura],
                :parents       #{['green-labs/gosura]},
                :paths         ["/Users/namenu/.gitlibs/libs/superlifter/superlifter/e0df5b36b496c485c75f38052a71b18f02772cc0/src"]}
               )))
