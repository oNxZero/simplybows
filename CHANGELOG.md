# Changes in this fork

Fork of [Sweenus/simplybows](https://github.com/Sweenus/simplybows). Item ids of the bows that remain are unchanged, so existing items stay valid. Echo and Starweave items do not.

Install `neoforge/build/libs/SimplyBows-neoforge-0.1.4.jar` on the server and on every client. The old Modrinth jar cannot run beside it. Delete `config/simplybows/config.toml` once after updating so renamed sections are written. A live file keeps old numbers: set `[winterfang] painDamageMultiplier` to `1.2` if it still says `1.5`, and leave `[everbloom] friendlyHeal` at `2.0`.

## Everbloom

Everbloom (`vine_bow`) is a healing weapon.

- A full draw plants the flower field. A short draw is still a healing arrow and does not plant flowers.
- Players, animals, and iron golems take no arrow damage. A direct hit heals them for 1 heart at Frame 0 and 3.5 hearts at Frame 5.
- The field heals only on the visible flower patch. Without Grace, only the one with the lowest health is healed. With Grace, players, animals, and iron golems in the patch are healed, on a cooldown equal to the field.
- Field regen is dripped across the second and overlapping fields do not stack. Frame 0 is half a heart per second, Frame 5 is two hearts per second, when `friendlyHeal` is `2.0`.
- Only monsters take field damage, in the larger field radius.
- Enchanted String grows the flower patch and arrow speed. The tooltip shows that patch radius, not the monster radius.
- The patch fills the center and grows outward, then sinks back in from the outside. Grace's cherry tree does the same.

## Other bows

- Winterfang Pain damage multiplier default is `1.2`. Focus and every-shot behavior are unchanged. The Chaos wall keeps its cooldown.
- Buzzkill's special, including poison and Grace bees, only fires on a full draw. Grace bees have a short cooldown. Chaos honey already had one.
- Bubbleveil's special only fires on a full draw.
- Petalwind keeps one storm at a time. Grace still seeks allies inside it. Pain stays the area storm.
- Tremorstrike will not replant its spike field until a lockout ends. Grace launches allies once per field.
- Winterfang, Petalwind, and Tremorstrike already required a full draw.

## Removed

- Starweave (Cosmic) is removed from items, loot, tooltips, models, and config.
- Echo is removed the same way. Koi visuals stay; they belong to Petalwind Chaos.

## Upgrades and tooltips

Upgrades are stored in the `simplybows:upgrades` component so they survive mods that replace `minecraft:custom_data` on death. Old upgrade data still on an item is copied into that component. Data already wiped by a previous death cannot be restored.

Ability cooldowns draw the vanilla hotbar overlay and do not lock the bow. Tooltips stay short: the ability line, one rune line, and the Alt row shows the gain per String or Frame level.
