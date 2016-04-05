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
Save tracking paths for later (use local storage)
- Tracking paths option in main menu
- Ask to save if not upload
- Delete saved tracking path on upload

Fetch waypoint paths
Use waypoint paths with tracking
Add bluetooth

Add tests (mock side effects for now) start testing side effects
Have handlers return a list of effects instead of performing them
Find a way to test views without having to restart on figwheel load
"Polish" figwheel-bridge
Upload old version (sigsub) of gps-tracker to heroku
Find better way to filter gps position updates (core.async?, rx?)
Sign apk for release deployment
