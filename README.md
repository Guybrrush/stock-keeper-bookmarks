# Create: Stock Keeper Bookmarks

Client-side Create addon for Minecraft 1.20.1 / Forge.

> **This is the 1.20.1 branch.** For Minecraft 1.21.1 see
> [`1.21.1-neoforge`](../../tree/1.21.1-neoforge); [`main`](../../tree/main) indexes every
> supported version.

This mod puts a column of saved destinations next to the panel. You click the one you want,
and the selected items are routed to the matching address.

## What it does

- A column of bookmarks beside the Stock Keeper panel. One click picks that destination.
- Every Stock Keeper keeps its own list. Your workshop keeper and your smelter keeper don't
  share bookmarks, even on the same logistics network.
- The address field is empty on every open.
- Type an address and click **+** to save it. Right-click a bookmark to remove it, drag to
  reorder. The list scrolls once it gets long.
- A small name-tag button above the list cycles through label layouts, hides the list, and
  toggles whether the item search box is focused when you open the screen.
- Sending with the field blank still works. A small setup with one unaddressed destination
  is a normal way to play, and the mod never blocks a send.

## Config

`config/stockkeeperbookmarks-client.toml`

| Key | Default | Meaning |
| --- | --- | --- |
| `addresses` | _(empty)_ | Starting bookmarks for a keeper you have not customised yet |
| `clickToSend` | `true` | Click sends immediately; `false` = select, then press Send |
| `clearAddressOnOpen` | `true` | The safeguard. `false` restores Create's sticky behaviour |
| `buttonWidth` | `72` | Bookmark width in pixels. In `WIDE` it is the minimum, not the fixed width |
| `displayMode` | `FIT` | Label layout — see below. Cycled in-game from the name-tag button |
| `collapsed` | `false` | Whether the list is hidden. Toggled by Shift+clicking the name-tag button |
| `autoFocusSearch` | `true` | Focus the item search box on open. Toggled by Ctrl+clicking that button |

Per-keeper bookmarks live in `config/stockkeeperbookmarks-bookmarks.json`, keyed by world,
dimension and block position. A keeper with no entry shows the `addresses` list above;
the first pin, removal or reorder promotes it to its own entry.

### Display modes

A small **name-tag button** sits above the bookmarks, aligned to the panel edge. Left-click
cycles the layout, right-click steps back, Shift+click hides or shows the list, and
Ctrl+click toggles whether the search box is focused when the screen opens. Hovering it
explains all of this in a tooltip, and it is drawn even when a keeper has no bookmarks yet.

| Layout | What a too-long address does |
| --- | --- |
| `FIT` | Shrinks toward a legibility floor, then takes an ellipsis |
| `ELLIPSED` | Never shrinks — cut at full size |
| `MINIMAL` | Cut at full size, in a column a few characters wide |
| `WIDE` | The column grows instead, as far as the space to its left allows |
| `WRAP` | Runs onto a second line, in taller rows |

Each has a `_TOOLTIPS` variant that names the full address on hover whenever the label had
to be cut or shrunk. Collapsing is a separate flag rather than a mode, so hiding the list
and showing it again restores the layout you were using.

## Controls

| Action | Result |
| --- | --- |
| Left-click a bookmark | Select the destination (and send, if `clickToSend`) |
| Right-click a bookmark | Remove it |
| Drag a bookmark | Reorder; auto-scrolls at the viewport edges |
| Scroll wheel over the column | Scroll the list |
| **+** in the footer | Pin whatever is typed in the address field |
| **=** key (rebindable) | The same pin action, from the keyboard |
| Left-click the name-tag button | Next display mode |
| Right-click the name-tag button | Previous display mode |
| Shift + left-click it | Hide or show the bookmark list |
| Ctrl + left-click it | Toggle search-box auto-focus |

A low-pitched click from **+** means nothing was pinned: the field was blank, or that
address is already bookmarked.

Create already sources address autocomplete from **nearby Clipboards**. Put a clipboard
listing your addresses near the Stock Ticker and the field will suggest them.

The pin key is an ordinary keybind, listed under Options → Controls. It reads **`=`** because
that is the physical key you press to type `+`, and it pins whether or not Shift is held. It
works while the address field has focus, so the bound character can't be typed into an
address. Rebind it if you need that character in a destination name. It is not swallowed
while the item search box has focus.

## Compatibility

Built against Create `6.0.8-291`, Minecraft 1.20.1, Forge 47.4.23, and confirmed working
in-game on Create `6.0.7` and `6.0.8`.

**Create 6.0.7 is the minimum**, found by launching against each release in turn rather than
assumed. Earlier releases refuse to load rather than crashing;
[DEVELOPMENT.md](DEVELOPMENT.md) records which ones fail and how.

The same jar runs on **Forge and NeoForge alike** on 1.20.1, confirmed by testing both.
NeoForge's 1.20.1 line is a compatibility backport that still uses Forge's namespace, which
is what makes that possible. From 1.20.2 onward the two diverge, hence the separate branch
for 1.21.1.

It *should* work on any Minecraft 1.20.1 server running Create, including servers that don't
have it installed. That is the case the client-side claim rests on, and it has been played
that way. One server is not a guarantee though: a conflicting mod or an unusual setup could
still break it.

## Implementation

Everything is client-side, through one mixin into `StockKeeperRequestScreen`. Create's menu
hands the client the real `StockTickerBlockEntity`, so telling one Stock Keeper from another
needs no server component.

See **[DEVELOPMENT.md](DEVELOPMENT.md)** for mixin injection points, the version-coupled
constants read out of Create's sprite sheet and bytecode, why the buttons are not widgets,
and what was deliberately left alone. It also covers the build setup; this branch needs
**JDK 17**, where 1.21.1 needs 21.

```
./gradlew build      # jar in build/libs/
./gradlew runClient  # dev client with Create loaded
```

## License

[LGPL-3.0-or-later](COPYING.LESSER), with the GPL text it builds on in [`COPYING`](COPYING).

Use it, ship it in a modpack, fork it. If you distribute a modified version, that version stays
open under the same licence. Mods that just depend on this one are unaffected. If you want to do
something the licence doesn't allow, ask.
