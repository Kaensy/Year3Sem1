#include <iostream>
#include <chrono>
#include <thread>
#include <vector>

using namespace std;

int MAX = 100000;
int NUM_THREADS = 4;

void suma_sec(double* v1, double* v2, double* v3, int start, int end){
    for (int i = 0; i< MAX; i++){
        v3[i] = v1[i] + v2[i];
    }
}


void suma_sec2(double* v1, double* v2, double* v3, int start, int end, int step) {
    for (int i = start; i < end; i += step) {
        v3[i] = v1[i] + v2[i];
    }

}

int main() {

    double *a = new double[MAX];
    double *b = new double[MAX];
    double *c = new double[MAX];

    for (int i = 0; i < MAX; i++) {
        a[i] = i;
        b[i] = 2 * i;
    }


    auto start1 = chrono::high_resolution_clock::now();
    suma_sec(a, b, c, 0, MAX);
    auto stop1 = chrono::high_resolution_clock::now();
    auto duration1 = chrono::duration_cast<chrono::microseconds>(stop1 - start1);

    cout << "Secvential: " << duration1.count() << endl;

    int size = MAX / NUM_THREADS;
    vector<thread> threads;

    auto start2 = chrono::high_resolution_clock::now();
    for (int i = 0; i < NUM_THREADS; i++) {
        int start = i * size;
        int end = (i == NUM_THREADS - 1) ? MAX : (i + 1) * size;
        threads.emplace_back(suma_sec, a, b, c, start, end);
    }
    for (auto &t: threads) {
        t.join();
    }
    auto end2 = chrono::high_resolution_clock::now();

    vector<thread> threads2;

    auto start3 = chrono::high_resolution_clock::now();
    for (int i = 0; i < NUM_THREADS; i++) {
        int start = i * size;
        int end = (1 == NUM_THREADS - 1) ? MAX : (i + 1) * size;
        threads2.emplace_back(suma_sec2, a, b, c, start, end, MAX / NUM_THREADS);
    }
    for (auto &t: threads2) {
        t.join();
    }
    auto end3 = chrono::high_resolution_clock::now();

    auto duration2 = chrono::duration_cast<chrono::microseconds>(end2 - start2);
    auto duration3 = chrono::duration_cast<chrono::microseconds>(end3 - start3);

    cout << "Paralel: " << duration2.count() << endl;
    cout << "Ciclic: " << duration3.count() << endl;
}