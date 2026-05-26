#requires -Version 5.1
<#
.SYNOPSIS
    Generates a structured snapshot of a Minecraft mod project for sharing with Claude.

.DESCRIPTION
    Walks the target directory, builds a tree view, then dumps the contents of
    source / config / data / resource files. Skips build outputs, IDE folders,
    git internals, and binary assets.

.PARAMETER Path
    Root directory to snapshot. Defaults to current directory.

.PARAMETER OutputFile
    Output filename. Defaults to "geotectonic-snapshot.txt" in current directory.

.EXAMPLE
    .\snapshot.ps1
    .\snapshot.ps1 -Path "C:\Projects\GeoTectonic" -OutputFile "snap.txt"
#>

param(
    [string]$Path = (Get-Location).Path,
    [string]$OutputFile = "geotectonic-snapshot.txt"
)

# ---------------------------------------------------------------------------
# Configuration
# ---------------------------------------------------------------------------

# Directories to skip entirely
$ExcludeDirs = @(
    'build', '.gradle', '.idea', '.vscode', 'out', 'bin', 'run',
    '.git', 'node_modules', '__pycache__', '.kotlin', 'gradle',
    'libs', 'logs', 'crash-reports', 'saves', 'screenshots'
)

# File extensions whose contents we want dumped
$IncludeContentExtensions = @(
    '.java', '.kt', '.json', '.toml', '.mcmeta', '.properties',
    '.gradle', '.kts', '.md', '.txt', '.yml', '.yaml', '.cfg',
    '.xml', '.gitignore', '.editorconfig'
)

# Specific filenames (no extension or special) to include contents for
$IncludeContentFilenames = @(
    'gradlew', 'gradlew.bat', 'settings.gradle', 'build.gradle',
    'LICENSE', 'LICENSE.md', 'README', 'README.md', '.gitattributes'
)

# Per-file line cap so a runaway file doesn't dominate the report
$MaxLinesPerFile = 500

# Resolve absolute paths
$RootPath = (Resolve-Path -LiteralPath $Path).Path
$OutputPath = if ([System.IO.Path]::IsPathRooted($OutputFile)) {
    $OutputFile
} else {
    Join-Path (Get-Location).Path $OutputFile
}

Write-Host "Scanning: $RootPath" -ForegroundColor Cyan
Write-Host "Output:   $OutputPath" -ForegroundColor Cyan
Write-Host ""

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

function Test-ShouldIncludeContent {
    param([System.IO.FileInfo]$File)

    if ($IncludeContentFilenames -contains $File.Name) { return $true }
    if ($IncludeContentExtensions -contains $File.Extension.ToLower()) { return $true }
    return $false
}

function Test-ShouldExcludeDir {
    param([string]$DirName)
    return $ExcludeDirs -contains $DirName
}

function Get-RelativePath {
    param([string]$FullPath, [string]$BasePath)
    $base = $BasePath.TrimEnd('\', '/') + [System.IO.Path]::DirectorySeparatorChar
    if ($FullPath.StartsWith($base, [System.StringComparison]::OrdinalIgnoreCase)) {
        return $FullPath.Substring($base.Length)
    }
    return $FullPath
}

# ---------------------------------------------------------------------------
# Tree generation
# ---------------------------------------------------------------------------

function Write-Tree {
    param(
        [string]$DirPath,
        [string]$Prefix,
        [System.Collections.Generic.List[string]]$Lines,
        [int]$Depth = 0
    )

    if ($Depth -eq 0) {
        $Lines.Add((Split-Path $DirPath -Leaf) + '/')
    }

    $entries = @()
    try {
        $entries = Get-ChildItem -LiteralPath $DirPath -Force -ErrorAction Stop |
                   Where-Object { -not ($_.PSIsContainer -and (Test-ShouldExcludeDir $_.Name)) } |
                   Sort-Object @{Expression={-not $_.PSIsContainer}}, Name
    } catch {
        $Lines.Add("$Prefix[unreadable: $($_.Exception.Message)]")
        return
    }

    for ($i = 0; $i -lt $entries.Count; $i++) {
        $entry = $entries[$i]
        $isLast = ($i -eq $entries.Count - 1)
        $connector = if ($isLast) { '└── ' } else { '├── ' }
        $extension = if ($isLast) { '    ' } else { '│   ' }

        if ($entry.PSIsContainer) {
            $Lines.Add("$Prefix$connector$($entry.Name)/")
            Write-Tree -DirPath $entry.FullName -Prefix "$Prefix$extension" -Lines $Lines -Depth ($Depth + 1)
        } else {
            $sizeStr = if ($entry.Length -lt 1KB) {
                "$($entry.Length) B"
            } elseif ($entry.Length -lt 1MB) {
                "{0:N1} KB" -f ($entry.Length / 1KB)
            } else {
                "{0:N1} MB" -f ($entry.Length / 1MB)
            }
            $Lines.Add("$Prefix$connector$($entry.Name)  ($sizeStr)")
        }
    }
}

# ---------------------------------------------------------------------------
# File collection (flat list for content dump)
# ---------------------------------------------------------------------------

function Get-IncludedFiles {
    param([string]$DirPath)

    $results = New-Object System.Collections.Generic.List[System.IO.FileInfo]
    $stack = New-Object System.Collections.Generic.Stack[string]
    $stack.Push($DirPath)

    while ($stack.Count -gt 0) {
        $current = $stack.Pop()
        try {
            $children = Get-ChildItem -LiteralPath $current -Force -ErrorAction Stop
        } catch {
            continue
        }
        foreach ($child in $children) {
            if ($child.PSIsContainer) {
                if (-not (Test-ShouldExcludeDir $child.Name)) {
                    $stack.Push($child.FullName)
                }
            } else {
                if (Test-ShouldIncludeContent $child) {
                    $results.Add($child)
                }
            }
        }
    }
    return $results | Sort-Object FullName
}

# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

$output = New-Object System.Collections.Generic.List[string]

$output.Add("=" * 80)
$output.Add("GeoTectonic Project Snapshot")
$output.Add("Generated: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')")
$output.Add("Root:      $RootPath")
$output.Add("=" * 80)
$output.Add("")
$output.Add("DIRECTORY TREE")
$output.Add("-" * 80)
$output.Add("")

$treeLines = New-Object System.Collections.Generic.List[string]
Write-Tree -DirPath $RootPath -Prefix "" -Lines $treeLines
foreach ($line in $treeLines) { $output.Add($line) }

$output.Add("")
$output.Add("")
$output.Add("FILE CONTENTS")
$output.Add("-" * 80)
$output.Add("")

Write-Host "Collecting file list..." -ForegroundColor Yellow
$files = Get-IncludedFiles -DirPath $RootPath
Write-Host "Found $($files.Count) files to dump." -ForegroundColor Yellow
Write-Host ""

$count = 0
foreach ($file in $files) {
    $count++
    $relPath = Get-RelativePath -FullPath $file.FullName -BasePath $RootPath
    Write-Progress -Activity "Dumping files" -Status $relPath -PercentComplete (($count / $files.Count) * 100)

    $output.Add("")
    $output.Add("=" * 80)
    $output.Add("FILE: $relPath")
    $output.Add("=" * 80)

    try {
        $content = Get-Content -LiteralPath $file.FullName -ErrorAction Stop -Encoding UTF8
        if ($null -eq $content) { $content = @() }
        if ($content -isnot [array]) { $content = @($content) }

        if ($content.Count -gt $MaxLinesPerFile) {
            for ($i = 0; $i -lt $MaxLinesPerFile; $i++) {
                $output.Add($content[$i])
            }
            $output.Add("")
            $output.Add("[... truncated $($content.Count - $MaxLinesPerFile) more lines ...]")
        } else {
            foreach ($line in $content) { $output.Add($line) }
        }
    } catch {
        $output.Add("[unreadable: $($_.Exception.Message)]")
    }
}

Write-Progress -Activity "Dumping files" -Completed

$output.Add("")
$output.Add("=" * 80)
$output.Add("END OF SNAPSHOT")
$output.Add("=" * 80)

# Write with UTF-8 (no BOM) for clean reading
$utf8NoBom = New-Object System.Text.UTF8Encoding $false
[System.IO.File]::WriteAllLines($OutputPath, $output, $utf8NoBom)

$sizeKB = "{0:N1}" -f ((Get-Item -LiteralPath $OutputPath).Length / 1KB)
Write-Host ""
Write-Host "Done." -ForegroundColor Green
Write-Host "Wrote $($output.Count) lines / $sizeKB KB to: $OutputPath" -ForegroundColor Green
