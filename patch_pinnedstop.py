import re

with open('app/src/main/java/com/example/ui/screens/PinnedStopsScreen.kt', 'r') as f:
    content = f.read()

pill_new = """            Row(verticalAlignment = Alignment.Top) {
                // Route Number Pill
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (isActive) BusLavenderPrimary else BusDarkBackground,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .border(1.dp, if (isActive) Color.Transparent else BusSubtleBorder, RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = pinned.route,
                            color = if (isActive) BusLavenderOnPrimary else BusTextSecondary,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = pinned.co,
                        color = BusTextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }"""
content = re.sub(r'            Row\(verticalAlignment = Alignment\.Top\) \{\n                // Route Number Pill\n                Box\(\n.*?\n                \}', pill_new, content, flags=re.DOTALL)

with open('app/src/main/java/com/example/ui/screens/PinnedStopsScreen.kt', 'w') as f:
    f.write(content)

