# Changelog

## 0.1.4: current fork updates

### Loot probability fix

All loot chances now use one per-thousand scale, including values at or below 1. `1` means 0.1%, `0.5` means 0.05%, and `1000` means 100%. The former raw-probability compatibility branch is removed. Existing defaults retain their chances; legacy raw-probability configurations must multiply their old values by 1000 to preserve the intended rate. Added boundary and fractional-chance regression coverage.

### Gameplay and saves

Anvil upgrades require bow left and component middle, consume one component, and support simultaneous renaming. Removed the right-click upgrade shortcut. Winterfang counts finite ammo across stacks, handles supported Infinity use, and requires one arrow for ready rune shots. All seven special projectile classes preserve their upgrade and ability state, including normal and spectral Winterfang homing arrows.

Retuned Everbloom Pain/Bounty, Petalwind normal/Bounty, and Bubbleveil Bounty damage. Increased Everbloom tree spacing. Bubbleveil Chaos travels four blocks farther and has reduced high-Frame knockback. Everbloom Pain uses direct magic pulses to avoid Wither overlap.

### Tooltips and documentation

Restored compact String/Frame bonuses, combined contextual upgrade previews, clearer combat stats, single-decimal formatting, named page headings, and colored plus navigation with the rebound key. Removed duplicate headings and attribute lore. Bow names use their individual colors; rune names no longer say “etching”. Split renderer hooks by loader and check them against Simply Tooltips 0.1.5.

Replaced the README with a concise overview and linked guides covering installation, all bow/rune builds, upgrade costs, all 212 validated settings, loot, commands, development, persistence, and manual tests.

[Earlier revision notes](docs/historical-changelog.md)
