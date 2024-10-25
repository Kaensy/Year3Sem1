import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;
import java.util.Random;


public class Main {

    private static int N, M, k, p;
    private static int[][] matrix;
    private static int[][] convolutionMatrix;
    private static final String INPUT_FILE = "N1000M1000k5.txt";

    public static void main(String[] args) {

        if (args.length == 0 || args[0].equals("generate")) {
            generateTestFile();
            System.out.println("Test file generated!");
            if (args.length == 0) return;
        }

        try {
            // Get thread count from command line
            p = Integer.parseInt(args[1]);

            // Read input file
            readInputFile();

            // Create bordered matrix
            int[][] borderedMatrix = createBorderedMatrix();

            // Run sequential
            long seqTime = runSequential(borderedMatrix);

            // Run vertical
            long vertTime = runVertical(borderedMatrix);

            // Run horizontal
            long horTime = runHorizontal(borderedMatrix);

            // Print all times on one line for the script to parse
            System.out.println(String.format("%d %d %d", seqTime, vertTime, horTime));

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void readInputFile() throws FileNotFoundException {
        Scanner scanner = new Scanner(new File(INPUT_FILE));

        // Read dimensions and parameters
        N = scanner.nextInt();
        M = scanner.nextInt();
        k = scanner.nextInt();
        int fileP = scanner.nextInt(); // Read p from file but don't override command line p

        // Read main matrix
        matrix = new int[N][M];
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < M; j++) {
                matrix[i][j] = scanner.nextInt();
            }
        }

        // Read convolution matrix
        convolutionMatrix = new int[k][k];
        for (int i = 0; i < k; i++) {
            for (int j = 0; j < k; j++) {
                convolutionMatrix[i][j] = scanner.nextInt();
            }
        }

        scanner.close();
    }

    private static int[][] createBorderedMatrix() {
        int[][] borderedMatrix = new int[N + k - 1][M + k - 1];
        int padding = k / 2;

        // Copy original matrix to center
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < M; j++) {
                borderedMatrix[i + padding][j + padding] = matrix[i][j];
            }
        }

        // Fill borders
        // Top and bottom
        for (int j = padding; j < M + padding; j++) {
            for (int i = 0; i < padding; i++) {
                borderedMatrix[i][j] = matrix[0][j - padding];
                borderedMatrix[N + padding + i][j] = matrix[N-1][j - padding];
            }
        }

        // Left and right
        for (int i = padding; i < N + padding; i++) {
            for (int j = 0; j < padding; j++) {
                borderedMatrix[i][j] = matrix[i - padding][0];
                borderedMatrix[i][M + padding + j] = matrix[i - padding][M-1];
            }
        }

        // Corners
        for (int i = 0; i < padding; i++) {
            for (int j = 0; j < padding; j++) {
                // Top-left
                borderedMatrix[i][j] = matrix[0][0];
                // Top-right
                borderedMatrix[i][M + padding + j] = matrix[0][M-1];
                // Bottom-left
                borderedMatrix[N + padding + i][j] = matrix[N-1][0];
                // Bottom-right
                borderedMatrix[N + padding + i][M + padding + j] = matrix[N-1][M-1];
            }
        }

        return borderedMatrix;
    }

    private static long runSequential(int[][] borderedMatrix) {
        long startTime = System.nanoTime();
        Sequential sequential = new Sequential(N, M, k, borderedMatrix, convolutionMatrix);
        sequential.run();
        return System.nanoTime() - startTime;
    }

    private static long runVertical(int[][] borderedMatrix) throws InterruptedException {
        long startTime = System.nanoTime();
        VerticalAllocatedThreads vertical = new VerticalAllocatedThreads(N, M, k, p, borderedMatrix, convolutionMatrix);
        vertical.run();
        return System.nanoTime() - startTime;
    }

    private static long runHorizontal(int[][] borderedMatrix) throws InterruptedException {
        long startTime = System.nanoTime();
        HorizontalAllocatedThreads horizontal = new HorizontalAllocatedThreads(N, M, k, p, borderedMatrix, convolutionMatrix);
        horizontal.run();
        return System.nanoTime() - startTime;
    }

    private static void writeMatrixToFile(int[][] matrix, String filename) {
        try (FileWriter writer = new FileWriter(filename)) {
            writer.write(String.format("Matrix %dx%d with convolution %dx%d using %d threads\n", N, M, k, k, p));
            for (int[] row : matrix) {
                for (int value : row) {
                    writer.write(value + " ");
                }
                writer.write("\n");
            }
        } catch (IOException e) {
            System.err.println("Error writing matrix to file: " + e.getMessage());
        }
    }

    public static void generateInputFile(int N, int M, int k, int p, int min, int max) {
        Random random = new Random();
        try (FileWriter writer = new FileWriter(String.format("N%dM%dk%d.txt", N, M, k))) {
            // Write dimensions
            writer.write(String.format("%d %d %d %d\n", N, M, k, p));

            // Generate and write main matrix
            for (int i = 0; i < N; i++) {
                for (int j = 0; j < M; j++) {
                    writer.write(String.format("%d ", min + random.nextInt(max - min + 1)));
                }
                writer.write("\n");
            }

            // Generate and write convolution matrix
            for (int i = 0; i < k; i++) {
                for (int j = 0; j < k; j++) {
                    writer.write(String.format("%d ", random.nextInt(2)));
                }
                writer.write("\n");
            }
        } catch (IOException e) {
            System.err.println("Error generating input file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Optional: Method to generate a test file if needed
    private static void generateTestFile() {
        N = 10000;
        M = 10000;
        k = 5;
        p = 16; // Maximum number of threads we'll test
        generateInputFile(N, M, k, p, 40, 60);
    }
}