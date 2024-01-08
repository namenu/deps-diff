(ns user
  (:require [clojure.tools.deps.cli.api :as cli]))


(comment
  (cli/tree {:project "./test-resources/base/deps.edn"
             :aliases [:dev]}))