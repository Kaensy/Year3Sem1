# Comprehensive Performance Comparison
| Test Case | Implementation | Threads | Metric | Average | Minimum | Maximum |
|-----------|---------------|---------|---------|----------|----------|----------|
| N1=N2=18 | Sequential | - | Reading Time | 0.264 ms | 0.244 ms | 0.299 ms |
| N1=N2=18 | Sequential | - | Computation Time | 0.003 ms | 0.003 ms | 0.003 ms |
| N1=N2=18 | Sequential | - | Total Time | 0.267 ms | 0.247 ms | 0.303 ms |
| N1=N2=18 | Variant 1 | 4 | Reading Time | 289.100 μs | 260.000 μs | 366.000 μs |
| N1=N2=18 | Variant 1 | 4 | Computation Time | 200.800 μs | 178.000 μs | 216.000 μs |
| N1=N2=18 | Variant 1 | 4 | Total Time | 489.900 μs | 444.000 μs | 568.000 μs |
| N1=N2=18 | Variant 1 | 8 | Reading Time | 312.300 μs | 266.000 μs | 417.000 μs |
| N1=N2=18 | Variant 1 | 8 | Computation Time | 461.000 μs | 437.000 μs | 530.000 μs |
| N1=N2=18 | Variant 1 | 8 | Total Time | 773.300 μs | 710.000 μs | 866.000 μs |
| N1=N2=18 | Variant 1 | 16 | Reading Time | 289.700 μs | 276.000 μs | 316.000 μs |
| N1=N2=18 | Variant 1 | 16 | Computation Time | 1224.100 μs | 1089.000 μs | 1340.000 μs |
| N1=N2=18 | Variant 1 | 16 | Total Time | 1513.800 μs | 1367.000 μs | 1636.000 μs |
| N1=N2=18 | Variant 2 | 4 | Reading Time | 280.700 μs | 258.000 μs | 337.000 μs |
| N1=N2=18 | Variant 2 | 4 | Computation Time | 577.700 μs | 519.000 μs | 673.000 μs |
| N1=N2=18 | Variant 2 | 4 | Total Time | 858.400 μs | 781.000 μs | 1010.000 μs |
| N1=N2=18 | Variant 2 | 8 | Reading Time | 277.300 μs | 261.000 μs | 322.000 μs |
| N1=N2=18 | Variant 2 | 8 | Computation Time | 864.700 μs | 787.000 μs | 1037.000 μs |
| N1=N2=18 | Variant 2 | 8 | Total Time | 1142.000 μs | 1056.000 μs | 1335.000 μs |
| N1=N2=18 | Variant 2 | 16 | Reading Time | 351.500 μs | 293.000 μs | 456.000 μs |
| N1=N2=18 | Variant 2 | 16 | Computation Time | 1254.100 μs | 1159.000 μs | 1324.000 μs |
| N1=N2=18 | Variant 2 | 16 | Total Time | 1605.600 μs | 1487.000 μs | 1780.000 μs |
| | | | | | | |
| N1=N2=1000 | Sequential | - | Reading Time | 0.403 ms | 0.377 ms | 0.444 ms |
| N1=N2=1000 | Sequential | - | Computation Time | 0.040 ms | 0.039 ms | 0.041 ms |
| N1=N2=1000 | Sequential | - | Total Time | 0.443 ms | 0.417 ms | 0.484 ms |
| N1=N2=1000 | Variant 1 | 4 | Reading Time | 358.500 μs | 315.000 μs | 459.000 μs |
| N1=N2=1000 | Variant 1 | 4 | Computation Time | 196.900 μs | 168.000 μs | 278.000 μs |
| N1=N2=1000 | Variant 1 | 4 | Total Time | 555.400 μs | 483.000 μs | 737.000 μs |
| N1=N2=1000 | Variant 1 | 8 | Reading Time | 429.600 μs | 325.000 μs | 598.000 μs |
| N1=N2=1000 | Variant 1 | 8 | Computation Time | 493.200 μs | 418.000 μs | 564.000 μs |
| N1=N2=1000 | Variant 1 | 8 | Total Time | 922.800 μs | 743.000 μs | 1135.000 μs |
| N1=N2=1000 | Variant 1 | 16 | Reading Time | 422.000 μs | 363.000 μs | 528.000 μs |
| N1=N2=1000 | Variant 1 | 16 | Computation Time | 1236.500 μs | 1130.000 μs | 1342.000 μs |
| N1=N2=1000 | Variant 1 | 16 | Total Time | 1658.500 μs | 1528.000 μs | 1837.000 μs |
| N1=N2=1000 | Variant 2 | 4 | Reading Time | 111335.500 μs | 103608.000 μs | 118618.000 μs |
| N1=N2=1000 | Variant 2 | 4 | Computation Time | 647.000 μs | 566.000 μs | 738.000 μs |
| N1=N2=1000 | Variant 2 | 4 | Total Time | 111982.500 μs | 104333.000 μs | 119338.000 μs |
| N1=N2=1000 | Variant 2 | 8 | Reading Time | 111250.900 μs | 105085.000 μs | 118162.000 μs |
| N1=N2=1000 | Variant 2 | 8 | Computation Time | 982.000 μs | 897.000 μs | 1083.000 μs |
| N1=N2=1000 | Variant 2 | 8 | Total Time | 112232.900 μs | 106021.000 μs | 119109.000 μs |
| N1=N2=1000 | Variant 2 | 16 | Reading Time | 107679.900 μs | 103573.000 μs | 114955.000 μs |
| N1=N2=1000 | Variant 2 | 16 | Computation Time | 1375.800 μs | 1320.000 μs | 1511.000 μs |
| N1=N2=1000 | Variant 2 | 16 | Total Time | 109055.700 μs | 104917.000 μs | 116329.000 μs |
| | | | | | | |
| N1=100,N2=100000 | Sequential | - | Reading Time | 2.434 ms | 2.400 ms | 2.452 ms |
| N1=100,N2=100000 | Sequential | - | Computation Time | 3.352 ms | 2.935 ms | 3.785 ms |
| N1=100,N2=100000 | Sequential | - | Total Time | 5.787 ms | 5.375 ms | 6.225 ms |
| N1=100,N2=100000 | Variant 1 | 4 | Reading Time | 4349.800 μs | 4240.000 μs | 4445.000 μs |
| N1=100,N2=100000 | Variant 1 | 4 | Computation Time | 1211.500 μs | 1108.000 μs | 1382.000 μs |
| N1=100,N2=100000 | Variant 1 | 4 | Total Time | 5561.300 μs | 5439.000 μs | 5697.000 μs |
| N1=100,N2=100000 | Variant 1 | 8 | Reading Time | 4315.900 μs | 4170.000 μs | 4490.000 μs |
| N1=100,N2=100000 | Variant 1 | 8 | Computation Time | 1708.400 μs | 1632.000 μs | 1868.000 μs |
| N1=100,N2=100000 | Variant 1 | 8 | Total Time | 6024.300 μs | 5893.000 μs | 6255.000 μs |
| N1=100,N2=100000 | Variant 1 | 16 | Reading Time | 4309.300 μs | 4198.000 μs | 4473.000 μs |
| N1=100,N2=100000 | Variant 1 | 16 | Computation Time | 2904.500 μs | 2774.000 μs | 3077.000 μs |
| N1=100,N2=100000 | Variant 1 | 16 | Total Time | 7213.800 μs | 6991.000 μs | 7421.000 μs |
| N1=100,N2=100000 | Variant 2 | 4 | Reading Time | 221760.500 μs | 210501.000 μs | 227029.000 μs |
| N1=100,N2=100000 | Variant 2 | 4 | Computation Time | 1115.800 μs | 927.000 μs | 1461.000 μs |
| N1=100,N2=100000 | Variant 2 | 4 | Total Time | 222876.300 μs | 211711.000 μs | 227956.000 μs |
| N1=100,N2=100000 | Variant 2 | 8 | Reading Time | 223632.900 μs | 213580.000 μs | 241355.000 μs |
| N1=100,N2=100000 | Variant 2 | 8 | Computation Time | 1345.000 μs | 1160.000 μs | 1570.000 μs |
| N1=100,N2=100000 | Variant 2 | 8 | Total Time | 224977.900 μs | 214953.000 μs | 242825.000 μs |
| N1=100,N2=100000 | Variant 2 | 16 | Reading Time | 218076.800 μs | 209370.000 μs | 230963.000 μs |
| N1=100,N2=100000 | Variant 2 | 16 | Computation Time | 1776.200 μs | 1658.000 μs | 2135.000 μs |
| N1=100,N2=100000 | Variant 2 | 16 | Total Time | 219853.000 μs | 211061.000 μs | 232877.000 μs |
| | | | | | | |
| N1=N2=16 | Variant 1 | 4 | Reading Time | 288.100 μs | 261.000 μs | 481.000 μs |
| N1=N2=16 | Variant 1 | 4 | Computation Time | 201.400 μs | 180.000 μs | 227.000 μs |
| N1=N2=16 | Variant 1 | 4 | Total Time | 489.500 μs | 442.000 μs | 696.000 μs |
| N1=N2=16 | Variant 2 | 4 | Reading Time | 295.100 μs | 263.000 μs | 368.000 μs |
| N1=N2=16 | Variant 2 | 4 | Computation Time | 607.500 μs | 554.000 μs | 716.000 μs |
| N1=N2=16 | Variant 2 | 4 | Total Time | 902.600 μs | 820.000 μs | 984.000 μs |
