import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.Callable;

class ReadingTask implements Callable<Integer> {
    private final String filename;
    private final BoundedBlockingQueue queue;
    private final int country;

    public ReadingTask(String filename, BoundedBlockingQueue queue, int country) {
        this.filename = filename;
        this.queue = queue;
        this.country = country;
    }

    @Override
    public Integer call() throws Exception {
        int entriesRead = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.trim().split("\\s+");
                if (parts.length == 2) {
                    try {
                        int id = Integer.parseInt(parts[0]);
                        int score = Integer.parseInt(parts[1]);
                        ScoreEntry entry = new ScoreEntry(id, score, country);
                        queue.put(entry);
                        entriesRead++;
                    } catch (NumberFormatException e) {
                        System.err.println("Invalid data format in file " + filename + ": " + line);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading file " + filename + ": " + e.getMessage());
        }
        return entriesRead;
    }
}