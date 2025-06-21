# GSMiner

This repository implements algorithms for mining anomalous graph sequences in attributed graphs. 

## Requirements

1. **Java 8 or higher** is required to run the program.
2. Ensure that all necessary libraries and dependencies are set up in your project.

## Running the Program

### Step 1: Open the Project in IntelliJ IDEA

If you're using IntelliJ IDEA, simply open the project directory in the IDE. IntelliJ will automatically detect the `.java` files and compile them when you run the program. No need for a manual compilation step.

### Step 2: Run the `MainGSMiner.java`

To run the program, execute the `MainGSMiner.java` file. This file is the entry point for testing the subgraph mining algorithm.

In IntelliJ IDEA:
1. Right-click on the `MainGSMiner.java` file.
2. Select **Run 'MainGSMiner'** from the context menu.

Alternatively, you can use the **Run** button in the IDE toolbar after opening `MainGSMiner.java`.

  ### Step 3: Adjust the threshold

Since the method of the algorithm can be parameter-free, it is not necessary to adust the threshold. If you would like adjust the JS divergence threshold, you can open `AlgoGSMiner.java` and find

- **minJS**: the threshold for the JS divergence. The default value is 0.5.
