# Parameters for the script
param(
    [int]$ThreadCount = 16,       # Default thread count
    [int]$RunsPerTest = 5,        # Default number of runs per test
    [string]$InputFile = "N1000M1000k5.txt"  # Default input file
)

# Setup directories
$srcDir = "src"
$outDir = "out"

# Create output directory if it doesn't exist
if (!(Test-Path $outDir)) {
    New-Item -ItemType Directory -Path $outDir | Out-Null
}

# Clean any existing class files
Remove-Item "$outDir\*.class" -ErrorAction SilentlyContinue

# Compile Java files
Write-Host "Compiling Java files..."
$compileResult = javac -d $outDir "$srcDir\*.java" 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "Error: Compilation failed!"
    Write-Host $compileResult
    exit 1
}

# Verify Main.class exists
if (!(Test-Path "$outDir\Main.class")) {
    Write-Host "Error: Main.class not found after compilation!"
    exit 1
}

Write-Host "Running tests with $ThreadCount threads, $RunsPerTest runs per test"
Write-Host "Using input file: $InputFile"
Write-Host "----------------------------------------"

$totalTime = 0

for ($i = 1; $i -le $RunsPerTest; $i++) {
    Write-Host "`nRun $i of $RunsPerTest"
    
    # Run Java with explicit class path pointing to the out directory
    $result = java -cp $outDir Main generate $ThreadCount 2>&1
    
    if ($result -match "(\d+) (\d+) (\d+)") {
        $seqTime = [double]$matches[1] / 1e6  # Convert to milliseconds
        $vertTime = [double]$matches[2] / 1e6
        $horTime = [double]$matches[3] / 1e6
        
        Write-Host "Sequential: $seqTime ms"
        Write-Host "Vertical: $vertTime ms"
        Write-Host "Horizontal: $horTime ms"
        
        $totalTime += $horTime  # Track horizontal implementation time
    } else {
        Write-Host "Warning: Unexpected output format from run $i"
        Write-Host "Output: $result"
    }
    
    # Add a small delay between runs
    Start-Sleep -Milliseconds 500
}

$avgTime = $totalTime / $RunsPerTest

# Create or append to CSV file
$csvFile = "convolution_results.csv"
if (!(Test-Path $csvFile)) {
    "Input File,Threads,Average Time (ms)" | Out-File $csvFile
}

"$InputFile,$ThreadCount,$avgTime" | Add-Content $csvFile

Write-Host "`nResults Summary:"
Write-Host "Average execution time: $avgTime ms"
Write-Host "Results saved in convolution_results.csv"