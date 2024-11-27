# Programming Contest Implementation Performance Analysis

## Raw Performance Results

### Sequential Implementation
- **Average Time**: 59.4ms
- **Range**: 47.0ms - 162.0ms

### Parallel Implementation Results

| Configuration | Average Time (ms) | Range (ms) | Speedup vs Sequential |
|--------------|-------------------|------------|----------------------|
| p4_pr1       | 28.9             | 28.0 - 31.0| 2.06x               |
| p4_pr2       | 10.2             | 9.0 - 12.0 | 5.82x               |
| p6_pr1       | 17.2             | 16.0 - 20.0| 3.45x               |
| p6_pr2       | 9.3              | 7.0 - 11.0 | 6.39x               |
| p8_pr1       | 11.9             | 11.0 - 13.0| 4.99x               |
| p8_pr2       | 8.7              | 8.0 - 10.0 | 6.83x               |
| p16_pr1      | 12.2             | 11.0 - 13.0| 4.87x               |
| p16_pr2      | 8.8              | 7.0 - 11.0 | 6.75x               |

## Key Findings

### 1. Performance Overview
- All parallel configurations significantly outperformed the sequential implementation
- The sequential implementation showed more variance in execution times
- Parallel implementations demonstrated more consistent performance

### 2. Best Performing Configurations
1. **p8_pr2**: 8.7ms (6.83x speedup)
2. **p16_pr2**: 8.8ms (6.75x speedup)
3. **p6_pr2**: 9.3ms (6.39x speedup)
4. **p4_pr2**: 10.2ms (5.82x speedup)

### 3. Impact of Reader Threads
- Configurations with 2 reader threads (pr=2) consistently outperformed those with 1 reader
- Most dramatic improvement seen in 4-thread configuration:
  * p4_pr1: 28.9ms
  * p4_pr2: 10.2ms (2.83x improvement)

### 4. Scaling Analysis
- Optimal performance achieved with 6-8 total threads and 2 readers
- Diminishing returns observed beyond 8 threads
- Adding more threads beyond 8 provided minimal performance benefits
- Two reader threads consistently proved more efficient than one reader across all configurations

### 5. Performance Consistency
- Parallel implementations showed more stable performance with narrower time ranges
- Sequential implementation showed larger variation in execution times
- Configurations with 2 readers maintained more consistent performance across runs

## Conclusion
The parallel implementation successfully improved performance, with the optimal configuration being 8 total threads with 2 reader threads (p8_pr2), achieving nearly 7x speedup over the sequential version. The data also clearly shows that using 2 reader threads is beneficial for all configurations, suggesting that I/O operations were a significant bottleneck in the single-reader configurations.