(ns user
  (:require [clojure.tools.deps.cli.api :as cli]))

(comment
  (= (with-out-str
       (cli/tree {:project "./test-resources/base/deps.edn"
                  :aliases [:dev]}))
     (with-out-str
       (cli/tree {:dir     "./test-resources/base"
                  :aliases [:dev]})))
  )