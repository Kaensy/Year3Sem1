public class Sequential {
    private final int N, M, k;
    private final int[][] matrix;
    private final int[][] convolutionMatrix;

    public Sequential(int N, int M, int k, int[][] matrix, int[][] convolutionMatrix) {
        this.N = N;
        this.M = M;
        this.k = k;
        this.matrix = matrix;
        this.convolutionMatrix = convolutionMatrix;
    }

    public int[][] run() {
        int[][] resultMatrix = new int[N][M];
        int padding = k / 2;

        for (int i = 0; i < N; i++) {
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

        return resultMatrix;
    }
}