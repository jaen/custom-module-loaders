(ns custom-module-loaders.core
  (:require ;React
            [reagent.core :as reagent]))

; (set! (.-React js/window) js/React$ReactWithAddons)

; (def TestComponent
;   (React/createClass
;     #js {:displayName "TestComponent"
;          :render (fn []
;                    (React/createElement
;                      "div"
;                      #js {:style #js {:color "red"}}
;                      "Test"))}))
;
; (React/render
;   (React/createElement TestComponent nil)
;   (.-body js/document))

(defn test-component []
  (let [counter (reagent/atom 0)]
    (fn []
      [:div {:style {:color "red"}}
        [:p "Test: " @counter]
        [:button {:on-click (fn [e]
                         (swap! counter inc)
                         (.preventDefault e))}
            "Click Me!"]])))

(reagent/render [test-component] (.-body js/document))
