import re

with open('app/src/main/java/com/example/ui/viewmodel/BusViewModel.kt', 'r') as f:
    content = f.read()

content = content.replace("loadStopsForRoute(routeData.route, routeData.bound, routeData.serviceType)", "loadStopsForRoute(routeData.co, routeData.route, routeData.bound, routeData.serviceType)")
content = content.replace("loadStopsForRoute(route.route, bound, route.serviceType)", "loadStopsForRoute(route.co, route.route, bound, route.serviceType)")
content = content.replace("fun loadStopsForRoute(route: String", "fun loadStopsForRoute(co: String, route: String")
content = content.replace("val stops = repository.getStopsForRoute(route", "val stops = repository.getStopsForRoute(co, route")
content = content.replace("fetchEtaPreviewForStop(stopData.stop, route", "fetchEtaPreviewForStop(co, stopData.stop, route")

content = content.replace("fun fetchEtaPreviewForStop(stopId: String", "fun fetchEtaPreviewForStop(co: String, stopId: String")
content = content.replace("val etas = repository.getEta(stopId", "val etas = repository.getEta(co, stopId")

content = content.replace("fun trackStopOnIsland(\n        route: String,", "fun trackStopOnIsland(\n        co: String,\n        route: String,")
content = content.replace("repository.setActiveTrackedStop(\n                route = route,", "repository.setActiveTrackedStop(\n                co = co,\n                route = route,")

content = content.replace("repository.setActiveTrackedStop(\n                route = pinned.route,", "repository.setActiveTrackedStop(\n                co = pinned.co,\n                route = pinned.route,")


with open('app/src/main/java/com/example/ui/viewmodel/BusViewModel.kt', 'w') as f:
    f.write(content)
