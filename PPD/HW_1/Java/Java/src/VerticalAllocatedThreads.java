public class VerticalAllocatedThreads {
    private final int N, M, k, p;
    private final int[][] matrix;
    private final int[][] convolutionMatrix;
    private final int[][] resultMatrix;
    private final Thread[] threads;

    public VerticalAllocatedThreads(int N, int M, int k, int p, int[][] matrix, int[][] convolutionMatrix) {
        this.N = N;
        this.M = M;
        this.k = k;
        this.p = Math.min(p, M); // Ensure we don't create more threads than columns
        this.matrix = matrix;
        this.convolutionMatrix = convolutionMatrix;
        this.resultMatrix = new int[N][M];
        this.threads = new Thread[this.p];
    }

    public int[][] run() throws InterruptedException {
        // Calculate columns per thread and remaining columns
        int colsPerThread = M / p;
        int remainingCols = M % p;
        int startCol = 0;

        // Create and start threads
        for (int i = 0; i < p; i++) {
            int threadCols = colsPerThread + (remainingCols > 0 ? 1 : 0);
            int endCol = startCol + threadCols;

            threads[i] = new Thread(new VerticalWorker(startCol, endCol));
            threads[i].start();

            startCol = endCol;
            remainingCols--;
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }

        return resultMatrix;
    }

    private class VerticalWorker implements Runnable {
        private final int startCol;
        private final int endCol;

        public VerticalWorker(int startCol, int endCol) {
            this.startCol = startCol;
            this.endCol = endCol;
        }

        @Override
        public void run() {
            int padding = k / 2;

            for (int i = 0; i < N; i++) {
                for (int j = startCol; j < endCol; j++) {
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