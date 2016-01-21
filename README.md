To run:
re-natal use-figwheel -H <IP e.g. 10.0.0.252> (should only have to do on host change)
lein repl :headless
cider-connect and then in cider repl (figwheel-android)
react-native run-android (i think this is only necessary on java changes)
react-native start

Todo:
Rename to GPSTracker
Migrate "framework" (db, handlers)
Fork quiescent into its own project (for react native)
Add waypoint path page
Remove iOs stuff (for now)
