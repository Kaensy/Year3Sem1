#include <mpi.h>
#include <iostream>
#include <fstream>
#include <vector>
#include <chrono>
#include <string>
#include <random>
#include <thread>
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

    int rank, size;
    MPI_Comm_rank(MPI_COMM_WORLD, &rank);
    MPI_Comm_size(MPI_COMM_WORLD, &size);

    if (size < 2) {
        std::cerr << "This program requires at least 2 processes!" << std::endl;
        MPI_Abort(MPI_COMM_WORLD, 1);
    }

    std::vector<unsigned char> num1, num2;
    int N1 = 0, N2 = 0;

    if (rank == 0) {
        const int numDigits = 1000; //
        // const int numDigits2 = 100000; //
        generateTestFile("Numar1.txt", numDigits);
        generateTestFile("Numar2.txt", numDigits);
        std::this_thread::sleep_for(std::chrono::milliseconds(100));
    }

    MPI_Barrier(MPI_COMM_WORLD);

    auto startTime = std::chrono::high_resolution_clock::now();
    auto readEndTime = startTime; // Initialize here for scope

    if (rank == 0) {
        readLargeNumber("Numar1.txt", num1, N1);
        readLargeNumber("Numar2.txt", num2, N2);

        readEndTime = std::chrono::high_resolution_clock::now();
        auto readDuration = std::chrono::duration_cast<std::chrono::microseconds>(readEndTime - startTime);
        std::cout << "\nFile sizes: " << N1 << " digits" << std::endl;
        std::cout << "Reading time: " << readDuration.count() << " microseconds" << std::endl;
    }

    MPI_Bcast(&N1, 1, MPI_INT, 0, MPI_COMM_WORLD);
    MPI_Bcast(&N2, 1, MPI_INT, 0, MPI_COMM_WORLD);

    int maxN = std::max(N1, N2);
    int chunkSize = maxN / (size - 1);
    int remainder = maxN % (size - 1);

    if (rank != 0) {
        int startIdx = (rank - 1) * chunkSize;
        int localChunkSize = (rank == size - 1) ? chunkSize + remainder : chunkSize;

        std::vector<unsigned char> localNum1(localChunkSize), localNum2(localChunkSize);

        MPI_Recv(localNum1.data(), localChunkSize, MPI_UNSIGNED_CHAR, 0, 0, MPI_COMM_WORLD, MPI_STATUS_IGNORE);
        MPI_Recv(localNum2.data(), localChunkSize, MPI_UNSIGNED_CHAR, 0, 1, MPI_COMM_WORLD, MPI_STATUS_IGNORE);

        unsigned char carry = 0;
        std::vector<unsigned char> localSum(localChunkSize);

        if (rank > 1) {
            MPI_Recv(&carry, 1, MPI_UNSIGNED_CHAR, rank - 1, 2, MPI_COMM_WORLD, MPI_STATUS_IGNORE);
        }

        for (int i = 0; i < localChunkSize; i++) {
            int sum = localNum1[i] + localNum2[i] + carry;
            localSum[i] = sum % 10;
            carry = sum / 10;
        }

        if (rank < size - 1) {
            MPI_Send(&carry, 1, MPI_UNSIGNED_CHAR, rank + 1, 2, MPI_COMM_WORLD);
        }

        MPI_Send(localSum.data(), localChunkSize, MPI_UNSIGNED_CHAR, 0, 3, MPI_COMM_WORLD);

        if (rank == size - 1 && carry > 0) {
            MPI_Send(&carry, 1, MPI_UNSIGNED_CHAR, 0, 4, MPI_COMM_WORLD);
        }
    } else {
        std::vector<unsigned char> result(maxN);

        for (int i = 1; i < size; i++) {
            int startIdx = (i - 1) * chunkSize;
            int localChunkSize = (i == size - 1) ? chunkSize + remainder : chunkSize;

            std::vector<unsigned char> chunk1(localChunkSize, 0);
            std::vector<unsigned char> chunk2(localChunkSize, 0);

            for (int j = 0; j < localChunkSize; j++) {
                if (startIdx + j < N1) chunk1[j] = num1[startIdx + j];
                if (startIdx + j < N2) chunk2[j] = num2[startIdx + j];
            }

            MPI_Send(chunk1.data(), localChunkSize, MPI_UNSIGNED_CHAR, i, 0, MPI_COMM_WORLD);
            MPI_Send(chunk2.data(), localChunkSize, MPI_UNSIGNED_CHAR, i, 1, MPI_COMM_WORLD);
        }

        for (int i = 1; i < size; i++) {
            int startIdx = (i - 1) * chunkSize;
            int localChunkSize = (i == size - 1) ? chunkSize + remainder : chunkSize;

            std::vector<unsigned char> localSum(localChunkSize);
            MPI_Recv(localSum.data(), localChunkSize, MPI_UNSIGNED_CHAR, i, 3, MPI_COMM_WORLD, MPI_STATUS_IGNORE);

            for (int j = 0; j < localChunkSize; j++) {
                result[startIdx + j] = localSum[j];
            }
        }

        MPI_Status status;
        int flag;
        MPI_Iprobe(size - 1, 4, MPI_COMM_WORLD, &flag, &status);
        if (flag) {
            unsigned char finalCarry;
            MPI_Recv(&finalCarry, 1, MPI_UNSIGNED_CHAR, size - 1, 4, MPI_COMM_WORLD, MPI_STATUS_IGNORE);
            if (finalCarry > 0) {
                result.push_back(finalCarry);
            }
        }

        auto endTime = std::chrono::high_resolution_clock::now();
        auto totalDuration = std::chrono::duration_cast<std::chrono::microseconds>(endTime - startTime);
        auto computationTime = totalDuration.count() -
            std::chrono::duration_cast<std::chrono::microseconds>(readEndTime - startTime).count();

        std::cout << "Total execution time: " << totalDuration.count() << " microseconds" << std::endl;
        std::cout << "Computation time: " << computationTime << " microseconds" << std::endl;
        double digitPerMicrosecond = static_cast<double>(N1) / computationTime;
        std::cout << "Performance: " << digitPerMicrosecond << " digits/microsecond" << std::endl;

        writeResult("Numar3.txt", result);
    }

    MPI_Finalize();
    return 0;
}