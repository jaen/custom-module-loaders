(ns custom-module-loaders.core
  (:require [module$react$react :as React]
            [reagent.core :as reagent]))

(defn increment-button [counter text]
  [:button {:on-click (fn [e]
                        (swap! counter inc)
                        (.preventDefault e))}
           text])

(defn test-component []
  (let [counter (reagent/atom 0)]
    (fn []
      [:div {:style {:color "red"}}
        [:p "Test: " @counter]
        [increment-button counter "Click me!"]])))

  (reagent/render [test-component] (.-body js/document))
