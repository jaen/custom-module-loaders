# Clojurescript modular library conversion demo

## How to run this

You need to have Java (obviously) ant, maven and npm installed locally for this to run (as well as wget and git but if you checked out this repo there's a quite high chance you already have them anyway). On this branch you also need lein to create reagent jar.

If you have, then fire up your shell, type
```
./build.sh
```
and wait for the script to compile GClosure & Clojurescript, get the deps and fire up the watch mode.

At that point you should be able to open up `index.html` in your browser and see a simple demo using raw React and [material-ui](http://material-ui.com/) just as they are packaged in npm (module few `cp`s and `mv`s to work around things I haven't addressed yet). Feel free to play around with the source in `src/cljs/custom-module-loders` adding your own components to the page and whatnot.
You might even try adding some more deps to `package.json` and if you're lucky the might just actually work.

I advise against trying to understand what `src/clj/process_libs.clj` does or changes done to GClosure, since it's all just an ugly hack to get this proof-of-concept working. As examples of that ugliness sticking out there's the `:depth` key which basically says "if I see node_modules one more time in the path I'm gonna flip the table", having to list all transitive dependencies in the `:depends` key or having to rename `react.js` in root of the library to `index.js` because otherwise it can't be found.
If all kinks get ironed out then this might as well become part boot task<sup>1</sup>, part Clojurescript feature.  
Until then, that's about it.

## Remarks

You might be interested to know how to use `require` vs `import` - basically use `require` if the CommonJS module exports a JS object (which is most of the time); if the export is a function you have to resort to `import` on things like `injectTapEventPlugin` or `ThemeManager` (which incidentally also gets mangled to `theme-manager`, another kink), as evidenced by the `custom-module-loaders.core` namespace.

<sup>1</sup> - totally named Garrett, because it will let you steal all the best libs from the frontend ecosystem just as they are.
