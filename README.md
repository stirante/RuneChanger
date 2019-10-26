# RuneChanger [![Chat](https://img.shields.io/discord/593147882496851978.svg?label=&logo=discord&logoColor=ffffff&color=7389D8&labelColor=6A7EC2)](https://discord.gg/9tEsjhj)

RuneChanger is an app that improves and speeds up the use of LoL client.

[![Video](https://upload.wikimedia.org/wikipedia/commons/thumb/e/e1/YouTube_play_buttom_icon_%282013-2017%29.svg/200px-YouTube_play_buttom_icon_%282013-2017%29.svg.png)](https://www.youtube.com/watch?v=cTbkTanju8I)

#### Features
* Quickly import runes for selected champion in champion selection
* Quickly send message to champion selection room (BOT/MID/TOP) with one click of a button
* Quickly select one of your recently played champions in champion selection
* Disenchant all your champion shards with one click
* Craft all your hextech key fragments 
* Automatically accept the game queue
* Disable 'Away' status
* Save your runes, restore them in champion selection and share them with friends
* \+ a lot more to come in the future! 

## Requirements
#### Running
* Windows
#### Building
* AdoptOpenJDK 12
* Maven
* JavaFX

## Building

Using Git Bash
```
git clone https://github.com/stirante/RuneChanger.git
cd RuneChanger
mvn package
```

## Running
Just run jar or start it using ``java -jar RuneChanger.jar`` for extra logging

## Translating
Right now translation is done through [Crowdin](https://crowdin.com/project/runechanger). Feel free to translate this project into your language.

## How it works
RuneChanger uses internal LoL client API. If it finds active session it gets rune pages for currently selected champion and displays option to set those runes to active rune page.

All new elements withing client window are actually a completely separate window which tracks client position and size to be exactly where it should be.

## More information
For more information and better detailed instructions you can check out our github wiki.
https://github.com/stirante/RuneChanger/wiki
