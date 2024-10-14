
#include <iostream>

using namespace std;


void CircleProperties() {
    int radius;
    double area, perimeter;

    cout << "r="; cin >> radius;

    area = 3.14 * radius * radius;
    perimeter = 2 * 3.14  * radius;
    cout << "area: " ;
    cout << area ;
    cout << endl;
    cout << "perimeter: " ;
    cout << perimeter ;
    cout<< endl;
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
    cout << "GCD:";
    cout << a ;
    cout << endl;
}

void numberSum() {
    double elems[100], sum = 0;
    int n, index;

    cout << "n="; cin >> n;

    index = 0;

    while (index < n) {
        cout << "elems[" ;
        cout << index ;
        cout<< "]=";
        cin >> elems[index];
        sum = sum + elems[index];
        index = index + 1;
    }

    cout << "SUM: " ;
    cout << sum ;
    cout << endl;
}



int main()
{

}