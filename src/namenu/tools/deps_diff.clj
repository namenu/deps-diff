(ns namenu.tools.deps-diff
  (:require [clojure.spec.alpha :as s]
            [namenu.deps-diff.core :as dd]
            [namenu.deps-diff.output :as output]
            [namenu.deps-diff.spec :as spec]))

(defn diff
  "
  opts
    :base - tools.deps resolved edn of base deps.edn
    :target - tools.deps resolved edn of target deps.edn
    :format - #{:edn, :markdown, :cli}
  "
  [{:keys [base target format] :as opts}]
  (assert (s/valid? ::spec/ref base))
  (assert (s/valid? ::spec/ref target))
  (assert (s/valid? ::spec/format format))

  (let [deps-from (dd/parse-resolved-tree base)
        deps-to   (dd/parse-resolved-tree target)

        d         (dd/diff* deps-from deps-to)
        exit-code (if (output/equal? d) 0 1)]
    (dd/print-result! d opts)
    (System/exit exit-code)))

(comment

  (deps/resolve-deps (assoc (edn/read-string (slurp "deps.edn"))
                       :mvn/repos mvn/standard-repos) {})

  )
