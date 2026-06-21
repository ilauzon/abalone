# Abalone

An Abalone bot using adversarial game theory.

Abalone is a marble-based board game in which two players are competing to push each other’s marbles off a hexagonal grid.

## Building from source

### Dependencies
- JRE == 21.*
- JDK >= 21

### Instructions

To create a fat JAR:
```
./gradlew packageUberJarForCurrentOS
```

Run the resulting JAR in `build/compose/jars` with `java -jar <name of jar>`.
