import subprocess
import statistics
import re
import argparse
import os
from typing import List, Tuple


def parse_execution_times(output: str) -> Tuple[float, float, float, float]:
    """Extract reading, computation, and total times from program output."""
    try:
        file_size = int(re.search(r"File sizes: (\d+) digits", output).group(1))
        reading_time = float(re.search(r"Reading time: ([\d.]+) microseconds", output).group(1))
        total_time = float(re.search(r"Total execution time: ([\d.]+) microseconds", output).group(1))
        computation_time = float(re.search(r"Computation time: ([\d.]+) microseconds", output).group(1))
        performance = float(re.search(r"Performance: ([\d.]+) digits/microsecond", output).group(1))
        return reading_time, computation_time, total_time, performance
    except AttributeError as e:
        print(f"Failed to parse output: {output}")
        raise e


def run_benchmark(executable_path: str, num_processes: int, iterations: int) -> dict:
    """Run the MPI program multiple times and collect timing statistics."""
    reading_times = []
    computation_times = []
    total_times = []
    performance_metrics = []

    # Convert to absolute path and normalize
    executable_path = os.path.abspath(executable_path)
    print(f"Running benchmark for {iterations} iterations with {num_processes} processes...")
    print(f"Using executable: {executable_path}")

    # First, check if the executable exists
    if not os.path.exists(executable_path):
        raise FileNotFoundError(f"Executable not found: {executable_path}")

    # For Windows, use mpiexec without ./ prefix
    mpi_command = ['mpiexec', '-n', str(num_processes), executable_path]

    # Try running once to check for immediate errors
    try:
        print("Running test with command:", ' '.join(mpi_command))
        test_run = subprocess.run(
            mpi_command,
            capture_output=True,
            text=True
        )
        print("\nTest run output:")
        print(f"stdout: {test_run.stdout}")
        if test_run.stderr:
            print(f"stderr: {test_run.stderr}")
        print(f"Return code: {test_run.returncode}\n")
    except Exception as e:
        print(f"Error during test run: {str(e)}")
        raise

    for i in range(iterations):
        try:
            result = subprocess.run(
                mpi_command,
                capture_output=True,
                text=True
            )

            if result.returncode != 0:
                print(f"Error in iteration {i + 1}:")
                print(f"Return code: {result.returncode}")
                print(f"stderr: {result.stderr}")
                print(f"stdout: {result.stdout}")
                continue

            # Parse the timing results
            reading_time, computation_time, total_time, performance = parse_execution_times(result.stdout)

            reading_times.append(reading_time)
            computation_times.append(computation_time)
            total_times.append(total_time)
            performance_metrics.append(performance)

            # Print progress
            print(f"Completed iteration {i + 1}/{iterations}: {total_time:.3f} microseconds")

        except Exception as e:
            print(f"Error in iteration {i + 1}: {str(e)}")
            continue

    if not reading_times:
        raise RuntimeError("No successful iterations completed")

    # Calculate statistics
    stats = {
        'reading': {
            'avg': statistics.mean(reading_times),
            'min': min(reading_times),
            'max': max(reading_times),
            'stddev': statistics.stdev(reading_times) if len(reading_times) > 1 else 0,
            'successful_runs': len(reading_times)
        },
        'computation': {
            'avg': statistics.mean(computation_times),
            'min': min(computation_times),
            'max': max(computation_times),
            'stddev': statistics.stdev(computation_times) if len(computation_times) > 1 else 0,
            'successful_runs': len(computation_times)
        },
        'total': {
            'avg': statistics.mean(total_times),
            'min': min(total_times),
            'max': max(total_times),
            'stddev': statistics.stdev(total_times) if len(total_times) > 1 else 0,
            'successful_runs': len(total_times)
        },
        'performance': {
            'avg': statistics.mean(performance_metrics),
            'min': min(performance_metrics),
            'max': max(performance_metrics),
            'stddev': statistics.stdev(performance_metrics) if len(performance_metrics) > 1 else 0,
            'successful_runs': len(performance_metrics)
        }
    }

    return stats


def print_statistics(stats: dict):
    """Print formatted statistics."""
    print("\nBenchmark Results:")
    print("-" * 50)

    for operation, label in [
        ('reading', 'Reading Time (microseconds)'),
        ('computation', 'Computation Time (microseconds)'),
        ('total', 'Total Time (microseconds)'),
        ('performance', 'Performance (digits/microsecond)')
    ]:
        print(f"\n{label}:")
        print(f"  Average: {stats[operation]['avg']:.3f}")
        print(f"  Minimum: {stats[operation]['min']:.3f}")
        print(f"  Maximum: {stats[operation]['max']:.3f}")
        print(f"  Std Dev: {stats[operation]['stddev']:.3f}")
        print(f"  Successful Runs: {stats[operation]['successful_runs']}")


def main():
    parser = argparse.ArgumentParser(description='Benchmark the MPI large number addition program')
    parser.add_argument('executable', help='Path to the compiled executable')
    parser.add_argument('-n', '--iterations', type=int, default=10,
                        help='Number of iterations to run (default: 10)')
    parser.add_argument('-p', '--processes', type=int, default=4,
                        help='Number of MPI processes to use (default: 4)')

    args = parser.parse_args()

    try:
        stats = run_benchmark(args.executable, args.processes, args.iterations)
        print_statistics(stats)
    except Exception as e:
        print(f"Error running benchmark: {str(e)}")
        return 1

    return 0


if __name__ == "__main__":
    exit(main())