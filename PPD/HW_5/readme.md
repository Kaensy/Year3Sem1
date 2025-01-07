# Programming Contest Implementation Performance Analysis - Lab 5

## Raw Performance Results

### Parallel Implementation Results with Fine-Grained Synchronization

| Configuration | Average Time (ms) | Range (ms) | Relative Performance |
|--------------|-------------------|------------|---------------------|
| pr4_pw2      | 71.6             | 66.0 - 76.0| 7.16x slower       |
| pr4_pw4      | 20.2             | 19.0 - 22.0| 2.02x slower       |
| pr4_pw12     | 27.8             | 27.0 - 29.0| 2.78x slower       |
| pr2_pw2      | 10.0             | 9.0 - 11.0 | baseline (fastest)  |
| pr2_pw4      | 20.0             | 18.0 - 22.0| 2.00x slower       |
| pr2_pw12     | 23.0             | 21.0 - 24.0| 2.30x slower       |

## Key Findings

### 1. Performance Overview
- The implementation with fine-grained synchronization showed significant performance variations across different thread configurations
- All configurations demonstrated stable performance with relatively low standard deviations
- The balance between reader and worker threads proved crucial for optimal performance

### 2. Configuration Analysis
1. **Best Configuration**: pr2_pw2 (2 readers, 2 workers)
   - Average time: 10.0ms
   - Most consistent performance (std dev: 1.0ms)
   - Optimal balance between parallelism and overhead

2. **Worst Configuration**: pr4_pw2 (4 readers, 2 workers)
   - Average time: 71.6ms
   - Highest variability (std dev: 4.39ms)
   - Demonstrates the impact of reader-worker imbalance

### 3. Impact of Reader Threads
- Configurations with 2 reader threads (pr2) consistently outperformed those with 4 readers
- Key observations:
  * pr2_pw2: 10.0ms (best performance)
  * pr4_pw2: 71.6ms (worst performance)
  * Suggests that fewer reader threads reduce contention

### 4. Worker Thread Scaling Analysis
- For 2 reader threads (pr2):
  * 2 workers: 10.0ms (optimal)
  * 4 workers: 20.0ms (2x slower)
  * 12 workers: 23.0ms (2.3x slower)
- For 4 reader threads (pr4):
  * 2 workers: 71.6ms
  * 4 workers: 20.2ms
  * 12 workers: 27.8ms
- Adding more worker threads beyond the optimal point (2) decreased performance

### 5. Performance Consistency
- Most configurations showed stable performance:
  * pr2_pw2: σ = 1.0ms
  * pr4_pw4: σ = 1.1ms
  * pr4_pw2: σ = 4.4ms (most variable)
- Lower thread counts generally resulted in more consistent performance

## Conclusions

1. **Optimal Threading**: The best performance was achieved with a balanced, minimal thread configuration (2 readers, 2 workers)

2. **Scalability Limits**: 
   - Adding more threads did not improve performance
   - Performance degraded with thread counts above the optimal configuration

3. **Reader-Worker Balance**:
   - Equal numbers of reader and worker threads performed best
   - Imbalanced configurations (especially more readers than workers) performed poorly

4. **Fine-Grained Synchronization Impact**:
   - The implementation showed best results with fewer threads
   - Suggests that minimizing contention is more important than maximizing parallelism

5. **Practical Recommendations**:
   - Use a balanced 2:2 reader-worker configuration
   - Avoid configurations with more readers than workers
   - Keep total thread count low to minimize synchronization overhead