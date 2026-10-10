"""Check production injection selectors against a real Simply Tooltips jar.

Usage: python3 tests/tooltip-navigation/check_renderer_target.py <bows.jar> <tooltips.jar>
Requires javap on PATH. This checks bytecode targets, not Minecraft/Mixin execution.
"""
import re
import subprocess
import sys


def javap(jar, class_name, *flags):
    return subprocess.check_output(
        ["javap", "-p", *flags, "-classpath", jar, class_name], text=True
    )


mix = javap(sys.argv[1], "net.sweenus.simplybows.mixin.client.TooltipRendererMixin", "-v")
renderer = javap(sys.argv[2], "net.sweenus.simplytooltips.client.render.TooltipRenderer", "-s", "-c")
selectors = re.findall(r'method=\["(render\([^"\n]+)"\]', mix)
assert len(selectors) == 5, "All renderer injections must use explicit overload selectors"
assert len(set(selectors)) == 1, "All renderer injections must target the same overload"
descriptor = selectors[0][len("render"):]
methods = re.split(r"\n  (?=(?:public|private|protected) )", renderer)
matches = [m for m in methods if " render(" in m and "descriptor: " + descriptor + "\n" in m]
assert len(matches) == 1, "Packaged selector does not uniquely match the installed renderer"
target = "net/sweenus/simplytooltips/client/TooltipNavigationConfig.tooltipTabs:()Z"
calls = [line for line in matches[0].splitlines() if "invokestatic" in line and target in line]
assert len(calls) == 2, "Expected both native navigation checks in the selected overload"
assert "ordinal=0" in mix and "argsOnly=true" in mix
print("PASS: both packaged injections select the full renderer; both native tab calls exist")

assert "TooltipPainter.drawFooterDots:" in matches[0]
assert ("GuiGraphics.drawString:(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)I" in matches[0]
        or "class_332.method_51439:(Lnet/minecraft/class_327;Lnet/minecraft/class_2561;IIIZ)I" in matches[0])
verbose = javap(sys.argv[2], "net.sweenus.simplytooltips.client.render.TooltipRenderer", "-v")
assert re.search(r"\blineHeight\s+I\b", verbose), "Rune spacing requires the renderer lineHeight local"
print("PASS: custom footer and compact rune-row call sites exist in 0.1.5")

for target in re.findall(r'target="([^"\n]+)"', mix):
    owner, method = target[1:].split(";", 1)
    name, descriptor = method.split("(", 1)
    instruction = owner + "." + name + ":(" + descriptor
    assert instruction in matches[0], "Packaged invocation target does not exist: " + instruction
print("PASS: every packaged invocation target matches the installed renderer bytecode")
