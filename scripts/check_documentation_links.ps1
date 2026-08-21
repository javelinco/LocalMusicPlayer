$ErrorActionPreference = "Stop"
$root = (Get-Location).Path
$documents = @("README.md", "CHANGELOG.md") + @(
    Get-ChildItem -LiteralPath (Join-Path $root "docs") -Recurse -Filter *.md |
        ForEach-Object { [System.IO.Path]::GetRelativePath($root, $_.FullName) }
)
$missing = [System.Collections.Generic.List[string]]::new()
foreach ($relative in $documents) {
    $full = Join-Path $root $relative
    $text = Get-Content -Raw -LiteralPath $full
    foreach ($match in [regex]::Matches($text, '\[[^\]]+\]\(([^)]+)\)')) {
        $target = $match.Groups[1].Value.Trim().Trim('<', '>').Split('#')[0]
        if ([string]::IsNullOrWhiteSpace($target) -or $target -match '^[a-z]+://') { continue }
        $resolved = [System.IO.Path]::GetFullPath((Join-Path (Split-Path -Parent $full) $target))
        if (-not (Test-Path -LiteralPath $resolved)) { $missing.Add("$relative -> $target") }
    }
}
if ($missing.Count -gt 0) { throw "Broken documentation links:`n$($missing -join "`n")" }
Write-Host "Documentation link gate passed for $($documents.Count) Markdown files."
