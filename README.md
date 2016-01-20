To run:
re-natal use-figwheel -H <IP e.g. 10.0.0.252> (should only have to do on host change)
lein repl :headless
cider-connect and then in cider repl (figwheel-android)
react-native run-android (i think this is only necessary on java changes)
react-native start

Todo:
Fix repl (try removing quiescent, exclusions)
Use quiescent
Load waypoint paths on render
Add waypoint page path
