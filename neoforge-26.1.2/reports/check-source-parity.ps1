$ErrorActionPreference = 'Stop'
$targetRoot = Split-Path $PSScriptRoot -Parent
$originalRoot = Split-Path $targetRoot -Parent
function Read-Source($root, $name) {
    [IO.File]::ReadAllText((Join-Path $root ('src/main/java/dev/medievalstatus/' + $name))).Replace("`r`n", "`n")
}
function Assert-Same($label, $original, $target) {
    if ($original -cne $target) { throw "Source preservation failed: $label" }
    "PASS $label"
}
& {
    foreach ($name in @('core/Progression.java', 'Upgrade.java', 'client/ClientState.java')) {
        Assert-Same $name (Read-Source $originalRoot $name) (Read-Source $targetRoot $name)
    }
    $config = (Read-Source $originalRoot 'Config.java').Replace('net.minecraftforge.common.ForgeConfigSpec', 'net.neoforged.neoforge.common.ModConfigSpec').Replace('ForgeConfigSpec', 'ModConfigSpec')
    Assert-Same 'All configuration keys, defaults, ranges and ordering (loader type mapping only)' $config (Read-Source $targetRoot 'Config.java')
    $layout = (Read-Source $originalRoot 'client/UiLayout.java').Replace('net.minecraftforge.fml.loading.FMLPaths', 'net.neoforged.fml.loading.FMLPaths')
    Assert-Same 'UI layout settings, defaults and persistence (loader path import only)' $layout (Read-Source $targetRoot 'client/UiLayout.java')
    $oldBonus = Read-Source $originalRoot 'Bonuses.java'
    $newBonus = Read-Source $targetRoot 'Bonuses.java'
    $start = '    public static double skill('
    $end = '    public void apply('
    Assert-Same 'All passive/attribute formulas, caps, stacking, cache and precision' ($oldBonus.Substring($oldBonus.IndexOf($start), $oldBonus.IndexOf($end)-$oldBonus.IndexOf($start))) ($newBonus.Substring($newBonus.IndexOf($start), $newBonus.IndexOf($end)-$newBonus.IndexOf($start)))
    $write = '    public CompoundTag write()'
    Assert-Same 'Passive snapshot values, labels and ordering' ($oldBonus.Substring($oldBonus.IndexOf($write))) ($newBonus.Substring($newBonus.IndexOf($write)))
    $pattern = '"(?:\\.|[^"\\])*"'
    $oldStrings = [regex]::Matches((Read-Source $originalRoot 'client/StatusScreen.java'), $pattern).Value -join "`n"
    $newStrings = [regex]::Matches((Read-Source $targetRoot 'client/StatusScreen.java'), $pattern).Value -join "`n"
    Assert-Same 'Every status-screen string literal and its order' $oldStrings $newStrings
} | Tee-Object -FilePath (Join-Path $PSScriptRoot 'source-parity.txt')
