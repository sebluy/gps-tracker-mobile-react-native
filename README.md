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

Switch to use elm-like architecture
Add schema
Use gps-tracker-common
