#include <iostream>
#include <fstream>
#include <random>
#include <string>
#include <chrono>

void generateLargeNumberFile(const std::string& filename, size_t numberOfDigits) {
    // Create a random number generator
    std::random_device rd;
    std::mt19937 gen(rd());
    std::uniform_int_distribution<> dis(0, 9);

    // Open the file
    std::ofstream file(filename);
    if (!file.is_open()) {
        throw std::runtime_error("Could not create file: " + filename);
    }

    // Write the number of digits
    file << numberOfDigits << " ";

    // Generate and write random digits
    for (size_t i = 0; i < numberOfDigits; ++i) {
        // Make sure first digit isn't 0
        if (i == 0) {
            file << (dis(gen) % 9 + 1); // generates 1-9 for first digit
        } else {
            file << dis(gen);
        }
    }

    file.close();
}

int main() {
    try {
        // Example sizes (you can modify these)
        std::vector<size_t> testSizes = {
            100,        //
            1000,       //
            100000     //
        };

        // Generate files for each size
        for (size_t size : testSizes) {
            std::string filename = "num_" + std::to_string(size) + ".txt";
            std::cout << "Generating file " << filename << " with " << size << " digits..." << std::endl;
            generateLargeNumberFile(filename, size);
        }

        std::cout << "\nFile generation completed successfully!" << std::endl;

        return 0;
    }
    catch (const std::exception& e) {
        std::cerr << "Error: " << e.what() << std::endl;
        return 1;
    }
}