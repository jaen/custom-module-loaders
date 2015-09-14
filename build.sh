#!/usr/bin/env bash

echo -e "\n\033[37;1m"
echo "============================================================="
echo -e "\n\033[32;1m"
echo " (╯°□°）╯︵ ┻━┻ LET'S GET THIS PARTY STARTED (╯°□°）╯︵ ┻━┻"
echo -e "\033[0m\n\033[37;1m"
echo "============================================================="
echo -e "\033[0m\n"

BUILD_ROOT=`pwd`

if [ -d "build-log" ]; then
  rm -rf build-log
fi

mkdir -p build-log

# get patched versions of GClosure and Clojurescript

  (
    mkdir -p git-deps && cd git-deps

    echo -e "\033[37;1mChecking git dependencies...\033[0m"

      echo -e "  \033[32;1mClosure compiler\033[0m"

        if [ ! -d "closure-compiler" ]; then
          echo -n "    Cloning jaen/closure-compiler...  "
            git clone https://github.com/jaen/closure-compiler.git > /dev/null 2>&1
          echo -e "\033[32;1mDONE\033[0m"
        fi

        if [ ! -f "closure-compiler/build/compiler.jar" ]; then
          echo -n "    Building Closure compiler jar...  "
            (
              cd closure-compiler
              git checkout feature/custom-module-loaders > /dev/null 2>&1
              git pull > /dev/null 2>&1
              ant jar > $BUILD_ROOT/build-log/closure-compiler 2>&1
            )
          echo -e "\033[32;1mDONE\033[0m"
        else
          echo "    Closure compiler jar already built, nothing to do here."
        fi

      echo -e "  \033[32;1mClojurescript compiler\033[0m"

        if [ ! -d "clojurescript" ]; then
          echo -n "    Cloning jaen/clojurescript..  "
            git clone https://github.com/jaen/clojurescript.git > /dev/null 2>&1
          echo -e "    \033[32;1mDONE\033[0m"
        fi

        clojurescript_jar=(clojurescript/target/clojurescript-1.7.*-aot.jar)
        if [ ! -f "${clojurescript_jar[0]}" ]; then
          echo -n "    Building clojurescript jar...  "
            (
              cd clojurescript
              git checkout feature/custom-module-loaders > /dev/null 2>&1
              git pull > /dev/null 2>&1
              script/build > $BUILD_ROOT/build-log/clojurescript 2>&1
            )
          echo -e "   \033[32;1mDONE\033[0m"
        else
          echo "    Clojurescript jar already built, nothing to do here."
        fi
  )

# get required jars in place

  (
    mkdir -p deps && cd deps

    echo -en "\033[37;1mChecking jar dependencies...\033[0m  "

      wget -nc http://central.maven.org/maven2/org/clojure/clojure/1.7.0/clojure-1.7.0.jar -O clj.jar > /dev/null 2>&1
      wget -nc http://central.maven.org/maven2/org/clojure/google-closure-library/0.0-20150805-acd8b553/google-closure-library-0.0-20150805-acd8b553.jar -O gcl.jar > /dev/null 2>&1
      wget -nc http://central.maven.org/maven2/org/clojure/google-closure-library-third-party/0.0-20150805-acd8b553/google-closure-library-third-party-0.0-20150805-acd8b553.jar -O gcl-thirdparty.jar > /dev/null 2>&1

      if [ ! -d "deps" ]; then
        cp ../git-deps/closure-compiler/build/compiler.jar gcc.jar
      fi
      if [ ! -d "deps" ]; then
        cp ../git-deps/clojurescript/target/clojurescript-1.7.*-aot.jar cljs.jar
      fi

    echo -e "        \033[32;1mDONE\033[0m"
  )

#install node deps and fix up some things that the code doesn't deal with yet

  echo -en "\033[37;1mChecking npm dependencies...\033[0m  "
    if [ ! -d "node_modules" ]; then
      npm install > $BUILD_ROOT/build-log/npm-install 2>&1

      cp node_modules/performance-now/lib/performance-now.js node_modules/performance-now/index.js
      cp node_modules/react/react.js node_modules/react/index.js
      cp node_modules/react-tap-event-plugin/src/* node_modules/react-tap-event-plugin/
      cp node_modules/react-tap-event-plugin/src/TapEventPlugin.js node_modules/react-tap-event-plugin/index.js
      cp -R node_modules/material-ui/lib node_modules/material-ui/material-ui
      cp node_modules/material-ui/node_modules/react-draggable2/lib/draggable.js node_modules/material-ui/node_modules/react-draggable2/index.js
    fi
  echo -e "        \033[32;1mDONE\033[0m"

# set up the classpath

  CLASSPATH=$(dep_arr=(`echo deps/**`); IFS=:; echo "${dep_arr[*]}"):src/clj:src/cljs

# firing mah lazor

  java -cp $CLASSPATH clojure.main build.clj
