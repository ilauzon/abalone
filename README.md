# Abalone

An Abalone bot implemented with alpha-beta search and associated optimizations.

<img src="readme_assets/gameplay.gif">

## Running the game program

Download abalone.jar from the [Releases](https://github.com/ilauzon/abalone/releases) section. Requires Java 11 or newer.
```
java -jar abalone.jar
```

## Building from source

### Dependencies
- JDK == 21.*

### Instructions

To create a fat JAR:
```sh
./gradlew shadowJar
```

Run the resulting JAR: 
```sh
java -jar ./build/libs/abalone-all.jar
```
