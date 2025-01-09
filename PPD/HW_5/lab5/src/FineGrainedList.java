import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A thread-safe linked list implementation using fine-grained locking.
 * Uses sentinel nodes (dummy head and tail) and per-node locks for better concurrency.
 */
class FineGrainedList {
    // Sentinel nodes for simplified list operations
    private final Node head;  // Dummy head node
    private final Node tail;  // Dummy tail node
    // Set of blacklisted contestant IDs (for fraud cases)
    private final Set<Integer> blacklist;
    // Lock for blacklist modifications
    private final ReentrantLock blacklistLock;

    /**
     * Initializes the list with sentinel nodes and empty blacklist
     */
    public FineGrainedList() {
        this.head = new Node();  // Dummy head
        this.tail = new Node();  // Dummy tail
        head.next = tail;
        this.blacklist = Collections.synchronizedSet(new HashSet<>());
        this.blacklistLock = new ReentrantLock();
    }

    /**
     * Inserts or updates a contestant's score in the list
     * Uses hand-over-hand locking for thread safety
     */
    public void insert(int id, int score, int country) {
        // First check if contestant is blacklisted
        if (blacklist.contains(id)) {
            return;
        }

        // Handle fraud case
        if (score == -1) {
            removeContestant(id);
            blacklistLock.lock();
            try {
                blacklist.add(id);
            } finally {
                blacklistLock.unlock();
            }
            return;
        }

        // Hand-over-hand locking for traversing and modifying the list
        Node pred = head;
        pred.lock();
        Node curr = pred.next;
        curr.lock();

        try {
            boolean found = false;
            // Search for existing contestant or insertion point
            while (curr != tail) {
                if (curr.data != null && curr.data.getId() == id) {
                    // Update existing contestant's score
                    curr.data.addScore(score);
                    found = true;
                    break;
                }
                // Move forward, maintaining hand-over-hand locking
                Node next = curr.next;
                pred.unlock();
                pred = curr;
                curr = next;
                curr.lock();
            }

            if (!found) {
                // Insert new contestant
                ScoreEntry newEntry = new ScoreEntry(id, score, country);
                Node newNode = new Node(newEntry);
                newNode.next = curr;
                pred.next = newNode;
            }
        } finally {
            // Always unlock held locks
            curr.unlock();
            pred.unlock();
        }
    }

    /**
     * Removes a contestant from the list using hand-over-hand locking
     */
    private void removeContestant(int id) {
        Node pred = head;
        pred.lock();
        Node curr = pred.next;
        curr.lock();

        try {
            while (curr != tail) {
                if (curr.data != null && curr.data.getId() == id) {
                    // Remove the node by updating references
                    pred.next = curr.next;
                    return;
                }
                // Move forward with hand-over-hand locking
                Node next = curr.next;
                pred.unlock();
                pred = curr;
                curr = next;
                curr.lock();
            }
        } finally {
            curr.unlock();
            pred.unlock();
        }
    }

    /**
     * Sorts the list at the end (after all modifications are done)
     * Requires exclusive access to the entire list
     */
    public void sortList() {
        // Convert to array, sort, and rebuild list
        java.util.List<ScoreEntry> entries = new java.util.ArrayList<>();

        // Lock the entire list while collecting entries
        head.lock();
        try {
            Node current = head.next;
            while (current != tail) {
                if (current.data != null) {
                    entries.add(current.data);
                }
                current = current.next;
            }
        } finally {
            head.unlock();
        }

        // Sort entries by score (descending)
        entries.sort((a, b) -> Integer.compare(b.getScore(), a.getScore()));

        // Rebuild the sorted list
        head.lock();
        try {
            Node current = head;
            current.next = tail;

            // Insert sorted entries back into the list
            for (ScoreEntry entry : entries) {
                Node newNode = new Node(entry);
                newNode.next = current.next;
                current.next = newNode;
                current = newNode;
            }
        } finally {
            head.unlock();
        }
    }

    /**
     * Saves the current state of the list to a file
     */
    public void saveToFile(String filename) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            head.lock();
            try {
                Node current = head.next;
                int rank = 1;
                while (current != tail) {
                    if (current.data != null) {
                        writer.write(String.format("%d. %s%n", rank++, current.data.toString()));
                    }
                    current = current.next;
                }
            } finally {
                head.unlock();
            }
        } catch (IOException e) {
            System.err.println("Error writing to file: " + e.getMessage());
        }
    }
}