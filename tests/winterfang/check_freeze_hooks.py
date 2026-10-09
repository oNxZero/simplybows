"""Inspect packaged freeze injection selectors against the matching mapped Minecraft jar.
Usage: python3 check_freeze_hooks.py <production-bows.jar> <mapped-minecraft.jar>
Checks targets and descriptors, not an actual Mixin application or gameplay session.
"""
import json
import zipfile
import re
import subprocess
import sys

def inspect(jar, name, *flags):
    return subprocess.check_output(['javap', '-p', *flags, '-classpath', jar, name], text=True)

with zipfile.ZipFile(sys.argv[1]) as jar:
    config = json.loads(jar.read('simplybows.mixins.json'))
    refmap = json.loads(jar.read(config['refmap']))['mappings'] if 'refmap' in config else {}
count = 0
for simple in ['ServerWorldMixin', 'ServerPlayerEntityMixin', 'FrozenPlayerNetworkMixin', 'LivingEntityMixin']:
    mix = inspect(sys.argv[1], 'net.sweenus.simplybows.mixin.' + simple, '-v')
    target = re.search(r'org.spongepowered.asm.mixin.Mixin\(\s*value=\[class L([^;]+);\]', mix)
    assert target, simple + ': missing target class'
    methods = inspect(sys.argv[2], target[1].replace('/', '.'), '-s')
    for annotation in re.findall(r'org.spongepowered.asm.mixin.injection.Inject\(\s*method=\[([^\]]+)\]', mix):
        for selector in re.findall(r'"([^"]+)"', annotation):
            lookup = refmap.get('net/sweenus/simplybows/mixin/' + simple, {}).get(selector)
            if lookup and target[1].startswith('net/minecraft/class_'):
                selector = lookup.split(';', 1)[1]
            name = selector.split('(')[0]
            blocks = re.split(r'\n  (?=(?:public|protected|private) )', methods)
            matches = [b for b in blocks if re.search(r'\b' + re.escape(name) + r'\(', b)]
            assert matches, simple + ': target not found: ' + selector
            if '(' in selector:
                descriptor = '(' + selector.split('(', 1)[1]
                assert any('descriptor: ' + descriptor in b for b in matches), selector
            count += 1
print('PASS:', count, 'freeze and damage hook selectors resolve against the matching loader Minecraft classes')
