(ns process-libs
  (:require [clojure.reflect :as r]
            [clojure.pprint :as pp]
            [cljs.js-deps :as deps]
            [cljs.closure :as cls]
            [clojure.java.io :as io]
            [clojure.string :as string])
  (:import [java.net URI]
           [com.google.javascript.jscomp ES6ModuleLoader SourceFile CompilerInput Result JSError ProcessCommonJSModules IJavascriptModuleLoader]
           [com.google.javascript.rhino Node]
           [javax.script ScriptEngineManager]))

(defn make-my-js-loader [module-roots compiler-inputs]
  ""
  (let [es6-loader (ES6ModuleLoader. module-roots compiler-inputs)]
    (reify IJavascriptModuleLoader
      (locateCommonJsModule [_ require-name context]
        (.locateCommonJsModule es6-loader require-name context))

      (locateEs6Module [_ module-name context]
        (.locateEs6Module es6-loader module-name context))

      (normalizeInputAddress [_ input]
        (.normalizeInputAddress es6-loader input))

      (toModuleName [_ filename]
        ; (println "TO MODULE NAME CALLED" filename)
        (let [filename-string (.toString filename)]
          (-> filename-string
            (string/replace #".jsx?$" "")
            (string/replace #"-|:|\." "_")
            (string/replace #"\\|/" ".")
            )))

       (toModuleIdentifier [_ filename]
         ; (println "TO MODULE IDENTIFIER CALLED" filename)
         (.toModuleIdentifier es6-loader filename)))))

;;==========

(def errors (atom []))
(def warnings (atom []))

(defn file-path [file]
  (.toString file))

(defn get-file-list'
  ([module-root]
    (get-file-list' module-root 0 (constantly false)))
  ([module-root depth]
    (get-file-list' module-root depth (constantly false)))
  ([module-root depth filename-filter]
    (let [filename-filter (or filename-filter (constantly true))
          relevant-file (fn [file]
                          (and (not (.isDirectory file))
                               (filename-filter (.getPath file))
                               (<= (count (re-seq #"node_modules" (.getPath file))) depth) ;; HACK: do not get subdependencies
                               (or (.endsWith (.getName file) ".js")
                                   (.endsWith (.getName file) ".jsx"))))
          files (filter relevant-file (file-seq (io/file module-root)))]
      (map (fn [file]
             (deps/load-foreign-library {:file (file-path file)}))
           files))))

(def get-file-list (memoize get-file-list'))

; (cljs.js-deps/-source react)

(defn report-failure [^Result result]
  (let [closure-errors (.errors result)
        closure-warnings (.warnings result)]
    (doseq [next (seq closure-errors)]
      (let [message (str "\u001b[91m\u001b[1mERROR:\u001b[0m" (.toString ^JSError next))]
        (swap! errors conj message)
        (println message)))
    (doseq [next (seq closure-warnings)]
      (let [message (str "\u001b[93m\u001b[1mWARNING:\u001b[0m" (.toString ^JSError next))]
        (swap! warnings conj message)
        (println message)))))

(def conversion-cache (atom {}))

(defn convert-single-file' [{:keys [file] :as js-lib} {:keys [library-root library-name module-roots module-type lib-out-dir ^List source-files] :as opts}]
  (let [^List externs '()
        ^CompilerOptions options (cls/make-convert-js-module-options {:language-in :ecmascript6 :language-out :ecmascript3})
        closure-compiler (doto (cls/make-closure-compiler)
                           (.init externs source-files options))
        _ (println "\n\u001b[92mCONVERT SINGLE FILE\u001b[0m: " file)
        es6-loader (let [^List module-roots (concat module-roots ["."])
                         ^List compiler-inputs (map #(CompilerInput. %) source-files)]
                     (make-my-js-loader module-roots compiler-inputs))
        cjs (ProcessCommonJSModules. closure-compiler es6-loader)
        ^Node root (cls/get-root-node js-lib closure-compiler)]
    (.process cjs nil root)
    (report-failure (.getResult closure-compiler))
    (let [converted-source (.toSource closure-compiler root)
          relative-path (if (.isDirectory (io/file library-root))
                          (.toString (.relativize (.toPath (io/file library-root)) (.toPath (io/file file))))
                          file)
          file-out-path (string/replace (str lib-out-dir (when lib-out-dir "/")
                                             (when library-name (name library-name)) (when library-name "/")
                                             relative-path)
                                        "-" "_")]
      (println "\u001b[92mOUTPUT TO:\u001b[0m " file-out-path)
      (swap! conversion-cache assoc file converted-source) ;; too simple, better to use dir on disk
      (io/make-parents file-out-path)
      (spit file-out-path converted-source)
      (assoc js-lib :source converted-source))))

(defn convert-single-file [{:keys [file] :as js-lib} {:as opts}]
  (if-let [converted-source (get @conversion-cache file)]
    (assoc js-lib :source converted-source)
    (convert-single-file' js-lib opts)))

(def nashorn-engine
  (doto (.getEngineByName (ScriptEngineManager.) "nashorn")
        (.eval (io/reader (io/file "jstransform-simple.bundle.js")))))

(defmethod cls/js-transforms :jsx [ijs opts]
 (let [engine (doto nashorn-engine
                (.put "originalCode" (:source ijs)))]
   (assoc ijs :source
     (.eval engine (str "simple.transform(originalCode, {react: true}).code")))))

(defn match-preprocess [{:keys [file url] :as js-lib} preprocess-map]
 (second (first (filter (fn [[regex symbol]]
                           (re-find regex (.getPath url))) ;;
                         preprocess-map))))

(defn sources-for-library' [{:keys [root depth filter] :as lib-spec}]
 (let [library-root root
       files (get-file-list library-root depth filter)
       source-files (map (fn [file]
                            (assoc file :source (deps/-source file)))
                          files)]
   source-files))

(def sources-for-library (memoize sources-for-library'))

(defn one-up [path]
 (let [idx (.lastIndexOf path "/")]
   (if (> idx 0)
     (subs path 0 idx)
     path)))

(defn transform-library [{:keys [root module-type depth filter preprocess depends] :as lib-spec} {:keys [source-files dependent-libs lib-out-dir] :as opts}]
 (println "\n\u001b[92mTRANSFORMING LIBRARY\u001b[0m: " root)
 (pp/pprint (dissoc lib-spec :source))
 (let [lib-files (flatten (map sources-for-library (concat [lib-spec] dependent-libs)))
       module-roots (distinct (reverse (sort-by count (map (comp one-up :root)
                                                      (concat [lib-spec] dependent-libs)))))] ;; HACK: length is not a good sort at all
   (println "\n\u001b[92mFILE LIST\u001b[0m")
   (doall (map (fn [lib-file]
                 (let [preprocess (match-preprocess lib-file preprocess)
                       _ (when preprocess (println "\n\u001b[92mTRANSFORMING FILE WITH\u001b[0m" preprocess))
                       lib-file   (if preprocess
                                    (cls/js-transforms (assoc lib-file :preprocess preprocess) {}) ;; opts are empty for now
                                    lib-file)]
                   (when preprocess (println "\n\u001b[93mTRANSFORMED FILE WITH\u001b[0m" preprocess))
                   ; (when preprocess (println (:source lib-file)))
                   (convert-single-file lib-file
                                        {:library-name (:name lib-spec)
                                         :lib-out-dir lib-out-dir
                                         :library-root root
                                         :module-roots module-roots
                                         :module-type  module-type
                                         :source-files (map (fn [file]
                                                              (cls/js-source-file (:file file) (:source file)))
                                                            lib-files)})))
               lib-files))))

(defn process-js-libraries [{:keys [libs lib-out-dir list-warnings list-errors] :as opts}]
 (let [sources (flatten (map (fn [lib-spec]
                               (sources-for-library lib-spec))
                             libs))
       list-errors (if (nil? list-errors) true list-errors)
       list-warnings (if (nil? list-errors) false list-warnings)]
   (reset! errors [])
   (reset! warnings [])
   (reset! conversion-cache {})
   (doall (map (fn [{:keys [depends] :as lib-spec}]
                 (let [dependent-libs (when depends (filter some? (map (fn [dependent]
                                                                    (first (filter #(= (:name %) dependent) libs)))
                                                               depends)))
                       additional-module-roots (when depends (map :root dependent-libs))]
                 (transform-library lib-spec {:source-files sources
                                              :lib-out-dir lib-out-dir
                                              :dependent-libs (or dependent-libs [])})))

               libs))
   (println "\u001b[91m\u001b[1mERRORS (" (count @errors) "):\u001b[0m")
     (when list-errors
       (doseq [error @errors]
         (println "" error)))
   (println "\u001b[93m\u001b[1mWARNINGS (" (count @warnings) "):\u001b[0m")
     (when list-warnings
       (doseq [warning @warnings]
         (println "" warning)))))
