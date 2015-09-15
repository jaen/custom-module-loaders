(require 'process-libs '[clojure.string :as string])

(def lib-opts
  {:libs [
    {:root "node_modules/react"
     :name "React"
     :files [#_"react.js" "addons.js" "lib/"]
     :main "react.js"
     :module-type :commonjs
     :module-mapping {"lib/cx.js" "React.classSet"
                      "addons.js" "React" ; "ReactWithAddons"
                    }
     :module-mapping-fn (fn [path mapping]
                          (let [lookup (get mapping path)]
                            (or lookup
                                (str "React." (process-libs/path-to-module-name (string/replace path #"lib/" ""))))))}
           ]
  :lib-out-dir "libs"})

(process-libs/process-js-libraries lib-opts)

(require 'cljs.build.api)

(cljs.build.api/watch
  "src/cljs"
  {:main          "custom-module-loaders.core"
   ; :language-in   :ecmascript6-strict
   :output-to     "out/custom-module-loaders.js"
   :output-dir    "out"
   :optimizations :none
   :verbose       true
   :libs  ["libs"]})
