Run
---------------------

Change dev settings on phone to use IP:8081
lein repl :headless
cider-connect and then in cider repl (figwheel-android)
react-native start

On new ip address:
change ip in env/dev/env/main.cljs
change ip in figwheel-bridge.js

On java src changes:
react-native run-android

Todo
--------------------

Change page to {:id ... :state ...}
Save tracking paths for later

Add tests (mock side effects for now) start testing side effects
Have handlers return a list of effects instead of performing them
Use waypoint path with tracking
Add bluetooth
Find a way to test views without having to restart on figwheel load
"Polish" figwheel-bridge
Upload old version (sigsub) of gps-tracker to heroku
Find better way to filter gps position updates (core.async?, rx?)
Sign apk for release deployment
