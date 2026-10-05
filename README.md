# Create: Stock Keeper Bookmarks

A client-side [Create](https://www.curseforge.com/minecraft/mc-mods/create) addon.

Create's Stock Keeper has a single "Package Address" field, and it keeps whatever was typed
into it last. On a server that includes what somebody else typed. Nothing checks the address
before the order ships, so a stale one sends your package to the wrong machine or to nowhere
at all.

This mod puts a column of saved destinations next to the panel. You click the one you want,
and the text field starts empty every time the screen opens.

## Download

**[CurseForge](https://www.curseforge.com/minecraft/mc-mods/create-stock-keeper-bookmarks)**

| Minecraft | Loader | Create |
| --- | --- | --- |
| 1.21.1 | NeoForge | 6.0.x |
| 1.20.1 | Forge or NeoForge | 6.0.7+ |

The 1.20.1 jar runs on Forge and NeoForge both, from the same file. Install it like any other
mod. Both builds are declared for Create 6.0 only, so a future Create 6.1 will need an update
here first.

Nothing goes on the server. It also works on servers that don't have it installed.

## What it does

- A column of bookmarks beside the Stock Keeper panel. One click picks that destination.
- Every Stock Keeper keeps its own list. Your workshop keeper and your smelter keeper don't
  share bookmarks, even on the same logistics network.
- The address field is empty on every open. This is the actual fix; the bookmarks are what
  make it painless.
- Type an address and click **+** to save it. Right-click a bookmark to remove it, drag to
  reorder. The list scrolls once it gets long.
- A small name-tag button above the list cycles through label layouts, hides the list, and
  toggles whether the item search box is focused when you open the screen.
- The free-text field is still there. Glob and `regex:` addresses and one-off destinations
  still need it.
- Sending with the field blank still works. A small setup with one unaddressed destination
  is a normal way to play, and the mod never blocks a send.

Full controls, keybinds and config options are in the README for your version:
**[1.21.1](../../tree/1.21.1-neoforge)** · **[1.20.1](../../tree/1.20.1-forge)**

## Source

This branch holds no code. Each Minecraft version is on its own branch, since the two loaders
need different APIs and different build setups.

- [`1.21.1-neoforge`](../../tree/1.21.1-neoforge) — JDK 21
- [`1.20.1-forge`](../../tree/1.20.1-forge) — JDK 17

Either one builds with `./gradlew build`. Each has a `DEVELOPMENT.md` covering how the mod
hooks into Create's screen and which Create versions were tested.

## License

[LGPL-3.0-or-later](COPYING.LESSER), with the GPL text it builds on in [`COPYING`](COPYING).

Use it, ship it in a modpack, fork it. If you distribute a modified version, that version stays
open under the same licence. Mods that just depend on this one are unaffected. If you want to do
something the licence doesn't allow, ask.

Create itself is separately licensed. This mod ships none of Create's files; it reads the Stock
Keeper's texture at runtime from the copy you already have installed.
