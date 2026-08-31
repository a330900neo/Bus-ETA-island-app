import re

with open('app/src/main/java/com/example/ui/screens/RouteSearchScreen.kt', 'r') as f:
    content = f.read()

content = content.replace("viewModel.fetchEtaPreviewForStop(stopData.stop", "viewModel.fetchEtaPreviewForStop(currentRoute.co, stopData.stop")
content = content.replace("viewModel.trackStopOnIsland(\n                                    route = currentRoute.route", "viewModel.trackStopOnIsland(\n                                    co = currentRoute.co,\n                                    route = currentRoute.route")

with open('app/src/main/java/com/example/ui/screens/RouteSearchScreen.kt', 'w') as f:
    f.write(content)

