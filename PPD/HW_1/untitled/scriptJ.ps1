$param1 = $args[0] # Nume fisier java
$param2 = $args[1] # Input file (N10M10k3)
$param3 = $args[2] # No of threads
$param4 = $args[3] # No of runs

# $javaPath = "C:\Program Files\jdk-21.0.5+11\bin\java.exe"
$classPath = ".\out\production\untitled"

# Function to get matrix dimensions from filename
function Get-MatrixDescription {
    param ($filename)
    if ($filename -match "N(\d+)M(\d+)k(\d+)") {
        $n = $matches[1]
        $m = $matches[2]
        $k = $matches[3]
        return "N=$n M=$m k=$k"  # Changed to show both N and M separately
    }
    return $filename
}

# Initialize arrays to store times
$seqTimes = @()
$horTimes = @()
$vertTimes = @()

for ($i = 0; $i -lt $param4; $i++){
    Write-Host "Rulare" ($i+1)
    # $output = & $javaPath -cp $classPath Main $param2 $param3
    
    foreach ($line in $output) {
        if ($line -match "Sequential:(\d+)") {
            $seqTimes += [long]$matches[1]
        }
        elseif ($line -match "Horizontal:(\d+)") {
            $horTimes += [long]$matches[1]
        }
        elseif ($line -match "Vertical:(\d+)") {
            $vertTimes += [long]$matches[1]
        }
    }
    Write-Host ""
}

# Calculate averages
$seqAvg = if ($seqTimes.Count -gt 0) { ($seqTimes | Measure-Object -Average).Average } else { 0 }
$horAvg = if ($horTimes.Count -gt 0) { ($horTimes | Measure-Object -Average).Average } else { 0 }
$vertAvg = if ($vertTimes.Count -gt 0) { ($vertTimes | Measure-Object -Average).Average } else { 0 }

# Create or clear the CSV file
if (!(Test-Path outJ.csv)) {
    New-Item outJ.csv -ItemType File
}
Set-Content outJ.csv "Tip Matrice,Nr Threads,Timp Executie"

# Get matrix description
$matrixDesc = Get-MatrixDescription $param2

# Append data to CSV
Add-Content outJ.csv "$matrixDesc,,"
Add-Content outJ.csv "Sequential,1,$seqAvg"
Add-Content outJ.csv "$($param3)-horizontal,$param3,$horAvg"
Add-Content outJ.csv "$($param3)-vertical,$param3,$vertAvg"
Add-Content outJ.csv "" # Empty line between different runs