# Bus ETA Island

### this is a android app for view Bus ETA but look like dynamic and can view weather also

#### (made with AI)



![app icon](https://raw.githubusercontent.com/a330900neo/Bus-ETA-island-app/refs/heads/main/app/src/main/res/drawable/bus\_island\_icon\_1787741609418.png)







## Features

- KMB CTB MTR GMB ETA api support

- Collapsible overlay even at lockscreen (dynamic island like look)

- Today highest lowest current temperature, humidity and rain probability

- Rain nowcast map from HKO 2 hour rain nowcast grid map API (1 frame means 30min

- Flight progress tracking (used python package \[pyflightdata](https://github.com/supercoderz/pyflightdata))



***



## Copyright 

You can fork, use partial code and not whole this app's code without credit.

You **cannot** publish this app's code and claim you made it all



***



## FAQ



##### Some questions you might ask



> 1. What android version supports and why iOS not supported 

>> Minimum SDK: 24 (Android 7.0) Target: 36 (Android 16)

>> iOS is not supported because developer dont have iPhone to develop on and it is not possible to achieve same feature on iOS

---

> 2. I found a bug what should i do?

>> Find a workaround or post a issue(even tho dev might lazy to fix)

---

> 3. will this app stole my data?

>> No, this app only connect to api (usually data.gov.hk), no connection to server that store user data.

---

> 4.ETA time are inaccurate

>> ETA is fetch from data.gov.hk from bus operator, not my app predict, so go ask KMB/CTB/GMB/MTR why time are jumping.

---

> 5. The app request accessibility service, what does it do? 

>> Accessibility service allows the app to display overlay even at lockscreen, without it, only after unlock can show. 

---

> 6. Why is it using lots of battery?

>> Because it display no matter what app you using, even tho with some optimization done, idk will it actually solve.

---

