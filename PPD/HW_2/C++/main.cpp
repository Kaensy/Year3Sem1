#include "DynamicMatrices.h"

int main() {

    try {
        DynamicMatrices convolution("../N10000M10000k3.txt");
        convolution.run(4);
        return 0;
    } catch (const std::exception& e) {
        std::cerr << "Error: " << e.what() << std::endl;
        return 1;
    }
}