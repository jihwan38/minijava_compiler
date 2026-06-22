# MiniJava to MIPS Compiler Correctness Auto-Tester (test_all.ps1)
# Automates JVM execution vs MARS MIPS simulation outputs diff comparison.

$MarsJar = "$PSScriptRoot/../mars.jar"
$ProgramsDir = "../programs"
$DumpsDir = "./dumps"
$BinDir = "./bin"

# Create output directories
New-Item -ItemType Directory -Force -Path $DumpsDir | Out-Null
New-Item -ItemType Directory -Force -Path $BinDir | Out-Null

$Files = @(
    "Factorial",
    "BinarySearch",
    "BubbleSort",
    "LinearSearch",
    "LinkedList",
    "QuickSort",
    "TreeVisitor",
    "BinaryTree"
)

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "         MINIJAVA-TO-MIPS CORRECTNESS TESTING RUNNER      " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. Compile current Compiler sources (under chap11)
Write-Host "[1/3] Compiling Compiler Source Files..." -ForegroundColor Yellow
$Sources = Get-ChildItem -Path "./" -Recurse -Include "*.java" | ForEach-Object { $_.FullName }
if ($Sources.Count -eq 0) {
    Write-Error "No Java sources found in the current directory! Ensure you run this inside the chap11 source root."
    Exit 1
}

# Compile compiler
javac -encoding UTF-8 -d $BinDir $Sources
if ($LASTEXITCODE -ne 0) {
    Write-Error "Compiler compilation failed!"
    Exit 1
}
Write-Host "  -> Compiler built successfully inside $BinDir" -ForegroundColor Green

# 2. Loop through each program and test
$Results = @()
$TableOutput = ""

foreach ($File in $Files) {
    Write-Host "----------------------------------------------------------" -ForegroundColor Gray
    Write-Host "Testing $File.java..." -ForegroundColor Cyan

    $JavaFile = "$ProgramsDir/$File.java"
    if (-not (Test-Path $JavaFile)) {
        Write-Warning "File $JavaFile not found, skipping."
        continue
    }

    # A. Run via Java VM (Expected Output)
    # Compile the MiniJava program itself (needs javac)
    $TempClassDir = "$DumpsDir/temp_class_$File"
    New-Item -ItemType Directory -Force -Path $TempClassDir | Out-Null
    javac -d $TempClassDir $JavaFile
    if ($LASTEXITCODE -ne 0) {
        Write-Warning "Could not compile $File.java using javac!"
        continue
    }

    $ExpectedPath = "$DumpsDir/expected_$File.txt"
    # Execute Java class to capture standard output
    java -cp $TempClassDir $File > $ExpectedPath
    $ExpectedText = (Get-Content $ExpectedPath -Raw).Trim()

    # B. Compile via our MiniJava-to-MIPS Compiler (Actual Assembly)
    $AsmPath = "$DumpsDir/$File.s"
    java -cp $BinDir Main $JavaFile > $AsmPath
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Failed to compile $File.java to MIPS assembly using our compiler!"
        $Results += [PSCustomObject]@{ Program = $File; Status = "FAIL (Compiler Error)"; Expected = $ExpectedText; Actual = "N/A" }
        continue
    }

    # C. Run generated MIPS assembly in MARS simulator
    $ActualPath = "$DumpsDir/actual_$File.txt"
    # Run MARS in non-gui, quiet mode: java -jar mars.jar nc <file>
    java -jar $MarsJar nc $AsmPath > $ActualPath
    if ($LASTEXITCODE -ne 0) {
        Write-Error "MARS simulation failed for $File.s!"
        $Results += [PSCustomObject]@{ Program = $File; Status = "FAIL (MARS Error)"; Expected = $ExpectedText; Actual = "N/A" }
        continue
    }
    
    $ActualTextRaw = (Get-Content $ActualPath -Raw)
    # Process MARS output to strip any simulator header/footer lines if present
    # Usually 'nc' mode output matches standard output directly.
    $ActualText = $ActualTextRaw.Trim()

    # D. Compare outputs
    if ($ExpectedText -eq $ActualText) {
        Write-Host "  -> Verification SUCCESS!" -ForegroundColor Green
        $Status = "PASS"
    } else {
        Write-Host "  -> Verification FAILED!" -ForegroundColor Red
        Write-Host "     Expected: `"$ExpectedText`"" -ForegroundColor Yellow
        Write-Host "     Actual:   `"$ActualText`"" -ForegroundColor Red
        $Status = "FAIL (Diff Mismatch)"
    }

    $Results += [PSCustomObject]@{
        Program  = $File
        Status   = $Status
        Expected = $ExpectedText
        Actual   = $ActualText
    }
}

# 3. Print final report
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "                    FINAL CORRECTNESS REPORT              " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

$MarkdownTable = "| 프로그램명 (Programs) | Java 실행 결과 | MIPS (MARS) 실행 결과 | 동일 여부 (Correctness) |`n"
$MarkdownTable += "| :--- | :--- | :--- | :---: |`n"

foreach ($Res in $Results) {
    # Escape newlines in results for markdown table
    $EscapedExpected = $Res.Expected -replace "`r`n", " \n " -replace "`n", " \n "
    $EscapedActual = $Res.Actual -replace "`r`n", " \n " -replace "`n", " \n "
    $StatusCol = if ($Res.Status -eq "PASS") { "**PASS**" } else { "*$($Res.Status)*" }
    
    $MarkdownTable += "| $($Res.Program).java | `$EscapedExpected | `$EscapedActual | $StatusCol |`n"
    
    $Color = if ($Res.Status -eq "PASS") { "Green" } else { "Red" }
    Write-Host ("{0,-20} : {1}" -f $Res.Program, $Res.Status) -ForegroundColor $Color
}

# Write table to temporary report file for easy copy-paste
$TableOutputPath = "$DumpsDir/correctness_table.md"
$MarkdownTable | Out-File -FilePath $TableOutputPath -Encoding utf8
Write-Host "----------------------------------------------------------" -ForegroundColor Gray
Write-Host "Markdown Correctness Table exported to: $TableOutputPath" -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Cyan
