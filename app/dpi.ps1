

$screenWidthDp = @(540, 462, 392, 352, 320, 901, 759, 676, 609)
$densityDpi = @(375, 440, 490, 540)
$fontScale = @(0.85, 1, 1.15, 1.3)
$scale = 155

    foreach ($density in $screenWidthDp) {
        [string]$line = ""
        foreach ($font in $fontScale) {
            $a = [int]($density / ($scale * $font) + 0.5)
            $line += "$a, "
        }
        $line += "   // $density"
        $line
    }
