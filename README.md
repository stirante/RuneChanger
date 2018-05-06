# RuneChanger

[![Video](https://img.youtube.com/vi/5Gdlu3nIlAw/0.jpg)](https://www.youtube.com/watch?v=5Gdlu3nIlAw)

## Requirements
#### Running
JRE 8 or later
#### Building
JDK 8 or later

Maven

lol-client-java-api

## Building

Using Git Bash
```
git clone https://github.com/stirante/RuneChanger.git
cd RuneChanger
mvn package
```

## Running
Just run jar or start it using ``java -jar RuneChanger.jar`` for extra logging

## How it works
RuneChanger uses internal LoL client API. If it finds active session it gets rune pages for currently selected champion and displays option to set those runes to active rune page.

The button is actually completely separate window which tracks client position and size to be exactly where it should be.