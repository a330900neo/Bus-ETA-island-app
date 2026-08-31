with open('app/src/main/java/com/example/ui/screens/RouteSearchScreen.kt', 'r') as f:
    lines = f.readlines()

new_lines = []
skip = False
for i, line in enumerate(lines):
    if line.strip() == "color = BusLightGray,":
        new_lines.append(line.replace("BusLightGray", "Color.Gray"))
        continue
    if "padding(horizontal = 14.dp, vertical = 10.dp)" in line and i == 482:
        skip = True
        continue
    if skip and "}" in line and i == 491:
        skip = False
        continue
    if not skip:
        new_lines.append(line)

with open('app/src/main/java/com/example/ui/screens/RouteSearchScreen.kt', 'w') as f:
    f.writelines(new_lines)
