(ns custom-module-loaders.components
  (:require [react.index :as React]
            [material-ui.index :as material-ui]))

(defn div [{:as opts} & contents]
  (apply React/createElement
    "div"
    (clj->js opts)
    contents))

(defn h2 [{:as opts} & contents]
  (apply React/createElement
    "h2"
    (clj->js opts)
    contents))

(defn br []
  (apply React/createElement
    "br"
    nil
    nil))

(defn text-field [{:as opts}]
  (apply React/createElement
    material-ui/TextField
    (clj->js opts)
    nil))

(defn paper [{:as opts} & contents]
  (apply React/createElement
    material-ui/Paper
    (clj->js opts)
    contents))

(defn tabs [{:as opts} & contents]
  (apply React/createElement
    material-ui/Tabs
    (clj->js opts)
    contents))

(defn tab [{:as opts} & contents]
  (apply React/createElement
    material-ui/Tab
    (clj->js opts)
    contents))

(defn raised-button [{:as opts}]
  (React/createElement
    material-ui/RaisedButton
    (clj->js opts)))
