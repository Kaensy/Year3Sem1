import java.io.*;
import java.util.*;

public class TestDataGenerator {
    private static final int NUM_COUNTRIES = 5;
    private static final int NUM_PROBLEMS = 10;
    private static final int MIN_CONTESTANTS = 80;
    private static final int MAX_CONTESTANTS = 100;
    private static final double UNSOLVED_PROBABILITY = 0.10; // 10%
    private static final double FRAUD_PROBABILITY = 0.02;    // 2%
    private static final int MIN_SCORE = 10;  // Minimum valid score
    private static final int MAX_SCORE = 100; // Maximum valid score

    private static final Random random = new Random();

    public static void main(String[] args) {
        // Create directory for results if it doesn't exist
        new File("contest_data").mkdirs();

        // Generate contestant counts for each country
        int[] contestantsPerCountry = new int[NUM_COUNTRIES];
        for (int i = 0; i < NUM_COUNTRIES; i++) {
            contestantsPerCountry[i] = random.nextInt(MAX_CONTESTANTS - MIN_CONTESTANTS + 1) + MIN_CONTESTANTS;
        }

        // Generate results for each country and problem
        for (int country = 1; country <= NUM_COUNTRIES; country++) {
            for (int problem = 1; problem <= NUM_PROBLEMS; problem++) {
                generateResultFile(country, problem, contestantsPerCountry[country - 1]);
            }
        }

        // Print summary of generated data
        System.out.println("Test data generation completed!");
        System.out.println("Generated files are in the 'contest_data' directory");
        for (int i = 0; i < NUM_COUNTRIES; i++) {
            System.out.printf("Country %d: %d contestants%n", i + 1, contestantsPerCountry[i]);
        }
    }

    private static void generateResultFile(int country, int problem, int numContestants) {
        String filename = String.format("contest_data/RezultateC%d_P%d.txt", country, problem);

        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            // For each contestant
            for (int id = (country - 1) * 1000 + 1; id < (country - 1) * 1000 + numContestants + 1; id++) {
                // Determine if contestant solves this problem
                if (random.nextDouble() < UNSOLVED_PROBABILITY) {
                    continue; // Skip - problem not solved
                }

                // Determine if this is a fraud attempt
                if (random.nextDouble() < FRAUD_PROBABILITY) {
                    writer.printf("%d -1%n", id); // Fraud case
                    continue;
                }

                // Generate random valid score
                int score = random.nextInt(MAX_SCORE - MIN_SCORE + 1) + MIN_SCORE;
                writer.printf("%d %d%n", id, score);
            }
        } catch (IOException e) {
            System.err.println("Error generating file " + filename + ": " + e.getMessage());
        }
    }
}