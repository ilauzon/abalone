# Abalone

An Abalone bot implemented with alpha-beta search and associated optimizations.

<img src="readme_assets/gameplay.gif">

## Running the game program

Requires Java 8 or newer.
```
java -jar abalone.jar
```

## Building from source

### Dependencies
- JRE == 21.*
- JDK >= 21

### Instructions

To create a fat JAR:
```sh
./gradlew shadowJar
```

Run the resulting JAR: 
```sh
java -jar ./build/libs/abalone-all.jar
```
