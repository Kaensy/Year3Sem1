import java.io.*;
import java.util.HashSet;
import java.util.Set;

// Class to represent a contestant's score entry
class ScoreEntry {
    private int id;
    private int score;

    public ScoreEntry(int id, int score) {
        this.id = id;
        this.score = score;
    }

    public int getId() { return id; }
    public int getScore() { return score; }
    public void addScore(int points) { this.score += points; }

    @Override
    public String toString() {
        return String.format("(ID: %d, Score: %d)", id, score);
    }
}

// Node class for our linked list
class Node {
    ScoreEntry data;
    Node next;

    public Node(ScoreEntry data) {
        this.data = data;
        this.next = null;
    }
}

// Ordered linked list class
class OrderedList {
    private Node head;
    private Set<Integer> blacklist;

    public OrderedList() {
        head = null;
        blacklist = new HashSet<>();
    }

    public void insert(int id, int score) {
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

        // Check if contestant already exists
        while (current != null) {
            if (current.data.getId() == id) {
                current.data.addScore(score);
                // After adding score, we need to reposition the node
                removeContestant(id);
                insert(id, current.data.getScore());
                return;
            }
            current = current.next;
        }

        // Create new entry
        ScoreEntry newEntry = new ScoreEntry(id, score);
        Node newNode = new Node(newEntry);

        // Insert in correct position (descending order)
        current = head;
        prev = null;

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

    private void removeContestant(int id) {
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

    public void saveToFile(String filename) {
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

// Main class for sequential implementation
public class ContestSequential {
    private static final int NUM_PROBLEMS = 10;
    private static final int NUM_COUNTRIES = 5;

    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();

        OrderedList contestResults = new OrderedList();

        // Process each country
        for (int country = 1; country <= NUM_COUNTRIES; country++) {
            // Process each problem
            for (int problem = 1; problem <= NUM_PROBLEMS; problem++) {
                String filename = String.format("contest_data/RezultateC%d_P%d.txt", country, problem);
                processFile(filename, contestResults);
            }
        }

        // Save final results
        contestResults.saveToFile("Clasament.txt");

        long endTime = System.currentTimeMillis();
        System.out.printf("Sequential execution time: %d ms%n", endTime - startTime);
    }

    private static void processFile(String filename, OrderedList results) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.trim().split("\\s+");
                if (parts.length == 2) {
                    int id = Integer.parseInt(parts[0]);
                    int score = Integer.parseInt(parts[1]);
                    results.insert(id, score);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading file " + filename + ": " + e.getMessage());
        }
    }
}

