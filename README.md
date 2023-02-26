<a href="https://play.google.com/store/apps/details?id=com.LukeVideckis.minesweeper">
<img src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png" 
alt="Get it on Google Play"
height="80" />
</a>

## Overview

Relevant code found [here](https://github.com/lrvideckis/Minesweeper_Android/tree/master/app/src/main/java/com/LukeVideckis/minesweeper_android).

Aren't there like a million minesweeper games out there? - Yup

Well, then how is this any different? - I'll tell you :)

Features:

* A solver which tells you if you missed a move. Basically, the feature will tell you if you can logically figure out something in the given position. A lot of the time, I'll play minesweeper and get stuck. When that happens, I want to know if I missed anything. Is there a mine/free cell which I could have logically figured out?

* Mine probabilities for unknown cells, [explanation of algorithm](https://drive.google.com/file/d/1W96C6Cj5b8d6vBMJpuDmrdFpvkDIevMI/view). Let's say there's nothing more to be figured out, and you have to guess. The app will tell you which cell has the lowest probability of being a mine.

## Building a new release
A bash command to ask the user for passwords, then build+sign app. [source](https://stackoverflow.com/a/67274204/18306912). I don't want any passwords stored in plain text in this repo.

```
echo -n Key store password: && read -s storepw && echo && \
echo -n Key password: && read -s keypw && echo && \
./gradlew assembleRelease -Pkeystore_path='/home/twenty_one/keystore.properties.jks' -Pkeystore_pw=$storepw -Pkey_alias='key0' -Pkey_pw=$keypw
```
