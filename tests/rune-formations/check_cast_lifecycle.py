"""Check the packaged Chaos launch/impact contract that previously rejected player casts.
Usage: python3 check_cast_lifecycle.py <production-bows.jar>
This inspects bytecode; it does not start Minecraft.
"""
import re
import subprocess
import sys


def inspect(name):
    return subprocess.check_output(['javap', '-p', '-c', '-classpath', sys.argv[1],
                                    'net.sweenus.simplybows.' + name], text=True)


bow = inspect('item.unique.EarthBowItem')
for name in ('BubbleBowItem', 'BeeBowItem', 'EarthBowItem', 'IceBowItem', 'BlossomBowItem', 'VineBowItem'):
    code = inspect('item.unique.' + name)
    assert 'simplybows$isForcingVanillaArrow' in code, name + ' ignores partial-draw vanilla fallback'
    factory = code[code.rfind('\n  protected ', 0, code.index('simplybows$isForcingVanillaArrow')):]
    assert 'simplybows$isForcingVanillaArrow' in factory, name + ' ignores partial-draw vanilla fallback'
    guard = factory.index('simplybows$isForcingVanillaArrow')
    constructor = re.search(r'new\s+[^\n]+// class net/sweenus/simplybows/entity/', factory)
    assert constructor and guard < constructor.start(), name + ' creates an ability projectile before fallback'
print('PASS: all six bow projectile factories respect the partial-draw vanilla fallback')
assert 'simplybows$startAbilityItemCooldown' not in bow, 'Chaos still starts a hidden pre-impact cooldown'
assert bow.count('RuneUseCooldown.start:') == 1, 'launch must reserve the cooldown exactly once'
shot = re.search(r'invokevirtual[^\n]+// Method [^:]+:[^\n]+Ljava/util/List;FFZ', bow)
assert shot and shot.start() < bow.index('RuneUseCooldown.start:'), 'reserve after arrow creation succeeds'
arrow = inspect('entity.EarthArrowEntity')
lines = arrow.splitlines()
casts = [i for i, line in enumerate(lines) if 'RuneEffectManager.cast:' in line]
assert len(casts) == 1, 'expected one Chaos impact cast path'
assert re.search(r'iconst_0\s*$', lines[casts[0] - 1]), 'impact must consume its token without reserving again'
assert 'ChaosSunderOnImpact' in arrow, 'delivery token must survive unloading'
effect = inspect('entity.RuneEffectEntity')
assert 'FinishAt' in effect, 'finish state must persist across reload'
assert 'client/renderer/RuneWaterRenderer' in inspect('client.renderer.RuneEffectEntityRenderer'), 'vortex must use water rendering'
print('PASS: one Chaos launch reservation, token-consuming impact, persisted finish state and water renderer packaged')
