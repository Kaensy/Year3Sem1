import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.BrokenBarrierException;

class MatrixConvolution {
    private int[][] inputMatrix;
    private int[][] convolutionMatrix;
    private int n, m, k;

    public MatrixConvolution(String inputFile, Integer overrideThreadCount) {
        readFromFile(inputFile, overrideThreadCount);
    }

    private void readFromFile(String filename, Integer overrideThreadCount) {
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            // Read first line containing N M k p
            String[] params = br.readLine().trim().split("\\s+");
            n = Integer.parseInt(params[0]);
            m = Integer.parseInt(params[1]);
            k = Integer.parseInt(params[2]);
            // Use override thread count if provided, otherwise use file value
            if (overrideThreadCount != null) {
                System.out.println("Using command line thread count: " + overrideThreadCount);
            } else {
                overrideThreadCount = Integer.parseInt(params[3]);
                System.out.println("Using file thread count: " + overrideThreadCount);
            }

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
                    bw.write(inputMatrix[i][j] + " ");
                }
                bw.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int getExtendedValue(int row, int col) {
        // Handle border cases by extending edge values
        if (row < 0) row = 0;
        if (row >= n) row = n - 1;
        if (col < 0) col = 0;
        if (col >= m) col = m - 1;
        return inputMatrix[row][col];
    }

    private int applyConvolution(int row, int col, int[] rowCache) {
        int sum = 0;
        int offset = k / 2;

        for (int i = 0; i < k; i++) {
            for (int j = 0; j < k; j++) {
                int pixelRow = row - offset + i;
                int pixelCol = col - offset + j;

                int pixelValue;
                if (pixelRow == row) {
                    // Use cached values for current row
                    pixelValue = rowCache[Math.min(Math.max(pixelCol, 0), m - 1)];
                } else {
                    // Use matrix values for other rows
                    pixelValue = getExtendedValue(pixelRow, pixelCol);
                }

                sum += pixelValue * convolutionMatrix[i][j];
            }
        }
        return sum;
    }

    public long sequentialConvolution() {
        long startTime = System.nanoTime();
        int[] tempRow = new int[m];

        for (int i = 0; i < n; i++) {
            // Cache the current row
            System.arraycopy(inputMatrix[i], 0, tempRow, 0, m);

            // Process the row
            for (int j = 0; j < m; j++) {
                inputMatrix[i][j] = applyConvolution(i, j, tempRow);
            }
        }

        return System.nanoTime() - startTime;
    }

    public long horizontalParallelConvolution(int numThreads) throws InterruptedException {
        long startTime = System.nanoTime();
        List<Thread> threads = new ArrayList<>();
        int[] tempResults = new int[m]; // Temporary array for row results

        // Calculate rows per thread
        int chunkSize = (n + numThreads - 1) / numThreads;

        // Create barriers for synchronization
        CyclicBarrier startBarrier = new CyclicBarrier(numThreads);
        CyclicBarrier endBarrier = new CyclicBarrier(numThreads);

        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            final int startRow = i * chunkSize;
            final int endRow = Math.min(startRow + chunkSize, n);

            if (startRow >= n) break;

            Thread thread = new Thread(() -> {
                int[] threadTempRow = new int[m]; // Each thread has its own temp row

                try {
                    for (int row = startRow; row < endRow; row++) {
                        // Wait for all threads to be ready to process their rows
                        startBarrier.await();

                        // Cache the current row
                        System.arraycopy(inputMatrix[row], 0, threadTempRow, 0, m);

                        // Calculate new values for the row
                        for (int col = 0; col < m; col++) {
                            threadTempRow[col] = applyConvolution(row, col, threadTempRow);
                        }

                        // Wait for all threads to finish calculations
                        endBarrier.await();

                        // Update the row in the matrix
                        System.arraycopy(threadTempRow, 0, inputMatrix[row], 0, m);

                        // Wait for all threads to finish updating
                        startBarrier.await();
                    }
                } catch (InterruptedException | BrokenBarrierException e) {
                    e.printStackTrace();
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
        if (args.length < 1) {
            System.out.println("Usage: java Main <input_file> [num_threads]");
            return;
        }

        String inputFile = args[0];
        Integer overrideThreadCount = args.length > 1 ? Integer.parseInt(args[1]) : null;

        try {
            // Create instance with potential thread count override
            MatrixConvolution conv = new MatrixConvolution(inputFile, overrideThreadCount);

            // Run sequential version and write to output
            long seqTime = conv.sequentialConvolution();
            conv.writeToFile("output_sequential.txt");
            System.out.println("Sequential execution time: " + seqTime + " ns");

            // Create new instance for parallel version to start with fresh input
            conv = new MatrixConvolution(inputFile, overrideThreadCount);

            // Run parallel version with override thread count if provided
            long parTime = conv.horizontalParallelConvolution(overrideThreadCount != null ? overrideThreadCount : 1);
            conv.writeToFile("output_parallel.txt");
            System.out.println("Parallel execution time: " + parTime + " ns");
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}