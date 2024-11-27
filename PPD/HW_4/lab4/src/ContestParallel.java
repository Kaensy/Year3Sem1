import java.io.*;
import java.util.*;

// Custom queue to store score entries
class ScoreQueue {
    private static class QueueNode {
        ScoreEntry data;
        QueueNode next;

        QueueNode(ScoreEntry data) {
            this.data = data;
            this.next = null;
        }
    }

    private QueueNode head;
    private QueueNode tail;
    private boolean isComplete; // Flag to indicate all producers are done
    private int producersCount; // Number of active producers

    public ScoreQueue(int numProducers) {
        this.head = null;
        this.tail = null;
        this.isComplete = false;
        this.producersCount = numProducers;
    }

    // Add an element to the queue (used by producers/readers)
    public synchronized void enqueue(ScoreEntry entry) {
        QueueNode newNode = new QueueNode(entry);

        if (tail == null) {
            head = tail = newNode;
        } else {
            tail.next = newNode;
            tail = newNode;
        }

        // Notify any waiting consumers that data is available
        notify();
    }

    // Remove and return an element from the queue (used by consumers/workers)
    public synchronized ScoreEntry dequeue() throws InterruptedException {
        // Wait while queue is empty and producers are still active
        while (head == null && !isComplete) {
            wait();
        }

        // If queue is empty and all producers are done, return null
        if (head == null && isComplete) {
            return null;
        }

        ScoreEntry entry = head.data;
        head = head.next;

        if (head == null) {
            tail = null;
        }

        return entry;
    }

    // Called by a producer when it finishes
    public synchronized void producerComplete() {
        producersCount--;
        if (producersCount == 0) {
            isComplete = true;
            // Wake up any waiting consumers
            notifyAll();
        }
    }

    // Check if all producers are done and queue is empty
    public synchronized boolean isFinished() {
        return isComplete && head == null;
    }
}

class ThreadSafeOrderedList {
    private Node head;
    private Set<Integer> blacklist;
    private final Object lock = new Object(); // Explicit lock object for synchronization

    public ThreadSafeOrderedList() {
        head = null;
        blacklist = Collections.synchronizedSet(new HashSet<>());
    }

    // Thread-safe insert operation
    public void insert(int id, int score) {
        synchronized (lock) {
            // Check if contestant is blacklisted
            if (blacklist.contains(id)) {
                return;
            }

            // Handle fraud case
            if (score == -1) {
                removeContestant(id);
                blacklist.add(id);
                return;
            }

            // Try to find existing contestant
            Node current = head;
            Node prev = null;
            boolean found = false;

            // Check if contestant already exists
            while (current != null && !found) {
                if (current.data.getId() == id) {
                    current.data.addScore(score);
                    found = true;
                    // Need to reposition the node after updating score
                    int totalScore = current.data.getScore();
                    removeContestant(id);
                    insertNewScore(id, totalScore);
                    return;
                }
                current = current.next;
            }

            if (!found) {
                insertNewScore(id, score);
            }
        }
    }

    // Helper method to insert a new score (called within synchronized block)
    private void insertNewScore(int id, int score) {
        ScoreEntry newEntry = new ScoreEntry(id, score);
        Node newNode = new Node(newEntry);

        Node current = head;
        Node prev = null;

        while (current != null && current.data.getScore() > score) {
            prev = current;
            current = current.next;
        }

        if (prev == null) {
            // Insert at head
            newNode.next = head;
            head = newNode;
        } else {
            // Insert after prev
            newNode.next = current;
            prev.next = newNode;
        }
    }

    // Thread-safe remove operation
    private void removeContestant(int id) {
        // Already synchronized by caller
        Node current = head;
        Node prev = null;

        while (current != null && current.data.getId() != id) {
            prev = current;
            current = current.next;
        }

        if (current != null) {
            if (prev == null) {
                head = current.next;
            } else {
                prev.next = current.next;
            }
        }
    }

    // Thread-safe save to file operation
    public void saveToFile(String filename) {
        synchronized (lock) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
                Node current = head;
                int rank = 1;
                while (current != null) {
                    writer.write(String.format("%d. %s%n", rank++, current.data.toString()));
                    current = current.next;
                }
            } catch (IOException e) {
                System.err.println("Error writing to file: " + e.getMessage());
            }
        }
    }

    // Method to check if a contestant is blacklisted
    public boolean isBlacklisted(int id) {
        return blacklist.contains(id);
    }

    // For debugging/testing purposes
    public synchronized void printList() {
        synchronized (lock) {
            Node current = head;
            while (current != null) {
                System.out.println(current.data);
                current = current.next;
            }
        }
    }
}

// Reader thread to read files and produce data
class ReaderThread extends Thread {
    private final ScoreQueue queue;
    private final List<String> assignedFiles;
    private final int readerId;
    private final boolean isFirstReader; // Used to identify the thread that will write final results

    public ReaderThread(int id, ScoreQueue queue, List<String> files, boolean isFirstReader) {
        this.readerId = id;
        this.queue = queue;
        this.assignedFiles = files;
        this.isFirstReader = isFirstReader;
    }

    @Override
    public void run() {
        try {
            for (String filename : assignedFiles) {
                processFile(filename);
            }
        } finally {
            // Mark this producer as complete
            queue.producerComplete();
        }
    }

    private void processFile(String filename) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.trim().split("\\s+");
                if (parts.length == 2) {
                    try {
                        int id = Integer.parseInt(parts[0]);
                        int score = Integer.parseInt(parts[1]);
                        queue.enqueue(new ScoreEntry(id, score));
                    } catch (NumberFormatException e) {
                        System.err.println("Invalid data format in file " + filename + ": " + line);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading file " + filename + ": " + e.getMessage());
        }
    }

    public boolean isFirstReader() {
        return isFirstReader;
    }

    public int getReaderId() {
        return readerId;
    }
}

// Worker thread to consume data and update the list
class WorkerThread extends Thread {
    private final ScoreQueue queue;
    private final ThreadSafeOrderedList resultList;
    private final int workerId;

    public WorkerThread(int id, ScoreQueue queue, ThreadSafeOrderedList resultList) {
        this.workerId = id;
        this.queue = queue;
        this.resultList = resultList;
    }

    @Override
    public void run() {
        try {
            while (true) {
                // Try to get a score entry from the queue
                ScoreEntry entry = queue.dequeue();

                // If we get null, it means no more data will come
                if (entry == null) {
                    break;
                }

                // Process the entry
                resultList.insert(entry.getId(), entry.getScore());
            }
        } catch (InterruptedException e) {
            System.err.println("Worker " + workerId + " interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    public int getWorkerId() {
        return workerId;
    }
}

// Utility class to distribute files among readers
class FileDistributor {
    public static List<List<String>> distributeFiles(int numReaders, int numCountries, int numProblems) {
        List<List<String>> distribution = new ArrayList<>(numReaders);
        for (int i = 0; i < numReaders; i++) {
            distribution.add(new ArrayList<>());
        }

        int fileCount = 0;
        // Distribute files in round-robin fashion
        for (int country = 1; country <= numCountries; country++) {
            for (int problem = 1; problem <= numProblems; problem++) {
                String filename = String.format("contest_data/RezultateC%d_P%d.txt", country, problem);
                int readerIndex = fileCount % numReaders;
                distribution.get(readerIndex).add(filename);
                fileCount++;
            }
        }

        return distribution;
    }
}

public class ContestParallel {
    public static final int NUM_COUNTRIES = 5;
    public static final int NUM_PROBLEMS = 10;

    public static void main(String[] args) {
        // Configuration to test: p = 4,6,8,16 and p_r = 1,2
        int[] totalThreads = {4, 6, 8, 16};
        int[] readerThreads = {1, 2};

        // Run tests for each configuration
        for (int p : totalThreads) {
            for (int p_r : readerThreads) {
                // Ensure we don't have more readers than total threads
                if (p_r < p) {
                    runTest(p, p_r);
                }
            }
        }
    }

    private static void runTest(int totalThreads, int numReaders) {
        System.out.printf("%nRunning test with p=%d, p_r=%d%n", totalThreads, numReaders);
        long startTime = System.currentTimeMillis();

        // Initialize shared data structures
        ThreadSafeOrderedList resultList = new ThreadSafeOrderedList();
        ScoreQueue queue = new ScoreQueue(numReaders);

        // Distribute files among readers
        List<List<String>> fileDistribution = FileDistributor.distributeFiles(
                numReaders, NUM_COUNTRIES, NUM_PROBLEMS);

        // Create and start reader threads
        List<ReaderThread> readers = new ArrayList<>();
        for (int i = 0; i < numReaders; i++) {
            ReaderThread reader = new ReaderThread(
                    i,
                    queue,
                    fileDistribution.get(i),
                    i == 0  // First reader will write final results
            );
            readers.add(reader);
            reader.start();
        }

        // Create and start worker threads
        int numWorkers = totalThreads - numReaders;
        List<WorkerThread> workers = new ArrayList<>();
        for (int i = 0; i < numWorkers; i++) {
            WorkerThread worker = new WorkerThread(i, queue, resultList);
            workers.add(worker);
            worker.start();
        }

        // Wait for all threads to complete
        try {
            // Wait for all reader threads
            for (ReaderThread reader : readers) {
                reader.join();
            }

            // Wait for all worker threads
            for (WorkerThread worker : workers) {
                worker.join();
            }

            // Have the first reader write the results
            String resultFileName = String.format("Clasament_p%d_pr%d.txt",
                    totalThreads, numReaders);
            resultList.saveToFile(resultFileName);

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            System.out.printf("Test completed in %d ms%n", duration);
            System.out.printf("Results saved to %s%n", resultFileName);

        } catch (InterruptedException e) {
            System.err.println("Thread joining interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }
}