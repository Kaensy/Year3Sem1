#include <iostream>
#include <fstream>
#include <vector>
#include <algorithm>
#include <string>
#include <chrono>
#include <iomanip>

class LargeNumber {
private:
    std::vector<unsigned char> digits;

public:
    // Constructor that reads from file
    explicit LargeNumber(const std::string& filename) {
        std::ifstream file(filename);
        if (!file.is_open()) {
            throw std::runtime_error("Could not open file: " + filename);
        }

        int N;
        file >> N;  // Read number of digits

        // Read the entire number as a string
        std::string number;
        file >> number;

        // Convert each character to a digit and store
        for (char c : number) {
            if (std::isdigit(c)) {
                digits.push_back(static_cast<unsigned char>(c - '0'));
            }
        }

        // Reverse the digits to have least significant digit at index 0
        std::reverse(digits.begin(), digits.end());
        file.close();
    }

    explicit LargeNumber(const std::vector<unsigned char>& digits) : digits(digits) {}

    size_t size() const {
        return digits.size();
    }

    unsigned char operator[](size_t index) const {
        return index < digits.size() ? digits[index] : 0;
    }

    void print() const {
        for (auto it = digits.rbegin(); it != digits.rend(); ++it) {
            std::cout << static_cast<int>(*it);
        }
        std::cout << std::endl;
    }

    const std::vector<unsigned char>& getDigits() const {
        return digits;
    }
};

class LargeNumberCalculator {
public:
    static LargeNumber add(const LargeNumber& num1, const LargeNumber& num2) {
        std::vector<unsigned char> result;
        size_t maxSize = std::max(num1.size(), num2.size());
        unsigned char carry = 0;

        for (size_t i = 0; i < maxSize || carry; i++) {
            unsigned char sum = carry;
            if (i < num1.size()) sum += num1[i];
            if (i < num2.size()) sum += num2[i];

            result.push_back(sum % 10);
            carry = sum / 10;
        }

        return LargeNumber(result);
    }
};

// Simplified timing formatter that only uses milliseconds
template<typename Duration>
std::string formatDuration(Duration duration) {
    auto microseconds = std::chrono::duration_cast<std::chrono::microseconds>(duration).count();
    double milliseconds = microseconds / 1000.0;
    return std::to_string(milliseconds) + " ms";
}

int main() {
    try {
        // Timing variables
        std::chrono::high_resolution_clock::time_point startRead, endRead, startAdd, endAdd;

        // Start timing file reading
        startRead = std::chrono::high_resolution_clock::now();

        // Read numbers from files
        // 1
        LargeNumber num1("../Numar1.txt");
        LargeNumber num2("../Numar2.txt");

        // // 2
        // LargeNumber num1("../num_1000.txt");
        // LargeNumber num2("../num_1000.txt");

        // // 3
        // LargeNumber num1("../num_100.txt");
        // LargeNumber num2("../num_100000.txt");

        // // 4
        // LargeNumber num1("../Numar1_4.txt");
        // LargeNumber num2("../Numar2_4.txt");


        // End timing file reading
        endRead = std::chrono::high_resolution_clock::now();

        // std::cout << "First number: ";
        // num1.print();
        // std::cout << "Second number: ";
        // num2.print();

        // Start timing addition
        startAdd = std::chrono::high_resolution_clock::now();

        // Calculate sum
        LargeNumber sum = LargeNumberCalculator::add(num1, num2);

        // End timing addition
        endAdd = std::chrono::high_resolution_clock::now();

        // Print results
        // std::cout << "Sum: ";
        // sum.print();

        // Calculate and print timing information
        auto readDuration = endRead - startRead;
        auto addDuration = endAdd - startAdd;
        auto totalDuration = readDuration + addDuration;

        std::cout << "\nTiming Results:" << std::endl;
        std::cout << "Reading time: " << formatDuration(readDuration) << std::endl;
        std::cout << "Addition time: " << formatDuration(addDuration) << std::endl;
        std::cout << "Total time: " << formatDuration(totalDuration) << std::endl;

        return 0;
    }
    catch (const std::exception& e) {
        std::cerr << "Error: " << e.what() << std::endl;
        return 1;
    }
}