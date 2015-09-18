# Clojurescript modular library conversion demo

## How to run this

You need to have:

  * Java (obviously),
  * ant,
  * maven,
  * npm,
  * leiningen (to build reagent jar),
  * wget, git, bash (to run the build script),
  * python2 (optionally, for the `SimpleHTTPServer`).

If you have all the dependencies installed, then fire up your shell, type
```
./build.sh
```
and wait for the script to compile GClosure & Clojurescript, get the deps and
fire up the watch mode for you to experiment a bit. It's probably best to view
`index.html` with `python -m SimpleHTTPServer8000` (make that `python2` if you
use Arch, like me) as React devtools don't seem to work with `file://` protocol.

This branch demonstrates:

  * proof-of-concept integration of CommonJS-based library processing with
    the Clojurescript compiler proper. Doesn't have all the options of the
    previous standalone hack and is not yet patch-quality, but it works at least
    for the simple case of using just React,
  * using modified reagent with raw React; it totally works (at least for the
    simple test case that is).
