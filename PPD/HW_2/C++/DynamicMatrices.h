#ifndef DYNAMIC_MATRICES_H
#define DYNAMIC_MATRICES_H

#include <iostream>
#include <fstream>
#include <chrono>
#include <thread>
#include <vector>
#include <string>

class DynamicMatrices {
private:
    int N, M;
    static const int k = 3;
    std::string inputFileName;
    int** matrix;
    int** kernel;


    void allocateMatrices();
    void deallocateMatrices();
    void readMatrices();
    void processRowRange(int startRow, int endRow);
    void computeSequential();
    void computeParallel(int numThreads);
    void writeResult(const std::string& filename);
    bool compareOutputFiles(const std::string& file1, const std::string& file2);

public:
    DynamicMatrices(const std::string& inputFile);
    ~DynamicMatrices();
    void run(int numThreads);
    int seq_time, par_time;
};

#endif