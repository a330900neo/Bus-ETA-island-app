import re

with open('app/src/main/java/com/example/ui/screens/RouteSearchScreen.kt', 'r') as f:
    content = f.read()

pill_new = """            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Route Number Pill
                Box(
                    modifier = Modifier
                        .background(
                            color = BusLavenderPrimary,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = routeItem.route,
                        color = BusLavenderOnPrimary,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = routeItem.co,
                    color = BusLightGray,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }"""
content = re.sub(r'            // Route Number Pill\n            Box\(.*?            \)\n', pill_new + '\n', content, flags=re.DOTALL)

with open('app/src/main/java/com/example/ui/screens/RouteSearchScreen.kt', 'w') as f:
    f.write(content)
