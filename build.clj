(require 'cljs.build.api)

(cljs.build.api/watch
  "src/cljs"
  {:main          "custom-module-loaders.core"
   :output-to     "out/custom-module-loaders.js"
   :output-dir    "out"
   :optimizations :none
   :verbose       true
   :libs  [{:path "node_modules/react"
            :name "React"
            :files ["react.js" "addons.js" "lib/"]
            :module-type :commonjs}]})

; (cljs.build.api/watch
;   "src/cljs"
;   {:main          "custom-module-loaders.core"
;    :output-to     "out/custom-module-loaders.js"
;    :output-dir    "out"
;    :optimizations :none
;    :verbose       true
;    :libs  [{:path "node_modules/react"
;             :name "react"
;             :files ["react.js" "addons.js" "lib/"]
;             ; :main "react.js"
;             :module-type :commonjs}
;            {:path "node_modules/react-tap-event-plugin"
;             :name "react-tap-event-plugin"
;             :files ["src/"]
;             ; :main "src/injectTapEventPlugin.js"
;             :module-type :commonjs}
;            {:path "node_modules/material-ui/node_modules/react-draggable2"
;             :name "react-draggable2"
;             :src ["lib/"]
;             ; :main "lib/draggable.js"
;             :module-type :commonjs}
;            {:path "node_modules/material-ui/material-ui"
;             :name "material-ui"
;             :files ["lib/"]
;             ; :main ["lib/index.js"]
;             :module-type :commonjs}
;            "out/module"]})
