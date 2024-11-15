#include "DynamicMatrices.h"
#include <sstream>
#include <algorithm>

DynamicMatrices::DynamicMatrices(const std::string& inputFile)
    : inputFileName(inputFile), matrix(nullptr), kernel(nullptr) {
    std::ifstream file(inputFile);
    if (!file.is_open()) {
        throw std::runtime_error("Could not open input file: " + inputFile);
    }
    file >> N >> M;
    allocateMatrices();
    readMatrices();
}

DynamicMatrices::~DynamicMatrices() {
    deallocateMatrices();
}

void DynamicMatrices::allocateMatrices() {
    matrix = new int*[N];
    for(int i = 0; i < N; i++) {
        matrix[i] = new int[M];
    }

    kernel = new int*[k];
    for(int i = 0; i < k; i++) {
        kernel[i] = new int[k];
    }
}

void DynamicMatrices::deallocateMatrices() {
    if (matrix) {
        for(int i = 0; i < N; i++) {
            delete[] matrix[i];
        }
        delete[] matrix;
        matrix = nullptr;
    }

    if (kernel) {
        for(int i = 0; i < k; i++) {
            delete[] kernel[i];
        }
        delete[] kernel;
        kernel = nullptr;
    }
}

void DynamicMatrices::readMatrices() {
    std::ifstream file(inputFileName);
    int dummy;
    file >> dummy >> dummy;

    for(int i = 0; i < N; i++) {
        for(int j = 0; j < M; j++) {
            file >> matrix[i][j];
        }
    }

    for(int i = 0; i < k; i++) {
        for(int j = 0; j < k; j++) {
            file >> kernel[i][j];
        }
    }
}

void DynamicMatrices::processRowRange(int startRow, int endRow, std::barrier<>& startBarrier, std::barrier<>& endBarrier) {
    std::vector<int> cacheRow(M);
    std::vector<int> resultRow(M);

    for (int i = startRow; i < endRow; i++) {
        // Wait for all threads to be ready
        startBarrier.arrive_and_wait();

        // Cache current row
        for (int j = 0; j < M; j++) {
            cacheRow[j] = matrix[i][j];
        }

        // Process each element
        for (int j = 0; j < M; j++) {
            int sum = 0;
            for (int ki = -1; ki <= 1; ki++) {
                for (int kj = -1; kj <= 1; kj++) {
                    int row = i + ki;
                    int col = j + kj;

                    // Boundary checks with mirroring
                    if (row < 0) row = 0;
                    if (row >= N) row = N - 1;
                    if (col < 0) col = 0;
                    if (col >= M) col = M - 1;

                    // Use cached value for current row, direct access for others
                    int val = (row == i) ? cacheRow[col] : matrix[row][col];
                    sum += val * kernel[ki + 1][kj + 1];
                }
            }
            resultRow[j] = sum;
        }

        // Wait for all threads to finish calculations
        endBarrier.arrive_and_wait();

        // Update the matrix with computed results
        for (int j = 0; j < M; j++) {
            matrix[i][j] = resultRow[j];
        }

        // Wait for all threads to finish updating
        startBarrier.arrive_and_wait();
    }
}

void DynamicMatrices::computeParallel(int numThreads) {
    // Calculate work distribution
    int rowsPerThread = N / numThreads;
    int remainingRows = N % numThreads;
    int startRow = 0;

    // Create barriers for synchronization
    std::barrier startBarrier(numThreads);
    std::barrier endBarrier(numThreads);

    std::vector<std::thread> threads;
    threads.reserve(numThreads);

    for (int i = 0; i < numThreads; i++) {
        int threadRows = rowsPerThread + (i < remainingRows ? 1 : 0);
        if (threadRows > 0) {
            threads.emplace_back(&DynamicMatrices::processRowRange,
                                 this,
                                 startRow,
                                 startRow + threadRows,
                                 std::ref(startBarrier),
                                 std::ref(endBarrier));
        }
        startRow += threadRows;
    }

    for (auto& thread : threads) {
        thread.join();
    }
}

void DynamicMatrices::computeSequential() {
    processRowRange(0, N);
}

bool DynamicMatrices::compareOutputFiles(const std::string& file1, const std::string& file2) {
    std::ifstream f1(file1), f2(file2);
    if (!f1.is_open() || !f2.is_open()) {
        std::cerr << "Could not open files for comparison" << std::endl;
        return false;
    }

    std::string line1, line2;
    int lineNum = 0;
    bool filesEqual = true;

    while (std::getline(f1, line1) && std::getline(f2, line2)) {
        lineNum++;
        if (line1 != line2) {
            std::cout << "Difference at line " << lineNum << ":\n";

            std::istringstream iss1(line1), iss2(line2);
            int val1, val2, colNum = 0;

            while (iss1 >> val1 && iss2 >> val2) {
                colNum++;
                if (val1 != val2) {
                    std::cout << "Column " << colNum << ": "
                             << val1 << " != " << val2
                             << " (diff: " << val1 - val2 << ")\n";
                }
            }
            filesEqual = false;
            break;
        }
    }

    return filesEqual;
}

void DynamicMatrices::writeResult(const std::string& filename) {
    std::ofstream file(filename);
    if (!file.is_open()) {
        throw std::runtime_error("Could not open output file: " + filename);
    }

    for (int i = 0; i < N; i++) {
        for (int j = 0; j < M; j++) {
            file << matrix[i][j];
            if (j < M - 1) file << " ";
        }
        file << "\n";
    }
}

void DynamicMatrices::run(int numThreads) {
    try {
        std::cout << "Sequential execution...\n";
        auto startSeq = std::chrono::high_resolution_clock::now();
        computeSequential();
        auto endSeq = std::chrono::high_resolution_clock::now();
        auto seqDuration = std::chrono::duration_cast<std::chrono::milliseconds>(endSeq - startSeq);
        writeResult("output_sequential.txt");
        std::cout << "Sequential execution time: " << seqDuration.count() << "ms\n";
        seq_time = seqDuration.count();

        // Reset matrix to original state
        readMatrices();

        std::cout << "Parallel execution with " << numThreads << " threads...\n";
        auto startPar = std::chrono::high_resolution_clock::now();
        computeParallel(numThreads);
        auto endPar = std::chrono::high_resolution_clock::now();
        auto parDuration = std::chrono::duration_cast<std::chrono::milliseconds>(endPar - startPar);
        writeResult("output_parallel.txt");
        std::cout << "Parallel execution time: " << parDuration.count() << "ms\n";
        std::cout << "Speedup: " << (double)seqDuration.count() / parDuration.count() << "x\n";
        par_time = parDuration.count();

    } catch (const std::exception& e) {
        std::cerr << "Error during execution: " << e.what() << std::endl;
        throw;
    }
}