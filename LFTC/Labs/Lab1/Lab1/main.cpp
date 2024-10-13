#define USE_MATH_DEFINES
#include <cmath>
#include <iostream>

using namespace std;

void CircleProperties() {
    int radius;
    double area, perimeter;

    cout << "r="; cin >> radius;

    area = M_PI * radius * radius;
    perimeter = 2 * M_PI  * radius;
    cout << "area: " << area << endl;
    cout << "perimeter: " << perimeter << endl;
}

void gcd()
{
    int a, b;

    cout << "a="; cin >> a;
    cout << "b="; cin >> b;

    while (a != b) {
        if (a > b)
            a = a - b;
        else
            b = b - a;
    }
    cout << "GCD:" << a << endl;
}

void numberSum() {
    double elems[100], sum = 0;
    int n, index;

    cout << "n="; cin >> n;

    index = 0;

    while (index < n) {
        cout << "elems[" << index << "]=";
        cin >> elems[index];
        sum = sum + elems[index];
        index = index + 1;
    }

    cout << "SUM: " << sum << endl;
}



int main()
{
    int radius;
    double area, perimeter;

    cout << "r="; cin >> radius;

    area = M_PI * radius * radius;
    perimeter = 2 * M_PI  * radius;
    cout << "area: " << area << endl;
    cout << "perimeter: " << perimeter << endl;
}