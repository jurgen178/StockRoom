

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
        for ([int]$column = 0; $column -lt $row.Length - 2; $column++) {
            [int]$zielWert = $row[$column]
            [int]$a = [int]($density / ($scale * $fontScale[$column]) + 0.5)
            if($zielWert -ne $a)
            {
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
        if($minScale -eq 0 -and $test)
        {
            $minScale = $_
        }
        if($minScale -ne 0 -and $maxScale -eq 1001 -and -not $test)
        {
            $maxScale = $_ - 1
        }
    }

    Write-Host("$minScale .. $maxScale")

    [int](($minScale + $maxScale) / 2)
}

<#
7, 6, 5, 5,    // 847
4, 3, 3, 3,    // 462
3, 3, 3, 2,    // 392
3, 3, 2, 2,    // 352
3, 3, 2, 2,    // 320
7, 6, 6, 5,    // 901
6, 5, 5, 4,    // 759
6, 5, 4, 4,    // 676
5, 4, 4, 4,    // 609
#>
$data1 = @(
    @(7, 6, 5, 5, 847),
    @(4, 3, 3, 3, 462),
    @(3, 3, 3, 2, 392),
    @(3, 3, 2, 2, 352),
    @(3, 3, 2, 2, 320),
    @(7, 6, 6, 5, 901),
    @(6, 5, 5, 4, 759),
    @(6, 5, 4, 4, 676),
    @(5, 4, 4, 4, 609)
)

$scale = getScale $data1
$scale
#printScale($scale) grid
#printScale $data1 155 grid

<#
3, 2, 2, 2,    // 847
2, 1, 1, 1,    // 462
1, 1, 1, 1,    // 392
1, 1, 1, 1,    // 352
1, 1, 1, 1,    // 320
3, 2, 2, 2,    // 901
2, 2, 2, 2,    // 759
2, 2, 2, 2,    // 676
2, 2, 2, 1,    // 609
#>
$data2 = @(
    @(3, 2, 2, 2, 847),
    @(2, 1, 1, 1, 462),
    @(1, 1, 1, 1, 392),
    @(1, 1, 1, 1, 352),
    @(1, 1, 1, 1, 320),
    @(3, 2, 2, 2, 901),
    @(2, 2, 2, 2, 759),
    @(2, 2, 2, 2, 676),
    @(2, 2, 2, 1, 609)
)

$scale = getScale $data2
$scale
#printScale($scale) list
#printScale(480) list

<#
4, 3, 3, 3,    // 847
2, 2, 2, 2,    // 462
2, 2, 2, 2,    // 392
2, 2, 2, 1,    // 352
2, 2, 1, 1,    // 320
4, 4, 3, 3,    // 901
3, 3, 3, 2,    // 759
3, 3, 2, 2,    // 676
3, 3, 2, 2,    // 609
#>
$data3 = @(
    @(4, 3, 3, 3, 847),
    @(2, 2, 2, 2, 462),
    @(2, 2, 2, 2, 392),
    @(2, 2, 2, 1, 352),
    @(2, 2, 1, 1, 320),
    @(4, 4, 3, 3, 901),
    @(3, 3, 3, 2, 759),
    @(3, 3, 2, 2, 676),
    @(3, 3, 2, 2, 609)
)

$scale = getScale $data3
$scale
#printScale($scale) onlinedata
#printScale(300) onlinedata

