import subprocess
import statistics
from typing import List, Dict
import json
import re


def compile_java_files():
    """Compile all necessary Java files."""
    try:
        subprocess.run(['javac', 'TestDataGenerator.java'], check=True)
        subprocess.run(['javac', 'ContestSequential.java'], check=True)
        subprocess.run(['javac', 'ContestParallel.java'], check=True)
        print("Java files compiled successfully")
    except subprocess.CalledProcessError as e:
        print(f"Error compiling Java files: {e}")
        exit(1)


def generate_test_data():
    """Generate fresh test data using TestDataGenerator."""
    try:
        subprocess.run(['java', 'TestDataGenerator'], check=True)
        print("Test data generated successfully")
    except subprocess.CalledProcessError as e:
        print(f"Error generating test data: {e}")
        exit(1)


def run_sequential(num_runs: int) -> List[float]:
    """Run sequential implementation multiple times and return execution times."""
    times = []
    for i in range(num_runs):
        try:
            result = subprocess.run(['java', 'ContestSequential'],
                                    capture_output=True,
                                    text=True,
                                    check=True)
            # Extract execution time from output using regex
            match = re.search(r'Sequential execution time: (\d+) ms', result.stdout)
            if match:
                execution_time = float(match.group(1))
                times.append(execution_time)
                print(f"Sequential run {i + 1}: {execution_time:.2f}ms")
        except subprocess.CalledProcessError as e:
            print(f"Error in sequential run {i + 1}: {e}")
    return times


def run_parallel(num_runs: int) -> Dict[str, List[float]]:
    """Run parallel implementation multiple times and return execution times."""
    results = {
        'p4_pr1': [], 'p4_pr2': [],
        'p6_pr1': [], 'p6_pr2': [],
        'p8_pr1': [], 'p8_pr2': [],
        'p16_pr1': [], 'p16_pr2': []
    }

    for i in range(num_runs):
        print(f"\nParallel run {i + 1}")
        try:
            result = subprocess.run(['java', 'ContestParallel'],
                                    capture_output=True,
                                    text=True,
                                    check=True)

            # Parse execution times using regex
            pattern = r'Running test with p=(\d+), p_r=(\d+)\nTest completed in (\d+) ms'
            matches = re.finditer(pattern, result.stdout)

            for match in matches:
                p, pr, time = match.groups()
                config = f'p{p}_pr{pr}'
                execution_time = float(time)
                results[config].append(execution_time)
                print(f"{config}: {execution_time:.2f}ms")

        except subprocess.CalledProcessError as e:
            print(f"Error in parallel run {i + 1}: {e}")

    return results


def calculate_statistics(times: List[float]) -> Dict[str, float]:
    """Calculate basic statistics for a list of execution times."""
    if not times:
        return {'mean': 0, 'median': 0, 'stdev': 0, 'min': 0, 'max': 0}
    return {
        'mean': statistics.mean(times),
        'median': statistics.median(times),
        'stdev': statistics.stdev(times) if len(times) > 1 else 0,
        'min': min(times),
        'max': max(times)
    }


def main():
    NUM_RUNS = 10

    # Compile Java files
    compile_java_files()

    # Generate fresh test data
    generate_test_data()

    # Run sequential implementation
    print("\nRunning sequential implementation...")
    sequential_times = run_sequential(NUM_RUNS)

    # Run parallel implementation
    print("\nRunning parallel implementation...")
    parallel_times = run_parallel(NUM_RUNS)

    # Calculate and print results
    print("\nResults:")
    print("\nSequential Implementation:")
    seq_stats = calculate_statistics(sequential_times)
    print(f"Average time: {seq_stats['mean']:.2f}ms")
    print(f"Median time: {seq_stats['median']:.2f}ms")
    print(f"Standard deviation: {seq_stats['stdev']:.2f}ms")
    print(f"Min time: {seq_stats['min']:.2f}ms")
    print(f"Max time: {seq_stats['max']:.2f}ms")

    print("\nParallel Implementation:")
    for config, times in parallel_times.items():
        if times:  # Only print if we have results for this configuration
            print(f"\nConfiguration: {config}")
            par_stats = calculate_statistics(times)
            print(f"Average time: {par_stats['mean']:.2f}ms")
            print(f"Median time: {par_stats['median']:.2f}ms")
            print(f"Standard deviation: {par_stats['stdev']:.2f}ms")
            print(f"Min time: {par_stats['min']:.2f}ms")
            print(f"Max time: {par_stats['max']:.2f}ms")
            print(f"Speedup (vs sequential): {seq_stats['mean'] / par_stats['mean']:.2f}x")

    # Save results to JSON file
    results = {
        'sequential': seq_stats,
        'parallel': {config: calculate_statistics(times)
                     for config, times in parallel_times.items()
                     if times}
    }

    with open('benchmark_results.json', 'w') as f:
        json.dump(results, f, indent=4)
    print("\nDetailed results saved to benchmark_results.json")


if __name__ == "__main__":
    main()