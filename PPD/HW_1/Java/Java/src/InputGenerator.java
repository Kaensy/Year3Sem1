import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

public class InputGenerator {
    public static void generateInputFile(int N, int M, int k, int p, int min, int max) {
        Random random = new Random();
        String filename = String.format("N%dM%dk%d.txt", N, M, k);

        try (FileWriter writer = new FileWriter(filename)) {
            // Write dimensions and parameters
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
                    writer.write(String.format("%d ", random.nextInt(2))); // Typically convolution matrices use 0s and 1s
                }
                writer.write("\n");
            }

        } catch (IOException e) {
            System.err.println("Error generating input file: " + e.getMessage());
            e.printStackTrace();
        }
    }
}