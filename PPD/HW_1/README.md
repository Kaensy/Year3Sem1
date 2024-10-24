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
|-------------|--------------------|------------|---------------------|
| N=M=1000 k=5  |                    |            |                     |
|             | Static Sequential  | 1          | 1600 ns             |
|             | Dynamic Sequential | 1          | 2000 ns             |
|             | Static             | 4          | 137800 - vertical   |
|             |                    | 4          | 115500 - horizontal |
|             | Dynamic            | 4          | 261800 - vertical   |
|             |                    | 4          | 276500 - horizontal |
