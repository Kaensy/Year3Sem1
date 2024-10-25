import java.io.*;
import java.util.ArrayList;
import java.util.List;

class MatrixConvolution {
    private int[][] inputMatrix;
    private int[][] convolutionMatrix;
    private int[][] borderedMatrix;
    private int n, m, k, p;
    private int[][] resultMatrix;

    public MatrixConvolution(String inputFile) {
        readFromFile(inputFile);
        resultMatrix = new int[n][m];
        createBorderedMatrix();
    }

    private void createBorderedMatrix() {
        int offset = k / 2;
        borderedMatrix = new int[n + 2 * offset][m + 2 * offset];

        // Copy the original matrix to the center
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                borderedMatrix[i + offset][j + offset] = inputMatrix[i][j];
            }
        }

        // Fill top and bottom borders
        for (int j = 0; j < m; j++) {
            for (int i = 0; i < offset; i++) {
                borderedMatrix[i][j + offset] = inputMatrix[0][j];                    // Top border
                borderedMatrix[n + offset + i][j + offset] = inputMatrix[n-1][j];    // Bottom border
            }
        }

        // Fill left and right borders
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < offset; j++) {
                borderedMatrix[i + offset][j] = inputMatrix[i][0];                    // Left border
                borderedMatrix[i + offset][m + offset + j] = inputMatrix[i][m-1];    // Right border
            }
        }

        // Fill corners
        for (int i = 0; i < offset; i++) {
            for (int j = 0; j < offset; j++) {
                // Top-left corner
                borderedMatrix[i][j] = inputMatrix[0][0];
                // Top-right corner
                borderedMatrix[i][m + offset + j] = inputMatrix[0][m-1];
                // Bottom-left corner
                borderedMatrix[n + offset + i][j] = inputMatrix[n-1][0];
                // Bottom-right corner
                borderedMatrix[n + offset + i][m + offset + j] = inputMatrix[n-1][m-1];
            }
        }
    }

    private void readFromFile(String filename) {
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            // Read first line containing N M k p
            String[] params = br.readLine().trim().split("\\s+");
            n = Integer.parseInt(params[0]);
            m = Integer.parseInt(params[1]);
            k = Integer.parseInt(params[2]);
            p = Integer.parseInt(params[3]);

            // Initialize matrices
            inputMatrix = new int[n][m];
            convolutionMatrix = new int[k][k];

            // Read input matrix
            for (int i = 0; i < n; i++) {
                String[] line = br.readLine().trim().split("\\s+");
                for (int j = 0; j < m; j++) {
                    inputMatrix[i][j] = Integer.parseInt(line[j]);
                }
            }

            // Read convolution matrix
            for (int i = 0; i < k; i++) {
                String[] line = br.readLine().trim().split("\\s+");
                for (int j = 0; j < k; j++) {
                    convolutionMatrix[i][j] = Integer.parseInt(line[j]);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeToFile(String filename) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filename))) {
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < m; j++) {
                    bw.write(resultMatrix[i][j] + " ");
                }
                bw.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int applyConvolution(int row, int col) {
        int sum = 0;
        int offset = k / 2;

        for (int i = 0; i < k; i++) {
            for (int j = 0; j < k; j++) {
                sum += borderedMatrix[row + i][col + j] * convolutionMatrix[i][j];
            }
        }
        return sum;
    }

    public long sequentialConvolution() {
        long startTime = System.nanoTime(); // Use nanoTime for high precision

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                resultMatrix[i][j] = applyConvolution(i, j);
            }
        }

        long duration = System.nanoTime() - startTime;
        System.out.println("Sequential:" + duration); // Print the raw nanosecond value
        return duration;
    }

    public long horizontalParallelConvolution() throws InterruptedException {
        long startTime = System.nanoTime();
        List<Thread> threads = new ArrayList<>();

        // Make sure we don't divide by zero
        if (p <= 0) {
            throw new IllegalArgumentException("Number of threads must be greater than 0");
        }

        int rowsPerThread = Math.max(1, n / p);  // Ensure at least 1 row per thread

        for (int i = 0; i < p; i++) {
            final int startRow = i * rowsPerThread;
            final int endRow = (i == p - 1) ? n : (i + 1) * rowsPerThread;

            if (startRow >= n) break;  // Don't create more threads than needed

            Thread thread = new Thread(() -> {
                for (int row = startRow; row < endRow; row++) {
                    for (int col = 0; col < m; col++) {
                        resultMatrix[row][col] = applyConvolution(row, col);
                    }
                }
            });

            threads.add(thread);
            thread.start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        return System.nanoTime() - startTime;
    }

    public long verticalParallelConvolution() throws InterruptedException {
        long startTime = System.nanoTime();
        List<Thread> threads = new ArrayList<>();
        int colsPerThread = m / p;

        for (int i = 0; i < p; i++) {
            final int startCol = i * colsPerThread;
            final int endCol = (i == p - 1) ? m : (i + 1) * colsPerThread;

            Thread thread = new Thread(() -> {
                for (int row = 0; row < n; row++) {
                    for (int col = startCol; col < endCol; col++) {
                        resultMatrix[row][col] = applyConvolution(row, col);
                    }
                }
            });

            threads.add(thread);
            thread.start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        return System.nanoTime() - startTime;
    }
}

public class Main {
    public static void main(String[] args) throws InterruptedException {
        if (args.length < 2) {
            System.out.println("Usage: java Main <input_file> <num_threads>");
            return;
        }

        String inputFile = args[0] + ".txt";
        int numThreads = Integer.parseInt(args[1]);

        try {
            MatrixConvolution conv = new MatrixConvolution(inputFile);

            // Run sequential version
            long seqTime = conv.sequentialConvolution();
            System.out.println("Sequential:" + seqTime);  // Output label with time

            // Run horizontal parallel version
            long horTime = conv.horizontalParallelConvolution();
            System.out.println("Horizontal:" + horTime);  // Output label with time

            // Run vertical parallel version
            long vertTime = conv.verticalParallelConvolution();
            System.out.println("Vertical:" + vertTime);   // Output label with time
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }
}