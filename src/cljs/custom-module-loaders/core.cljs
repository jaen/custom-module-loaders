(ns custom-module-loaders.core
  (:require [react.index :as React]
            [material-ui.index :as material-ui]
            material-ui.styles.colors
            [react.lib.ReactPropTypes :as React.PropTypes]
            [custom-module-loaders.components :as c])
  (:import [material-ui.styles theme-manager]
           [react-tap-event-plugin injectTapEventPlugin]))

(injectTapEventPlugin)

(def theme-manager (theme-manager.))

(defn first-tab []
  (c/tab {:label "Item One"}
    (c/div {:style {:padding "20px"}}
      (c/h2 {} "Test Item One")
      (c/text-field {:floatingLabelText "I'm an input : " :hintText "Input Me!"})

      (c/br)
      (c/br)

      (c/raised-button
        {:label "Test Me!"
         :onClick (fn []
                    (js/alert "Tab Test" "Test Item One"))}))))

(defn second-tab []
  (c/tab {:label "Item Two"}
    (c/div {:style {:padding "20px"}}
      (c/h2 {} "Test Item Two")
      (c/raised-button
        {:label "Test Me!"
         :primary true
         :onClick (fn []
                    (js/alert "Tab Test" "Test Item Two"))}))))

(defn third-tab []
  (c/tab {:label "Item Three"}
    (c/div {:style {:padding "20px"}}
      (c/h2 {} "Test Item Three")
      (c/raised-button
        {:label "Test Me!"
         :secondary true
         :onClick (fn []
                    (js/alert "Tab Test" "Test Item Three"))}))))

(defn test-component-render-fn []
  (c/div {:className "test-class"}
    (c/raised-button
      {:label "Test"
       :primary true
       :onClick (fn [] (js/alert "Test" "Test"))})

    (c/br)
    (c/br)

    (c/paper {}
      (c/tabs {}
        (first-tab)

        (second-tab)

        (third-tab)))))

(def TestComponent
  (React/createClass
    #js {:displayName "TestComponent"
        :childContextTypes #js {:muiTheme React.PropTypes/object}
        :getChildContext (fn []
                           #js {:muiTheme (.getCurrentTheme theme-manager)})
        :render test-component-render-fn}))

(React/render (React/createElement TestComponent nil) (.-body js/document))
