param(
    [Parameter(Mandatory=$true)]
    [string]$executable = "cmake-build-debug\c__exe",
    [Parameter(Mandatory=$true)]
    [int]$threadCount,
    [Parameter(Mandatory=$true)]
    [int]$runCount
)

Write-Host "Testing Matrix Convolution Program"
Write-Host "================================="
Write-Host "Executable: $executable"
Write-Host "Thread Count: $threadCount"
Write-Host "Number of Runs: $runCount"
Write-Host ""

$totalSeqTime = 0
$totalParTime = 0

# Create or ensure the results file exists with headers
$resultsFile = "convolution_results.csv"
if (!(Test-Path $resultsFile)) {
    New-Item $resultsFile -ItemType File
    Add-Content $resultsFile "Matrix Size | Threads | Avg Sequential Time (ns) | Avg Parallel Time (ns)"
}

# Extract matrix size from program output or use default
$matrixSize = "10000x10000"  # Default size based on your constants
$totalSeqTimeNs = 0
$totalParTimeNs = 0

# Get the full path to the executable
$execFullPath = Join-Path (Get-Location) $executable

if (!(Test-Path $execFullPath)) {
    Write-Host "Error: Executable not found at path: $execFullPath"
    Write-Host "Current directory: $(Get-Location)"
    Write-Host "Available files in cmake-build-debug:"
    Get-ChildItem "cmake-build-debug" | Format-Table Name
    exit 1
}

Write-Host "Using executable at: $execFullPath"

for ($i = 1; $i -le $runCount; $i++) {
    Write-Host "`nRun $i of $runCount"
    Write-Host "-------------"
    
    try {
        # Execute the program from its directory
        $execDir = Split-Path $execFullPath -Parent
        $execName = Split-Path $execFullPath -Leaf
        
        Set-Location $execDir
        Write-Host "Executing from directory: $(Get-Location)"
        Write-Host "Running: .\$execName"
        
        $output = & ".\$execName" 2>&1
        
        # Process the output
        $times = @()
        foreach ($line in $output) {
            Write-Host $line
            if ($line -match "Execution Time: (\d+) ns") {
                $times += [long]$Matches[1]
            }
        }
        
        # Store the times (assuming first is vertical, second is horizontal)
        if ($times.Count -ge 2) {
            $totalSeqTimeNs += $times[0]
            $totalParTimeNs += $times[1]
            Write-Host "Captured times: $($times[0]) ns, $($times[1]) ns"
        }
        else {
            Write-Host "Warning: Did not get expected number of timing measurements"
        }
    }
    catch {
        Write-Host "Error executing program: $_"
        continue
    }
    finally {
        # Return to original directory
        Set-Location (Split-Path $execFullPath -Parent)
    }
}

if ($runCount -gt 0) {
    # Calculate averages
    $avgSeqTimeNs = [long]($totalSeqTimeNs / $runCount)
    $avgParTimeNs = [long]($totalParTimeNs / $runCount)

    # Format the output line with proper spacing
    $outputLine = "{0} | {1} | {2} | {3}" -f $matrixSize, $threadCount, $avgSeqTimeNs, $avgParTimeNs

    # Append results to CSV
    Add-Content $resultsFile $outputLine

    # Display summary
    Write-Host "`nFinal Results"
    Write-Host "============="
    Write-Host "Matrix Size: $matrixSize"
    Write-Host "Thread Count: $threadCount"
    Write-Host "Average Vertical Parallel Time: $avgSeqTimeNs ns"
    Write-Host "Average Horizontal Parallel Time: $avgParTimeNs ns"
    if ($avgParTimeNs -ne 0) {
        Write-Host "Speedup: $([math]::Round($avgSeqTimeNs / $avgParTimeNs, 2))x"
    }
    Write-Host "`nResults have been saved to $resultsFile"
}
else {
    Write-Host "No successful runs completed"
}