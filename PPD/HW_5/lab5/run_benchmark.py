import subprocess
import statistics
from typing import List, Dict
import json
import re
import os


def compile_java_files():
    """Compile all necessary Java files."""
    try:
        if not os.path.exists('src'):
            print("Error: src directory not found!")
            exit(1)

        java_files = [os.path.join('src', f) for f in os.listdir('src') if f.endswith('.java')]

        if not java_files:
            print("Error: No Java files found in src directory!")
            exit(1)

        print("Compiling files:", java_files)
        compilation_command = ['javac'] + java_files
        result = subprocess.run(compilation_command,
                                capture_output=True,
                                text=True,
                                check=True)
        print("Java files compiled successfully")

    except subprocess.CalledProcessError as e:
        print(f"Error compiling Java files: {e}")
        print(f"Compilation stdout: {e.stdout}")
        print(f"Compilation stderr: {e.stderr}")
        exit(1)


def parse_output(output: str) -> List[tuple]:
    """Parse the program output and return list of (pr, pw, time) tuples."""
    results = []
    lines = output.split('\n')
    i = 0
    while i < len(lines):
        line = lines[i].strip()
        if "Running test with p_r=" in line:
            # Extract pr and pw
            parts = line.split("p_r=")[1].split(", p_w=")
            pr = int(parts[0])
            pw = int(parts[1])

            # Find the next line with completion time
            while i < len(lines) and "Test completed in" not in lines[i]:
                i += 1
            if i < len(lines):
                time = int(lines[i].split("Test completed in ")[1].split(" ")[0])
                results.append((pr, pw, time))
        i += 1
    return results


def run_parallel(num_runs: int) -> Dict[str, List[float]]:
    """Run parallel implementation multiple times and return execution times."""
    results = {
        'pr4_pw2': [], 'pr4_pw4': [], 'pr4_pw12': [],
        'pr2_pw2': [], 'pr2_pw4': [], 'pr2_pw12': []
    }

    for i in range(num_runs):
        print(f"\nParallel run {i + 1}")
        try:
            result = subprocess.run(['java', '-cp', 'src', 'ContestParallel'],
                                    capture_output=True,
                                    text=True,
                                    check=True)

            print("Program output:")
            print(result.stdout)

            # Parse all results from this run
            run_results = parse_output(result.stdout)
            for pr, pw, time in run_results:
                config = f'pr{pr}_pw{pw}'
                if config in results:
                    results[config].append(time)
                    print(f"Recorded: {config}: {time}ms")

        except subprocess.CalledProcessError as e:
            print(f"Error in parallel run {i + 1}:")
            print(f"Return code: {e.returncode}")
            print(f"stdout: {e.stdout}")
            print(f"stderr: {e.stderr}")

    # Print summary of collected results
    print("\nCollected results:")
    for config, times in results.items():
        print(f"{config}: {len(times)} runs collected")

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
    NUM_RUNS = 5

    print("Starting benchmark script...")
    compile_java_files()

    print("\nRunning parallel implementation...")
    parallel_times = run_parallel(NUM_RUNS)

    # Check if we got any results
    total_results = sum(len(times) for times in parallel_times.values())
    if total_results == 0:
        print("\nError: No test results were collected!")
        exit(1)

    print("\nResults:")

    fastest_config = None
    fastest_time = float('inf')

    for config, times in parallel_times.items():
        if times:
            print(f"\nConfiguration: {config}")
            stats = calculate_statistics(times)
            print(f"Average time: {stats['mean']:.2f}ms")
            print(f"Median time: {stats['median']:.2f}ms")
            print(f"Standard deviation: {stats['stdev']:.2f}ms")
            print(f"Min time: {stats['min']:.2f}ms")
            print(f"Max time: {stats['max']:.2f}ms")

            if stats['mean'] < fastest_time:
                fastest_time = stats['mean']
                fastest_config = config

    if fastest_config:
        print(f"\nFastest configuration: {fastest_config} ({fastest_time:.2f}ms)")
        print("\nRelative performance (ratio to fastest):")
        for config, times in parallel_times.items():
            if times:
                avg_time = calculate_statistics(times)['mean']
                ratio = avg_time / fastest_time
                print(f"{config}: {ratio:.2f}x slower than fastest")

    results = {
        'configurations': {
            config: calculate_statistics(times)
            for config, times in parallel_times.items()
            if times
        },
        'fastest_config': {
            'config': fastest_config,
            'time': fastest_time if fastest_time != float('inf') else None
        }
    }

    with open('benchmark_results.json', 'w') as f:
        json.dump(results, f, indent=4)
    print("\nDetailed results saved to benchmark_results.json")


if __name__ == "__main__":
    main()