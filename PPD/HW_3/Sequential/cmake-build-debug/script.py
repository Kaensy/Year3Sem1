import subprocess
import statistics
import re
import argparse
from typing import List, Tuple


def parse_execution_times(output: str) -> Tuple[float, float, float]:
    """Extract reading, addition and total times from program output."""
    reading_time = float(re.search(r"Reading time: ([\d.]+) ms", output).group(1))
    addition_time = float(re.search(r"Addition time: ([\d.]+) ms", output).group(1))
    total_time = float(re.search(r"Total time: ([\d.]+) ms", output).group(1))
    return reading_time, addition_time, total_time


def run_benchmark(executable_path: str, iterations: int) -> dict:
    """Run the program multiple times and collect timing statistics."""
    reading_times = []
    addition_times = []
    total_times = []

    print(f"Running benchmark for {iterations} iterations...")

    for i in range(iterations):
        try:
            # Run the compiled program and capture output
            result = subprocess.run([executable_path],
                                    capture_output=True,
                                    text=True)

            if result.returncode != 0:
                print(f"Error in iteration {i + 1}: {result.stderr}")
                continue

            # Parse the timing results
            reading_time, addition_time, total_time = parse_execution_times(result.stdout)

            reading_times.append(reading_time)
            addition_times.append(addition_time)
            total_times.append(total_time)

            # Print progress
            if (i + 1) % 10 == 0:
                print(f"Completed {i + 1}/{iterations} iterations")

        except Exception as e:
            print(f"Error in iteration {i + 1}: {str(e)}")
            continue

    # Calculate statistics
    stats = {
        'reading': {
            'avg': statistics.mean(reading_times),
            'min': min(reading_times),
            'max': max(reading_times),
            'stddev': statistics.stdev(reading_times) if len(reading_times) > 1 else 0
        },
        'addition': {
            'avg': statistics.mean(addition_times),
            'min': min(addition_times),
            'max': max(addition_times),
            'stddev': statistics.stdev(addition_times) if len(addition_times) > 1 else 0
        },
        'total': {
            'avg': statistics.mean(total_times),
            'min': min(total_times),
            'max': max(total_times),
            'stddev': statistics.stdev(total_times) if len(total_times) > 1 else 0
        }
    }

    return stats


def print_statistics(stats: dict):
    """Print formatted statistics."""
    print("\nBenchmark Results:")
    print("-" * 50)

    for operation in ['reading', 'addition', 'total']:
        print(f"\n{operation.capitalize()} Time Statistics (ms):")
        print(f"  Average: {stats[operation]['avg']:.3f}")
        print(f"  Minimum: {stats[operation]['min']:.3f}")
        print(f"  Maximum: {stats[operation]['max']:.3f}")
        print(f"  Std Dev: {stats[operation]['stddev']:.3f}")


def main():
    parser = argparse.ArgumentParser(description='Benchmark the large number addition program')
    parser.add_argument('executable', help='Path to the compiled executable')
    parser.add_argument('-n', '--iterations', type=int, default=100,
                        help='Number of iterations to run (default: 100)')

    args = parser.parse_args()

    try:
        stats = run_benchmark(args.executable, args.iterations)
        print_statistics(stats)
    except Exception as e:
        print(f"Error running benchmark: {str(e)}")
        return 1

    return 0


if __name__ == "__main__":
    exit(main())