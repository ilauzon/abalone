# Abalone

An Abalone bot using adversarial game theory.

Abalone is a marble-based board game in which two players are competing to push each other’s marbles off a hexagonal grid.

## Running the game program

Requires Java 8 or newer.
```
java -jar abalone_1_0_0.jar
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
