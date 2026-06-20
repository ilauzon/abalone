# Abalone AI

🏆 Winner of the COMP3981 Abalone Tournament 2025!

Abalone is a marble-based board game in which two players are competing to push each other’s marbles off a hexagonal grid. This project is a game-playing agent in which we implemented adversarial game theory and associated optimizations.

## Building from source

### Dependencies
- JRE == 21.*
- JDK >= 21

### Instructions
1. In the project's root directory:
```
./gradlew createDistributable
```

2. Navigate to `./composeApp/build/compose/binaries/main/app/abalone/` to see the compiled executable `abalone`.

## Running the game program
To play a game of Abalone against the AI, first compile the project, and run the resulting executable.

1. Choose the game settings you'd like to play with. Click "Apply Settings".
2. If choosing a mode where the bot moves first, click "Start" to start the game, otherwise start your turn and enjoy the game!

## Running the state space generator
The executable includes a standalone state space generator to test that the bot is working correctly.

1. From the project root, navigate to `./composeApp/build/compose/binaries/main/app/abalone/`
2. Put any testing `.input` files to be run inside that folder.
3. Run `abalone`.
4. Click the "Go to State Space Generator" button.
5. Click on the file you want to choose then click the "Enter" button.
6. The output files wil be generated in the same folder as the executable.
