| Tip matrice   | Nr Threads | Timp Executie |
|---------------|------------|---------------|
| N=M=10000 k=5 |            |               |
| Sequential    | 1          | 1996769370    |
| 2-vertical    | 2          | 1405805560    |
| 2-horizontal  | 2          | 1718824910    |
| 4-vertical    | 4          | 885302470     |
| 4-horizontal  | 4          | 849077470     |
| 8-vertical    | 8          | 571547690     |
| 8-horizontal  | 8          | 559337680     |
| 16-vertical   | 16         | 321647010     |
| 16-horizontal | 16         | 367366360     |


C++
| Tip matrice | Tip Alocare        | Nr Threads | Timp executie       |
|-------------|--------------------|------------|---------------------|
| N=M=10 k=3  |                    |            |                     |
|             | Static Sequential  | 1          | 1600 ns             |
|             | Dynamic Sequential | 1          | 2000 ns             |
|             | Static             | 4          | 137800 - vertical   |
|             |                    | 4          | 115500 - horizontal |
|             | Dynamic            | 4          | 261800 - vertical   |
|             |                    | 4          | 276500 - horizontal |
| N=M=1000 k=5 |          |    |          |
|              | Static   | 1  | 36209400 |
|              |          | 2  | 21373200 |
|              |          | 4  | 10265900 |
|              |          | 8  | 5203100  |
|              |          | 16 | 4747100  |
|              | Dynamic  | 1  | 39059200 |
|              |          | 2  | 19946200 |
|              |          | 4  | 10194500 |
|              |          | 8  | 6653600  |
|              |          | 16 | 5337800  |
| N=10, M=10000 k=5 |          |    |          |
|              | Static   | 1  | 3631000 |
|              |          | 2  | 2326200 |
|              |          | 4  | 2041300 |
|              |          | 8  | 1549900  |
|              |          | 16 | 1377800  |
|              | Dynamic  | 1  | 4826500 |
|              |          | 2  | 2247100 |
|              |          | 4  | 1479600 |
|              |          | 8  | 1399400  |
|              |          | 16 | 1195400  |
| N=10000, M=10 k=5 |          |    |          |
|              | Static   | 1  | 3857600 |
|              |          | 2  | 2116500 |
|              |          | 4  | 1601900 |
|              |          | 8  | 1233400  |
|              |          | 16 | 1149400  |
|              | Dynamic  | 1  | 3979200 |
|              |          | 2  | 2698200 |
|              |          | 4  | 1870000 |
|              |          | 8  | 1463100  |
|              |          | 16 | 1128700  |
| N=M=10000 k=5 |          |    |          |
|              | Static   | 1  | 3701633200 |
|              |          | 2  | 1820865800 |
|              |          | 4  | 952187000 |
|              |          | 8  | 487314400  |
|              |          | 16 | 362124100  |
|              | Dynamic  | 1  | 3989014600 |
|              |          | 2  | 2020710800 |
|              |          | 4  | 1068964600 |
|              |          | 8  | 611830200  |
|              |          | 16 | 411341800  |



In cazul Java:

atât pentru dimensiuni mici ale matricei (N=M=10 sau N=M=1000), cât și pentru dimensiuni mari (N=M=10000), rezolvarea secvențială este aproximativ egală cu rezolvarea utilizând thread-uri, atât prin metoda verticală, cât și metoda orizontală, indiferent de câte thread-uri folosim.

In cazul C++:

atât pentru dimensiuni mici ale matricei (N=M=10 sau N=M=1000), cât și pentru dimensiuni mari (N=M=10000), diferența dintre modelul static și cel dinamic este foarte puțin semnificativă, modelul static fiind mai eficient decât cel dinamic, iar, în ambele cazuri (static și dinamic), creșterea numărului de thread-uri face și mai eficientă rezolvarea problemei.

