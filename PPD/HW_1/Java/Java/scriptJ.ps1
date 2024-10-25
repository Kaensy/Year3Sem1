# scriptJ.ps1
param (
    [Parameter(Mandatory=$true)]
    [int]$numberOfRuns
)

# Configuration
$threadCounts = @(2, 4, 8, 16)  # Changed this line to only include parallel thread counts
$outputFile = "results.csv"

# Create or clear the output file
"N=10000, M=10000`nk=5`n" | Set-Content $outputFile
"Implementation,Threads,Time (ns)" | Add-Content $outputFile

# Run sequential first
$seqTimes = @()
Write-Host "Running sequential implementation..."
for ($i = 1; $i -le $numberOfRuns; $i++) {
    Write-Host "  Run $i of $numberOfRuns"
    $output = java -cp target/classes Main 1
    if ($output -match "(\d+) \d+ \d+") {
        $seqTimes += [long]$matches[1]
    }
}
$seqAvg = ($seqTimes | Measure-Object -Average).Average
"sequential,1,$seqAvg" | Add-Content $outputFile

# Then run parallel implementations
foreach ($threads in $threadCounts) {
    Write-Host "Testing with $threads threads..."
    
    $vertTimes = @()
    $horTimes = @()
    
    for ($i = 1; $i -le $numberOfRuns; $i++) {
        Write-Host "  Run $i of $numberOfRuns"
        $output = java -cp target/classes Main $threads
        
        if ($output -match "\d+ (\d+) (\d+)") {
            $vertTimes += [long]$matches[1]
            $horTimes += [long]$matches[2]
        }
    }
    
    # Calculate averages
    $vertAvg = ($vertTimes | Measure-Object -Average).Average
    $horAvg = ($horTimes | Measure-Object -Average).Average
    
    # Write parallel results
    "$threads-vertical,$threads,$vertAvg" | Add-Content $outputFile
    "$threads-horizontal,$threads,$horAvg" | Add-Content $outputFile
}

Write-Host "Testing complete! Results saved to $outputFile"