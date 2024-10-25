public class HorizontalAllocatedThreads {
    private final int N, M, k, p;
    private final int[][] matrix;
    private final int[][] convolutionMatrix;
    private final int[][] resultMatrix;
    private final Thread[] threads;

    public HorizontalAllocatedThreads(int N, int M, int k, int p, int[][] matrix, int[][] convolutionMatrix) {
        this.N = N;
        this.M = M;
        this.k = k;
        this.p = Math.min(p, N); // Ensure we don't create more threads than rows
        this.matrix = matrix;
        this.convolutionMatrix = convolutionMatrix;
        this.resultMatrix = new int[N][M];
        this.threads = new Thread[this.p];
    }

    public int[][] run() throws InterruptedException {
        // Calculate rows per thread and remaining rows
        int rowsPerThread = N / p;
        int remainingRows = N % p;
        int startRow = 0;

        // Create and start threads
        for (int i = 0; i < p; i++) {
            int threadRows = rowsPerThread + (remainingRows > 0 ? 1 : 0);
            int endRow = startRow + threadRows;

            threads[i] = new Thread(new HorizontalWorker(startRow, endRow));
            threads[i].start();

            startRow = endRow;
            remainingRows--;
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }

        return resultMatrix;
    }

    private class HorizontalWorker implements Runnable {
        private final int startRow;
        private final int endRow;

        public HorizontalWorker(int startRow, int endRow) {
            this.startRow = startRow;
            this.endRow = endRow;
        }

        @Override
        public void run() {
            int padding = k / 2;

            for (int i = startRow; i < endRow; i++) {
                for (int j = 0; j < M; j++) {
                    int sum = 0;
                    // Apply convolution
                    for (int ki = 0; ki < k; ki++) {
                        for (int kj = 0; kj < k; kj++) {
                            sum += matrix[i + ki][j + kj] * convolutionMatrix[ki][kj];
                        }
                    }
                    resultMatrix[i][j] = sum;
                }
            }
        }
    }
}