(ns namenu.deps-diff.spec
  (:require [clojure.spec.alpha :as s]
            [clojure.tools.deps.specs :as deps.specs]))

(s/def ::ref (s/or :gitrevision string?
                   :filename string?))
(s/def ::aliases (s/* keyword?))

(s/def ::format (s/nilable #{:edn :markdown :cli}))

(s/def ::diff (s/keys :req-un [:diff/removed :diff/added :diff/modified]))

(s/def ::coord ::deps.specs/coord)

(s/def :diff/removed (s/map-of symbol? ::coord))
(s/def :diff/added (s/map-of symbol? ::coord))
(s/def :diff/modified (s/map-of symbol? ::coord))
