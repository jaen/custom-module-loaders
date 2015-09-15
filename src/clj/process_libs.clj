(ns process-libs
  (:require [clojure.reflect :as r]
            [clojure.pprint :as pp]
            [cljs.js-deps :as deps]
            [cljs.closure :as cls]
            [clojure.java.io :as io]
            [clojure.string :as string])
  (:import [java.net URI URL]
           [com.google.javascript.jscomp ES6ModuleLoader SourceFile CompilerInput Result JSError ProcessCommonJSModules IJavascriptModuleLoader]
           [com.google.javascript.rhino Node]
           [javax.script ScriptEngineManager]))

(defn lib-name-from-module-root [path]
  (last (string/split path #"/")))

(defn only-js-files-filter [path]
  (.endsWith path ".js"))

(defn make-path [[& path-elements]]
  (string/join "/" (filter some? path-elements)))

(defn file-path [file]
  (.toString file))

(defn absolute-path [root]
  (str (.getAbsolutePath (io/file root))))

(defn relativise [root path]
  (let [root (URI. (str "file:" (absolute-path root)))
        path (URI. (str "file:" (absolute-path path)))]
    (.toString (.relativize root path))))

(defn relative-path? [path]
  (if [path (.toString path)]
    (or (.startsWith path "../")
        (.startsWith path "./"))))

; assumes one extension, usually true
(defn strip-ext [path]
  (let [last-index (.lastIndexOf path ".")
        is-file? true #_(.isFile (io/file path))] ;; TODO: fix this; can't break on things like lodash.something
    (if (and (> last-index 0)
             is-file?)
      (subs path 0 last-index)
      path)))

(defn normalised-path [path]
  path)

(defn path-to-module-name [path]
  (-> path
    (strip-ext)
    (string/replace #"-|\." "_")
    (string/replace #"\\|/" ".")))

; (defn module-name-to-path [module-name ext]
;   (-> path
;     (string/replace #"\." "/")
;     (str "." ext)))

(defn get-file-list'
  ([files]
    (get-file-list' files {}))
  ([files {:keys [root] :as opts}]
    (let [filename-filter (or (:filename-filter opts)
                              only-js-files-filter)]
      ; (println "FILENAME-FILTER=" filename-filter)
      (mapcat (fn [rel-path]
                (let [path (make-path [root rel-path])
                      relevant-file (fn [file]
                                      (and (not (.isDirectory file))
                                           (filename-filter (.getPath file))))
                      lookup-file (io/file path)
                      files (when (.exists lookup-file)
                              (filter relevant-file (file-seq lookup-file)))]
                  (map (fn [file]
                         (deps/load-foreign-library {:file (file-path file)}))
                    files)))
              files))))

(def get-file-list (memoize get-file-list'))

; (defn make-loader-lookup-map
;   ([lib-specs]
;     (make-loader-lookup-map root lib-specs {}))
;   ([root lib-specs {:keys [main lib-name] :or {lib-name (lib-name-from-root root)}}]
;     (into {}
;           (concat
;                   (when main
;                     [lib-name main])))))

(def test-lib-spec
  {:root "node_modules/react"
 :name "React"
 :files ["react.js" "addons.js" "lib/"]
 :main "react.js"
 :module-type :commonjs
 :module-mapping {"lib/cx.js" "classSet"}
 :module-mapping-fn (fn [path mapping]
                      (let [lookup (get mapping path)]
                        (or lookup
                            (path-to-module-name (string/replace path #"lib/" "")))))})

(def test-lookup-map
{"react" ["react/react.js" "react/index.js"]
 "react/react" "react/react.js"
 "react/react.js" "react/react.js"
 "react/addons" "react/addons.js"
 "react/addons.js" "react/addons.js"
 "react/lib/EventPluginHub" "react/lib/EventPluginHub.js"
 "react/lib/EventPluginHub.js" "react/lib/EventPluginHub.js"})

; invert teh map
(defn invert-lookup-map [lookup-map]
  (let [get-uri (comp #(.toURI %) :url)]
    (reduce (fn [acc [k v]]
              (merge-with (fn [old new]
                            (vec (concat old new)))
                          acc {k v}))
            {}
            (mapcat (fn [[k v]]
                      (map (juxt get-uri (constantly (vec (flatten [k])))) (flatten [v])))
                    lookup-map))))

(defn module-name-to-identifier [name]
  (-> name
    (string/replace #"-" "_")
    (string/replace #"\." "\\$")))

(defn module-name-to-output-path [name ext]
  (-> name
    (string/replace #"-" "_")
    (string/replace #"\." "/")
    (str "." ext)))

(defn make-module-name [[& module-elements]]
  (string/join "." (filter some? module-elements)))

(defn lookup-map-for-lib-spec [lib-spec]
  (let [module-root (:root lib-spec)
        module-name (or
                      (:name lib-spec)
                      (lib-name-from-module-root module-root))
        module-dir-name (last (string/split module-root #"/"))
        main-file   (:main lib-spec)
        lib-file-paths (or (:files lib-spec)
                         [module-root])
        module-mapping (or (:module-mapping lib-spec)
                           {})
        module-mapping-fn (or (:module-mapping-fn lib-spec)
                              (fn [path module-mapping]
                                (get module-mapping path)))
        lib-files   (get-file-list lib-file-paths {:root module-root})]
    (into {}
          (concat
            (map (fn [lib-file]
                   (let [file-path (absolute-path (:file lib-file))
                         relative-path  (relativise module-root file-path)
                         module-mapping-result (module-mapping-fn relative-path module-mapping)
                         submodule-name (or module-mapping-result
                                            (path-to-module-name relative-path))]
                     ; (println "MODULE-MAPPING-FN=" (module-mapping-fn relative-path module-mapping) " PATH-TO-MODULE-NAME=" (path-to-module-name relative-path) " SUBMODULE-NAME=" submodule-name)
                     [(make-path [module-dir-name relative-path])
                      (assoc lib-file
                        :module-name (if module-mapping-result
                                       module-mapping-result
                                       (str module-name "." submodule-name)))]))
                 lib-files)
            (when main-file
              [[module-name (assoc (deps/load-foreign-library {:file (make-path [module-root main-file])})
                              :module-name module-name)]])))))

; on lookup react
;(or
;  (get loader-lookup-map require-name))

(defprotocol ILookupMap
  (lookup [this what])
  (inverted-lookup [this what]))

(defn make-my-js-loader [lib-spec module-roots compiler-inputs]
  (let [es6-loader (ES6ModuleLoader. module-roots compiler-inputs)
        lookup-map (lookup-map-for-lib-spec lib-spec)
        inverted-lookup-map (invert-lookup-map lookup-map)
        ]
    (reify
      IJavascriptModuleLoader
        (locateCommonJsModule [_ require-name context]
          (let [lookup-value (if (relative-path? require-name)
                               (let [context-name (.getName context)
                                     cwd (.toString (.getAbsolutePath (io/file "")))
                                     context-uri (URI. (str "file:" cwd "/" context-name))
                                     resolved (.resolve context-uri require-name)]
                                 ;(println "REQUIRE-NAME=" require-name " CONTEXT-URI=" context-uri " RESOLVED=" resolved)
                                 resolved)
                               require-name)
                lookup-value (.getPath lookup-value)]
            ; (println "  A=" (URI. (str "file:" lookup-value)) " RESULT="  (get inverted-lookup-map (URI. (str "file:" lookup-value))))
            ; (println "  B=" (URI. (str "file:" (make-path [lookup-value "index.js"]))) " RESULT="  (get inverted-lookup-map (URI. (str "file:" (make-path [lookup-value "index.js"])))))
            ; (println "  C=" (URI. (str "file:" (str lookup-value ".js"))) " RESULT=" (get inverted-lookup-map (URI. (str "file:" (str lookup-value ".js")))))
            (when-let [lookup-result (or (get inverted-lookup-map (URI. (str "file:" lookup-value)))
                                         (get inverted-lookup-map (URI. (str "file:" (make-path [lookup-value "index.js"]))))
                                         (get inverted-lookup-map (URI. (str "file:" (str lookup-value ".js")))))]
              (let [lookup-result (first lookup-result)
                    ;lookup-result (.toURI (get-in lookup-map [lookup-result :url]))]
                    ]
                ;(println "REQUIRE-NAME=" require-name " LOOKUP-RESULT=" lookup-result)
                (URI. lookup-result)))))

        (locateEs6Module [_ module-name context]
          (.locateEs6Module es6-loader module-name context))

        (normalizeInputAddress [_ input]
          (let [input-name      (.getName input)
                ; _ (println "INPUT-NAME=" input-name)
                cwd (.toString (.getAbsolutePath (io/file "")))
                input-uri (URI. (str "file:" cwd "/" input-name))
                ; _ (println "INPUT-URI=" input-uri)
                canonical-name  (first (get inverted-lookup-map input-uri))]
            ; (println "CANONICAL-NAME=" canonical-name)
            (or (URI. canonical-name)
                input)))

        (toModuleName [_ filename]
          (let [filename (.toString filename)]
            ; (println "TO MODULE NAME CALLED; FILENAME=" filename " RESULT=" (get-in lookup-map [filename :module-name]))
            (get-in lookup-map [filename :module-name])))

        (toModuleIdentifier [_ filename]
          (let [filename (.toString filename)]
            ; (println "TO MODULE IDENTIFIER CALLED; FILENAME=" filename " RESULT=" (module-name-to-identifier (get-in lookup-map [filename :module-name])))
            (module-name-to-identifier (get-in lookup-map [filename :module-name]))))

      ILookupMap
        (lookup [_ what]
          ; (pp/pprint lookup-map)
          (get lookup-map what))

        (inverted-lookup [_ what]
          (get inverted-lookup-map what)))))

; (defn make-my-js-loader [lib-spec module-roots compiler-inputs]
;   ""
;   (let [es6-loader (ES6ModuleLoader. module-roots compiler-inputs)
;         lookup-map (lookup-map-for-lib-spec lib-spec)
;         module-mapping (or (:module-mapping lib-spec)
;                            {})
;         module-mapping-fn (or (:module-mapping-fn lib-spec)
;                               (fn [path module-mapping]
;                                 (get module-mapping path)))]
;     (reify IJavascriptModuleLoader
;       (locateCommonJsModule [_ require-name context]
;         #_(.locateCommonJsModule es6-loader require-name context)
;         (or (get lookup-map require-name)
;             (get looup-map (str require-name ".js"))))
;
;       (locateEs6Module [_ module-name context]
;         (.locateEs6Module es6-loader module-name context))
;
;       (normalizeInputAddress [_ input]
;         (.normalizeInputAddress es6-loader input))
;
;       (toModuleName [_ filename]
;         ; (println "TO MODULE NAME CALLED" filename)
;         #_(let [filename-string (.toString filename)]
;           (-> filename-string
;             (string/replace #".jsx?$" "")
;             (string/replace #"-|:|\." "_")
;             (string/replace #"\\|/" ".")
;             ))
;         #_(get lookup-map filename))
;
;        (toModuleIdentifier [_ filename]
;          ; (println "TO MODULE IDENTIFIER CALLED" filename)
;          (.toModuleIdentifier es6-loader filename)))))

;;==========

(def errors (atom []))
(def warnings (atom []))

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

(defn convert-single-file' [{:keys [file module-name] :as js-lib} {:keys [lib-spec library-root library-name module-roots module-type lib-out-dir ^List source-files] :as opts}]
  (let [^List externs '()
        ^CompilerOptions options (cls/make-convert-js-module-options {:language-in :ecmascript6 :language-out :ecmascript3})
        closure-compiler (doto (cls/make-closure-compiler)
                           (.init externs source-files options))
        _ (println "\n\u001b[92mCONVERT SINGLE FILE\u001b[0m: " file)
        es6-loader (let [^List module-roots (concat module-roots ["."])
                         ^List compiler-inputs (map #(CompilerInput. %) source-files)]
                     (make-my-js-loader lib-spec module-roots compiler-inputs))
        cjs (ProcessCommonJSModules. closure-compiler es6-loader)
        ^Node root (cls/get-root-node js-lib closure-compiler)]
    (.process cjs nil root)
    (report-failure (.getResult closure-compiler))
    (let [
      ; (let [file-path (absolute-path (:file lib-file))
      ;           module-mapping-fn (:module-mapping-fn lib-spec)
      ;     submodule-name (or (module-mapping-fn relative-path module-mapping)
      ;                        (path-to-module-name relative-path))]
      ; ; (println "MODULE-MAPPING-FN=" (module-mapping-fn relative-path module-mapping) " PATH-TO-MODULE-NAME=" (path-to-module-name relative-path) " SUBMODULE-NAME=" submodule-name)
      ; (str module-name "." submodule-name))

          ; module-name
          converted-source (.toSource closure-compiler root)
          relative-path (if (.isDirectory (io/file library-root))
                          (.toString (.relativize (.toPath (io/file library-root)) (.toPath (io/file file))))
                          file)
          module-name (:module-name (.lookup es6-loader (str "react" #_library-name "/" relative-path)))
          _ (println "RELATIVE-PATH: " relative-path " LIBRARY-NAME: " library-name " MODULE-NAME: " module-name) ; " JS-LIB: " js-lib)
          extension "js" ; (subs relative-path)
          file-out-path (str lib-out-dir (when lib-out-dir "/")
                             (when library-name (name library-name)) (when library-name "/")
                             (module-name-to-output-path module-name extension))
          #_(string/replace (str lib-out-dir (when lib-out-dir "/")
                                             (when library-name (name library-name)) (when library-name "/")
                                             relative-path)
                                        "-" "_")] ;; TODO: use same normalisation
      (println "\u001b[92mOUTPUT TO:\u001b[0m " file-out-path)
      (swap! conversion-cache assoc file converted-source) ;; too simple, better to use dir on disk
      (io/make-parents file-out-path)
      (spit file-out-path converted-source)
      (assoc js-lib :source converted-source
                    :module-name module-name))))

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

(defn sources-for-library' [{:keys [root files filter] :as lib-spec}]
  (let [library-root root
        files (get-file-list files {:root library-root
                                    :filter filter})
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

(defn transform-library [{:keys [root module-type depth filter preprocess depends files] :as lib-spec} {:keys [source-files dependent-libs lib-out-dir] :as opts}]
 (println "\n\u001b[92mTRANSFORMING LIBRARY\u001b[0m: " root)
 (pp/pprint lib-spec)
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
                                         :lib-spec lib-spec
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
