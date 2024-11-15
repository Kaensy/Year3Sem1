#include <mpi.h>
#include <iostream>
#include <fstream>
#include <vector>
#include <chrono>
#include <string>
#include <random>
#include <thread>
#include <cmath>
#ifdef _WIN32
    #include <direct.h>
    #define GetCurrentDir _getcwd
#else
    #include <unistd.h>
    #define GetCurrentDir getcwd
#endif

void generateTestFile(const std::string& filename, int size) {
    std::string basePath = "../";
    std::string fullPath = basePath + filename;
    std::ofstream file(fullPath);

    if (!file.is_open()) {
        std::cerr << "Cannot create file: " << fullPath << std::endl;
        MPI_Abort(MPI_COMM_WORLD, 1);
    }

    std::random_device rd;
    std::mt19937 gen(rd());
    std::uniform_int_distribution<> dis(0, 9);

    file << size << " ";
    file << (dis(gen) % 9 + 1);

    for (int i = 1; i < size; i++) {
        file << dis(gen);
    }
    file << std::endl;
    file.close();
}

void readLargeNumber(const std::string& filename, std::vector<unsigned char>& number, int& size) {
    std::string basePath = "../";
    std::string fullPath = basePath + filename;
    std::ifstream file(fullPath);

    if (!file.is_open()) {
        std::cerr << "Cannot open file: " << fullPath << std::endl;
        MPI_Abort(MPI_COMM_WORLD, 1);
    }

    file >> size;
    std::string numStr;
    file >> numStr;

    if (numStr.length() != size) {
        std::cerr << "Error: Number length doesn't match specified size" << std::endl;
        MPI_Abort(MPI_COMM_WORLD, 1);
    }

    number.resize(size);
    for (int i = 0; i < size; i++) {
        if (!std::isdigit(numStr[i])) {
            std::cerr << "Error: Invalid character in number" << std::endl;
            MPI_Abort(MPI_COMM_WORLD, 1);
        }
        number[size - 1 - i] = numStr[i] - '0';
    }

    file.close();
}

void writeResult(const std::string& filename, const std::vector<unsigned char>& result) {
    std::string basePath = "../";
    std::string fullPath = basePath + filename;
    std::ofstream file(fullPath);

    if (!file.is_open()) {
        std::cerr << "Cannot open file for writing: " << fullPath << std::endl;
        MPI_Abort(MPI_COMM_WORLD, 1);
    }

    file << result.size() << " ";
    for (int i = result.size() - 1; i >= 0; i--) {
        file << static_cast<int>(result[i]);
    }
    file << std::endl;
    file.close();
}

int main(int argc, char** argv) {
    MPI_Init(&argc, &argv);

    int rank, num_procs;
    MPI_Comm_rank(MPI_COMM_WORLD, &rank);
    MPI_Comm_size(MPI_COMM_WORLD, &num_procs);

    if (num_procs < 2) {
        std::cerr << "This program requires at least 2 processes!" << std::endl;
        MPI_Abort(MPI_COMM_WORLD, 1);
    }

    std::vector<unsigned char> num1, num2;
    int N1 = 0, N2 = 0;
    auto startTime = std::chrono::high_resolution_clock::now();
    auto readEndTime = startTime;

    // Step 1: Process 0 reads the numbers and pads them
    if (rank == 0) {
        // Generate or read files
        // const int numDigits = 100;
        // const int numDigits2 = 100000;
        // generateTestFile("Numar1.txt", numDigits);
        // generateTestFile("Numar2.txt", numDigits2);
        // std::this_thread::sleep_for(std::chrono::milliseconds(100));

        readLargeNumber("Numar1.txt", num1, N1);
        readLargeNumber("Numar2.txt", num2, N2);

        readEndTime = std::chrono::high_resolution_clock::now();
        auto readDuration = std::chrono::duration_cast<std::chrono::microseconds>(readEndTime - startTime);
        std::cout << "\nFile sizes: " << N1 << " and " << N2 << " digits" << std::endl;
        std::cout << "Reading time: " << readDuration.count() << " microseconds" << std::endl;

        // Calculate padded size (must be divisible by num_procs)
        int maxN = std::max(N1, N2);
        int paddedN = (maxN + num_procs - 1) / num_procs * num_procs;

        // Pad numbers with zeros
        num1.resize(paddedN, 0);
        num2.resize(paddedN, 0);
    }

    // Broadcast the padded size to all processes
    int paddedSize = 0;
    if (rank == 0) {
        paddedSize = num1.size();
    }
    MPI_Bcast(&paddedSize, 1, MPI_INT, 0, MPI_COMM_WORLD);

    // Step 2: Distribute digits using MPI_Scatter
    int chunkSize = paddedSize / num_procs;
    std::vector<unsigned char> localNum1(chunkSize), localNum2(chunkSize);

    MPI_Scatter(rank == 0 ? num1.data() : nullptr, chunkSize, MPI_UNSIGNED_CHAR,
                localNum1.data(), chunkSize, MPI_UNSIGNED_CHAR, 0, MPI_COMM_WORLD);
    MPI_Scatter(rank == 0 ? num2.data() : nullptr, chunkSize, MPI_UNSIGNED_CHAR,
                localNum2.data(), chunkSize, MPI_UNSIGNED_CHAR, 0, MPI_COMM_WORLD);

    // Step 3: Calculate local sums and carry
    std::vector<unsigned char> localSum(chunkSize);
    unsigned char carry = 0;

    // Calculate initial sum without previous carry
    for (int i = 0; i < chunkSize; i++) {
        int sum = localNum1[i] + localNum2[i];
        localSum[i] = sum % 10;
        carry = sum / 10;
    }

    // Step 4: Handle carries between processes
    if (rank < num_procs - 1) {
        MPI_Send(&carry, 1, MPI_UNSIGNED_CHAR, rank + 1, 0, MPI_COMM_WORLD);
    }

    if (rank > 0) {
        unsigned char prevCarry;
        MPI_Recv(&prevCarry, 1, MPI_UNSIGNED_CHAR, rank - 1, 0, MPI_COMM_WORLD, MPI_STATUS_IGNORE);

        // Apply received carry
        int i = 0;
        while (prevCarry > 0 && i < chunkSize) {
            int sum = localSum[i] + prevCarry;
            localSum[i] = sum % 10;
            prevCarry = sum / 10;
            i++;
        }
        carry = prevCarry;
    }

    // Step 5: Gather results
    std::vector<unsigned char> finalResult;
    if (rank == 0) {
        finalResult.resize(paddedSize);
    }

    MPI_Gather(localSum.data(), chunkSize, MPI_UNSIGNED_CHAR,
               rank == 0 ? finalResult.data() : nullptr,
               chunkSize, MPI_UNSIGNED_CHAR, 0, MPI_COMM_WORLD);

    // Step 6: Process 0 writes the result
    if (rank == 0) {
        // Remove leading zeros
        while (finalResult.size() > 1 && finalResult.back() == 0) {
            finalResult.pop_back();
        }

        auto endTime = std::chrono::high_resolution_clock::now();
        auto totalDuration = std::chrono::duration_cast<std::chrono::microseconds>(endTime - startTime);
        auto computationTime = totalDuration.count() -
            std::chrono::duration_cast<std::chrono::microseconds>(readEndTime - startTime).count();

        std::cout << "Total execution time: " << totalDuration.count() << " microseconds" << std::endl;
        std::cout << "Computation time: " << computationTime << " microseconds" << std::endl;
        double digitPerMicrosecond = static_cast<double>(paddedSize) / computationTime;
        std::cout << "Performance: " << digitPerMicrosecond << " digits/microsecond" << std::endl;

        writeResult("Numar3.txt", finalResult);
    }

    MPI_Finalize();
    return 0;
}