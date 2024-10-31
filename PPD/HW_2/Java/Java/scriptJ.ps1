# Parameters from command line
$param1 = $args[0] # Java class name (without .class extension)
$param2 = $args[1] # Input file name (without .txt extension)
$param3 = $args[2] # Number of threads
$param4 = $args[3] # Number of runs

# Check if we have enough parameters
if ($args.Count -lt 4) {
    Write-Host "Usage: .\scriptJ.ps1 <class_name> <input_file> <num_threads> <num_runs>"
    Write-Host "Example: .\scriptJ.ps1 Main N10M10k3 8 10"
    exit 1
}

# Check if we have the input file
if (!(Test-Path "$param2.txt")) {
    Write-Host "Error: '$param2.txt' not found in current directory"
    exit 1
}

# Check if we have compiled classes in the out directory
if (!(Test-Path "out")) {
    Write-Host "Error: 'out' directory not found"
    Write-Host "Please compile your Java files first"
    exit 1
}

# Variables to store timing data
$seqSum = 0
$parSum = 0
$speedupSum = 0

# Change to the out directory for Java execution
Set-Location out

for ($i = 0; $i -lt $param4; $i++) {
    Write-Host "Run" ($i+1)
    
    # Run Java program with proper arguments including .txt extension
    $output = java $param1 "../$param2.txt" $param3
    
    # Process output lines
    foreach ($line in $output) {
        Write-Host $line
        
        if ($line -match "Sequential execution time: (\d+)") {
            $seqTime = $matches[1]
            $seqSum += [double]$seqTime
        }
        elseif ($line -match "Parallel execution time: (\d+)") {
            $parTime = $matches[1]
            $parSum += [double]$parTime
        }
    }
    
    Write-Host ""
}

# Return to original directory
Set-Location ..

# Calculate averages
$seqAvg = $seqSum / $param4
$parAvg = $parSum / $param4

# Display averages
Write-Host "Summary:"
Write-Host "Average Sequential Time: $seqAvg ns"
Write-Host "Average Parallel Time: $parAvg ns"

# CSV file handling
$csvPath = "convolution_results.csv"

# Create CSV file if it doesn't exist
if (!(Test-Path $csvPath)) {
    New-Item $csvPath -ItemType File
    Set-Content $csvPath 'Matrix Size | Threads | Avg Sequential Time (ns) | Avg Parallel Time (ns)'
}

# Get matrix size from input file
$matrixSize = "NxN" # Default value
if (Test-Path "$param2.txt") {
    $firstLine = Get-Content "$param2.txt" -First 1
    if ($firstLine -match "(\d+)\s+(\d+)") {
        $matrixSize = "$($matches[1])x$($matches[2])"
    }
}

# Append results
Add-Content $csvPath "$matrixSize | $($param3) | $($seqAvg) | $($parAvg)"