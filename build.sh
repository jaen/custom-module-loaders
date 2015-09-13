#!/usr/bin/env bash

# get patched versions of GClosure and Clojurescript

  if [ ! -d "git-deps" ]; then
    mkdir -p git-deps && cd git-deps

      git clone https://github.com/jaen/closure-compiler.git

      cd closure-compiler
        git checkout feature/custom-module-loaders && ant jar
      cd ..

      git clone https://github.com/jaen/clojurescript.git

      cd clojurescript
        git checkout feature/custom-module-loaders && script/build
      cd ..

    cd ..
  fi

# get required jars in place

  if [ ! -d "deps" ]; then
    mkdir -p deps && cd deps

      wget -nc http://central.maven.org/maven2/org/clojure/clojure/1.7.0/clojure-1.7.0.jar -O clj.jar
      wget -nc http://central.maven.org/maven2/org/clojure/google-closure-library/0.0-20150805-acd8b553/google-closure-library-0.0-20150805-acd8b553.jar -O gcl.jar
      wget -nc http://central.maven.org/maven2/org/clojure/google-closure-library-third-party/0.0-20150805-acd8b553/google-closure-library-third-party-0.0-20150805-acd8b553.jar -O gcl-thirdparty.jar
      cp ../git-deps/closure-compiler/build/compiler.jar gcc.jar
      cp ../git-deps/clojurescript/target/clojurescript-1.7.*-aot.jar cljs.jar

    cd ..
  fi

#install node deps and fix up some things that the code doesn't deal with yet

  if [ ! -d "node_modules" ]; then
    npm install

    cp node_modules/performance-now/lib/performance-now.js node_modules/performance-now/index.js
    cp node_modules/react/react.js node_modules/react/index.js
    cp node_modules/react-tap-event-plugin/src/* node_modules/react-tap-event-plugin/
    cp node_modules/react-tap-event-plugin/src/TapEventPlugin.js node_modules/react-tap-event-plugin/index.js
    cp -R node_modules/material-ui/lib node_modules/material-ui/material-ui
    cp node_modules/material-ui/node_modules/react-draggable2/lib/draggable.js node_modules/material-ui/node_modules/react-draggable2/index.js
  fi

# set up the classpath

  CLASSPATH=$(dep_arr=(`echo deps/**`); IFS=:; echo "${dep_arr[*]}"):src/clj:src/cljs

# get this party started

  echo "\n\n\x1b[30;1m(╯°□°）╯︵ ┻━┻ LET'S GET THIS PARTY STARTED (╯°□°）╯︵ ┻━┻\x1b[0m\n\n"

  java -cp $CLASSPATH clojure.main build.clj
