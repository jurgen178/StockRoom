
<#
$data = @(
    # Anzahl Anzeigeelemente für font_ bei screenWidthDp
    @(font_0.85, font_1, font_1.15, font_1.3, screenWidthDp),

screenWidthDp Pixel 3a
portrait:  462 392 352 320
landscape: 901 759 676 609

screenWidthDp Surface Duo
portrait:   635 540 469 415
landscape:  847 720 626 553
#>

$fontScale = @(0.85, 1, 1.15, 1.3)

function printScale($data, [int]$scale) {
    foreach ($row in $data) {
        [int]$density = $row[-1]
        [string]$line = ""
        foreach ($font in $fontScale) {
            $a = [int]($density / ($scale * $font) + 0.5)
            $line += "$a, "
        }
        $line += "   // $density"
        $line
    }
}

function testScale($data, [int]$scale) {
    foreach ($row in $data) {
        [int]$density = $row[-1]
        for ([int]$column = 0; $column -lt $row.Length - 1; $column++) {
            [int]$zielWert = $row[$column]
            [int]$a = [int]($density / ($scale * $fontScale[$column]) + 0.5)
            if ($zielWert -ne $a) {
                return $false
            }
        }
    }

    $true
}

function getScale($data) {
    [int]$minScale = 0
    [int]$maxScale = 1001
    1..1000 | % {
        [bool]$test = testScale $data $_
        if ($minScale -eq 0 -and $test) {
            $minScale = $_
        }
        if ($minScale -ne 0 -and $maxScale -eq 1001 -and -not $test) {
            $maxScale = $_ - 1
        }
   }

    if ($minScale -eq 0 -or $maxScale -eq 1001) {
        Write-Host("ungültig")
        0   
    }
    else {
        Write-Host("$minScale .. $maxScale")
        [int](($minScale + $maxScale) / 2)    
    }
}

<#
grid view

screenWidthDp Pixel 3a
portrait:  462 392 352 320
landscape: 901 759 676 609

screenWidthDp Surface Duo
portrait:   635 540 469 415
landscape:  847 720 626 553
#>
$data1 = @(
    # Surface Duo
    @(7, 6, 5, 5, 847),

    # Pixel 3a
    @(4, 3, 3, 3, 462),
    @(3, 3, 3, 2, 392),
    @(3, 3, 2, 2, 352),
    @(3, 3, 2, 2, 320),
    @(7, 6, 6, 5, 901),
    @(6, 5, 5, 4, 759),
    @(6, 5, 4, 4, 676),
    @(5, 4, 4, 4, 609)
)

"`ngrid view"
$scale = getScale $data1
$scale
#printScale $scale
#printScale $data1 156

<#
large view

screenWidthDp Pixel 3a
portrait:  462 392 352 320
landscape: 901 759 676 609

screenWidthDp Surface Duo
portrait:   635 540 469 415
landscape:  847 720 626 553
#>
$data2 = @(
    # Surface Duo
    @(2, 2, 2, 1, 635),
    @(3, 2, 2, 2, 847),

    # Pixel 3a
    @(2, 1, 1, 1, 462),
    @(1, 1, 1, 1, 392),
    @(1, 1, 1, 1, 352),
    @(1, 1, 1, 1, 320),
    @(3, 2, 2, 2, 901),
    @(2, 2, 2, 2, 759),
    @(2, 2, 2, 2, 676),
    @(2, 2, 2, 1, 609)
)

"`nlarge view"
$scale = getScale $data2
$scale
#printScale $scale
#printScale $data2 494

<#
onlinedata in summary view

screenWidthDp Pixel 3a
portrait:  462 392 352 320
landscape: 901 759 676 609

screenWidthDp Surface Duo
portrait:   635 540 469 415
landscape:  847 720 626 553
#>
$data3 = @(
    # Surface Duo
    @(4, 3, 3, 3, 847),

    # Pixel 3a
    @(2, 2, 2, 2, 462),
    @(2, 2, 2, 2, 392),
    @(2, 2, 2, 1, 352),
    @(2, 2, 1, 1, 320),
    @(4, 4, 3, 3, 901),
    @(3, 3, 3, 2, 759),
    @(3, 3, 2, 2, 676),
    @(3, 3, 2, 2, 609)
)

"`nonlinedata in summary view"
$scale = getScale $data3
$scale
#printScale $scale
#printScale $data3 299

