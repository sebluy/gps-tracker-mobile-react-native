Run
---------------------

Change dev settings on phone to use IP:8081
lein repl :headless
cider-connect and then in cider repl (figwheel-android)
react-native run-android (i think this is only necessary on java changes)
react-native start

On new ip address:
change ip in env/dev/env/main.cljs
change ip in figwheel-bridge.js

Todo
--------------------

Refactor state
Cleanup view
Filter gps updates (1/10 Hz) (core.async?)
Add tests (mock side effects for now) start testing side effects
Have handlers return a list of effects instead of performing them
Add bluetooth
Add a "evaluating scripts" view to figwheel-bridge
