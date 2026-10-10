"""Validate production packaging of area casts and persisted, bounded seeking attacks.
Usage: python3 check_area_projectiles.py <production-bows.jar>
This is a bytecode check; it does not replace gameplay testing.
"""
import subprocess, sys

def inspect(name):
    return subprocess.check_output(['javap','-p','-c','-classpath',sys.argv[1],
        'net.sweenus.simplybows.'+name],text=True)
arrow=inspect('entity.BubblePainArrowEntity')
assert arrow.count('RuneEffectManager.cast:')==1, 'cloud must have one guarded impact cast path'
assert arrow.count('Method spawnImpact:')==2, 'both block and entity impacts must create the area'
effect=inspect('entity.RuneEffectEntity')
for token in ('PayloadDamage','LastTargetX','LastTargetY','LastTargetZ','HitCount','FinishAt'):
    assert token in effect, f'missing persistent projectile/formation state: {token}'
assert 'tickSeekingProjectile' in effect and 'launchVolley' in effect
assert any(name in effect for name in ('RaycastContext','ClipContext','class_3959')), 'seeking shots must check terrain'
assert 'bipush        30' in effect, 'seeking shots must have a flight deadline'
assert 'double 16.0d' in effect, 'seeking shots must break on a target teleport'
renderer=inspect('client.renderer.RuneEffectEntityRenderer')
assert 'RuneWaterRenderer.box:' in renderer, '3D water drops must be packaged'
from pathlib import Path
source=(Path(__file__).resolve().parents[2]/'common/src/main/java/net/sweenus/simplybows/client/renderer/RuneEffectEntityRenderer.java').read_text()
assert 'WOOL' not in source, 'cloud must not contain wool geometry'
print('PASS: block/entity area casts, persisted seeking state, terrain/deadline/teleport limits and water geometry without wool')
