#RuneChanger

##Requirements
####Running
JRE 8 or later
####Building
JDK 8 or later

Maven

##Building

Using Git Bash
```
git clone https://github.com/stirante/RuneChanger.git
cd RuneChanger
mvn package
```

##Running
Just run jar or start it using ``java -jar RuneChanger.jar`` for extra logging

##How it works
RuneChanger uses internal LoL client API. Every second it checks for champion selection session. 
If it finds active session it gets rune pages for currently selected champion and sets those runes to owned rune pages.

The button is actually completely separate window which tracks client position and size to be exactly where it should be.