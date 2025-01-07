import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

class FineGrainedList {
    private final Node head;
    private final Node tail;
    private final Set<Integer> blacklist;
    private final ReentrantLock blacklistLock;

    public FineGrainedList() {
        // Create sentinel nodes
        this.head = new Node(); // Dummy head
        this.tail = new Node(); // Dummy tail
        head.next = tail;
        this.blacklist = Collections.synchronizedSet(new HashSet<>());
        this.blacklistLock = new ReentrantLock();
    }

    public void insert(int id, int score, int country) {
        // Check blacklist first
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

        // Find existing contestant or insertion point
        Node pred = head;
        pred.lock();
        Node curr = pred.next;
        curr.lock();

        try {
            // Search for the contestant or the insertion point
            boolean found = false;
            while (curr != tail) {
                if (curr.data != null && curr.data.getId() == id) {
                    // Update existing contestant
                    curr.data.addScore(score);
                    found = true;
                    break;
                }
                // Move to next nodes
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
            curr.unlock();
            pred.unlock();
        }
    }

    private void removeContestant(int id) {
        Node pred = head;
        pred.lock();
        Node curr = pred.next;
        curr.lock();

        try {
            while (curr != tail) {
                if (curr.data != null && curr.data.getId() == id) {
                    // Remove the node
                    pred.next = curr.next;
                    return;
                }
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

    // Method to sort the list at the end
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

        // Rebuild the list
        head.lock();
        try {
            // Clear the list except sentinels
            Node current = head;
            current.next = tail;

            // Rebuild with sorted entries
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