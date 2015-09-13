(require 'process-libs)

(def lib-opts
  {:libs [
          {:root "node_modules/lodash.isplainobject/node_modules/lodash.keysin/node_modules/lodash.isarray"
           :name :lodash.isarray
           :module-type :commonjs
          :depth 3}
          {:root "node_modules/lodash.isplainobject/node_modules/lodash._basefor"
           :name :lodash._basefor
           :module-type :commonjs
           :depth 2}
          {:root "node_modules/lodash.isplainobject/node_modules/lodash.isarguments"
           :name :lodash.isarguments
           :module-type :commonjs
           :depth 2}
          {:root "node_modules/lodash.isplainobject/node_modules/lodash.keysin"
           :name :lodash.keysin
           :module-type :commonjs
           :depends #{:lodash.isarray :lodash.isarguments}
           :depth 2}
          {:root "node_modules/lodash.isplainobject"
           :name :lodash.isplainobject
           :depends #{:lodash.keysin :lodash.isarguments :lodash.isarray :lodash._basefor}
           :module-type :commonjs
           :depth 1}
          {:root "node_modules/performance-now"
           :name :performance-now
           :filter #(re-find #"/index\.js$" %)
           ; :files ["lib/performance-now.js"]
           :module-type :commonjs
           :depth 1}
          {:root "node_modules/raf"
           :name :raf
           :filter #(re-find #"/index\.js$" %)
           :depends [:performance-now]
           :module-type :commonjs
           :depth 1}
          {:root "node_modules/react"
           :name :react
           ;:files ["react.js" "addons.js" "/lib"]
           :module-type :commonjs
           :depth 1
           :filter #(and (not (re-find #"/dist/" %))
                        (or (re-find #"/index\.js$" %)
                            (re-find #"/addons\.js$" %)
                            (re-find #"/lib/" %)))}
          {:root "node_modules/react-tap-event-plugin"
           :name :react-tap-event-plugin
           :depends #{:react}
           :filter #(not (re-find #"/src/" %))
           ; :files ["/src"]
           :module-type :commonjs
           :depth 1}
          {:root "node_modules/material-ui/node_modules/react-draggable2"
           :name :react-draggable2
           :depends #{:react}
           :module-type :commonjs
           :filter #(re-find #"/index\.js$" %)
           :depth 2}
          {:root "node_modules/material-ui/material-ui"
           :name :material-ui
           :depends #{:react :react-tap-event-plugin :react-draggable2 :react-tap-event-plugin2}
           ;:filter #(re-find #"/src/" %)
           ; :preprocess {#"\.jsx$" :jsx}
           :module-type :commonjs
           :depth 1}
          {:root "node_modules/react-motion/lib"
           :name :react-motion
           :depends #{:react :raf :performance-now :lodash.isplainobject :lodash.keysin :lodash.isarguments :lodash.isarray :lodash._basefor}
           :module-type :commonjsS
           :depth 1}
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
