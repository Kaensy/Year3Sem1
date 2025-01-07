import java.util.*;
import java.util.concurrent.*;
import java.io.File;

public class ContestParallel {
    private static final int NUM_COUNTRIES = 5;
    private static final int NUM_PROBLEMS = 10;
    private static final int QUEUE_CAPACITY = 100;

    public static void main(String[] args) {
        // Test configurations as specified
        int[][] configurations = {
                {4, 2}, // p_r = 4, p_w = 2
                {4, 4}, // p_r = 4, p_w = 4
                {4, 12}, // p_r = 4, p_w = 12
                {2, 2}, // p_r = 2, p_w = 2
                {2, 4}, // p_r = 2, p_w = 4
                {2, 12}  // p_r = 2, p_w = 12
        };

        for (int[] config : configurations) {
            runTest(config[0], config[1]);
        }
    }

    private static void runTest(int numReaders, int numWorkers) {
        System.out.printf("%nRunning test with p_r=%d, p_w=%d%n", numReaders, numWorkers);
        long startTime = System.currentTimeMillis();

        // Create thread pool for readers
        ExecutorService readerPool = Executors.newFixedThreadPool(numReaders);

        // Initialize shared data structures
        FineGrainedList resultList = new FineGrainedList();
        BoundedBlockingQueue queue = new BoundedBlockingQueue(QUEUE_CAPACITY, NUM_COUNTRIES * NUM_PROBLEMS);

        // Create list of reading tasks
        List<Future<Integer>> readerFutures = new ArrayList<>();
        for (int country = 1; country <= NUM_COUNTRIES; country++) {
            for (int problem = 1; problem <= NUM_PROBLEMS; problem++) {
                String filename = String.format("contest_data/RezultateC%d_P%d.txt", country, problem);
                ReadingTask task = new ReadingTask(filename, queue, country);
                readerFutures.add(readerPool.submit(task));
            }
        }

        // Create and start worker threads
        List<WorkerThread> workers = new ArrayList<>();
        for (int i = 0; i < numWorkers; i++) {
            WorkerThread worker = new WorkerThread(i, queue, resultList);
            workers.add(worker);
            worker.start();
        }

        try {
            // Wait for all reading tasks to complete
            int totalEntriesRead = 0;
            for (Future<Integer> future : readerFutures) {
                totalEntriesRead += future.get();
            }
            System.out.println("Total entries read: " + totalEntriesRead);

            // Signal completion of all reading tasks
            for (int i = 0; i < NUM_COUNTRIES * NUM_PROBLEMS; i++) {
                queue.producerComplete();
            }

            // Wait for all worker threads
            for (WorkerThread worker : workers) {
                worker.join();
            }

            // Sort the final list
            resultList.sortList();

            // Save results
            String resultFileName = String.format("Clasament_pr%d_pw%d.txt", numReaders, numWorkers);
            resultList.saveToFile(resultFileName);

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            System.out.printf("Test completed in %d ms%n", duration);
            System.out.printf("Results saved to %s%n", resultFileName);

        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Error during test execution: " + e.getMessage());
        } finally {
            readerPool.shutdown();
            try {
                if (!readerPool.awaitTermination(60, TimeUnit.SECONDS)) {
                    readerPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                readerPool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    // Utility method to check if all files exist before starting
    private static boolean checkFiles() {
        for (int country = 1; country <= NUM_COUNTRIES; country++) {
            for (int problem = 1; problem <= NUM_PROBLEMS; problem++) {
                String filename = String.format("contest_data/RezultateC%d_P%d.txt", country, problem);
                if (!new File(filename).exists()) {
                    System.err.println("Missing file: " + filename);
                    return false;
                }
            }
        }
        return true;
    }
}