# Create: Stock Keeper Bookmarks

Client-side Create addon for Minecraft 1.21.1 / NeoForge. Replaces the Stock Keeper's
single sticky address prompt with a column of one-click destination bookmarks, saved
per Stock Keeper.

## The problem

Create's Stock Keeper has one free-text "Package Address" field. Two things make it
easy to ship an order to the wrong place:

1. **The address is sticky, and it is not yours.** In
   `StockKeeperRequestScreen.init()` the field is seeded from
   `blockEntity.previouslyUsedAddress` — a field on the *block entity*, not on the
   player. Whatever was typed last is what you inherit, including from other players
   on a server.
2. **`sendIt()` does no validation.** It passes `addressBox.getValue()` straight into
   `PackageOrderRequestPacket`. Nothing checks that the address matches any real
   destination. `Shift+Enter` also fires `sendIt()` regardless of which text box has
   focus, so a stray keystroke can ship an order.

A mistyped address means the package rides the chain conveyor and never lands
(see Create issue #7680). A *valid but wrong* address is worse — it clogs a machine.

## What this does

- **A column of destination bookmarks** to the left of the panel, flush against its
  border. One click = one unambiguous destination.
- **Bookmarks are per Stock Keeper.** The keeper in your workshop and the one at your
  smelter keep separate lists, even on the same logistics network.
- **The address field starts empty on every open** — `previouslyUsedAddress` is never
  inherited. This is the actual fix; the bookmarks are what make it painless.
- **Pin, remove, reorder in-game.** Type an address and click **+** to pin it,
  right-click a bookmark to remove it, drag bookmarks to reorder them.
- **The list scrolls** when it outgrows its viewport, with a scrollbar in the left
  gutter. Dragging a bookmark against the top or bottom edge auto-scrolls, so you can
  reorder across the boundary.
- The free-text field is kept, because glob and `regex:` addresses and one-off
  destinations still need it. It is demoted, not removed.
- The column is registered via `getExtraAreas()` so JEI/EMI won't overlap it.

No confirmation dialog: the bookmark *is* the disambiguation, and a confirm step over
many requests is friction for no added safety.

**Sending with an empty address still works, on purpose.** A minimalistic storage
setup has a single, unaddressed destination, so leaving the field blank is the normal
way to use it — the mod never blocks a send.

## Config

`config/stockkeeperbookmarks-client.toml`

| Key | Default | Meaning |
| --- | --- | --- |
| `addresses` | _(empty)_ | Starting bookmarks for a keeper you have not customised yet |
| `clickToSend` | `true` | Click sends immediately; `false` = select, then press Send |
| `clearAddressOnOpen` | `true` | The safeguard. `false` restores Create's sticky behaviour |
| `buttonWidth` | `72` | Bookmark width in pixels. In `WIDE` it is the minimum, not the fixed width |
| `displayMode` | `FIT` | Label layout — see below. Cycled in-game from the name-tag button |
| `collapsed` | `false` | Whether the list is hidden. Toggled by right-clicking the name-tag button |

### Display modes

A small **name-tag button** sits above the bookmarks, aligned to the panel edge. Left-click
cycles the layout, Shift+left-click steps back, right-click hides or shows the list. Hovering
it explains all of this in a tooltip — and because it is drawn even when a keeper has no
bookmarks yet, it is what tells you the mod is there at all.

| Layout | What a too-long address does |
| --- | --- |
| `FIT` | Shrinks toward a legibility floor, then takes an ellipsis |
| `ELLIPSED` | Never shrinks — cut at full size |
| `MINIMAL` | Cut at full size, in a column a few characters wide |
| `WIDE` | The column grows instead, as far as the space to its left allows |
| `WRAP` | Runs onto a second line, in taller rows |

Each has a `_TOOLTIPS` variant that names the full address on hover whenever the label had to
be cut or shrunk. `WIDE_TOOLTIPS` is not redundant: the address box caps address length, so at
an ordinary window size the column can always grow to fit — but at a high GUI scale, or a small
window, it runs out of room and cuts after all.

Collapsing is not one of the modes. It is a separate flag, so hiding the list and showing it
again restores the layout you were using.

Per-keeper bookmarks live in `config/stockkeeperbookmarks-bookmarks.json`, keyed by world,
dimension and block position. A keeper with no entry shows the `addresses` list above;
the first pin, removal or reorder promotes it to its own entry.

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
| Shift + left-click it | Previous display mode |
| Right-click it | Hide or show the bookmark list |

A low-pitched click from **+** means nothing was pinned — the field was blank, or that
address is already bookmarked.

The pin key works while the address field has focus, since typing an address and pinning it
is one flow — so the bound character cannot be typed *into* an address. Rebind it under
Options → Controls if you need that character in a destination name. It is deliberately not
swallowed while the item search box has focus.

The pin key is **always active**. It is an ordinary keybind, registered at startup and listed
under Options → Controls like any other — not something that materialises only when something
breaks. Use it or the **+** button, whichever you prefer.

It shows up as **`=`**, not `+`. Keybinds bind physical keys, and on QWERTY and AZERTY alike
`+` is Shift + `=` — there is no `+` key on the main row for it to bind to, and Minecraft
labels each key by its unshifted character. It is the same physical key you press to type `+`,
and it pins with or without Shift held. The only key that genuinely reads `+` is the numpad's,
which laptops do not have; rebind to it if you prefer.

It does double as the safety net, which is why it was added: the footer **+** is drawn by the
most fragile injection in the mod, and that injection is allowed to fail rather than crash the
game (see below). If a future Create release ever drops the button, the key is how you keep
pinning.

## Implementation

See **[DEVELOPMENT.md](DEVELOPMENT.md)** for the full picture: mixin injection points,
the version-coupled constants read out of Create's sprite sheet and bytecode, why the
buttons are not widgets, and what was deliberately left alone.

The short version: everything is client-side, achieved with one mixin into
`StockKeeperRequestScreen`. Create's menu hands the client the real
`StockTickerBlockEntity`, so telling one Stock Keeper from another needs no server
component.

## Prior art checked

Nothing existing covers this:

- **Create: Better Stock Ticker** — blaze burner integration, NPC cosmetics. No addressing.
- **Create: Easy Stock Ticker** — search autofocus, scroll increments. No addressing.
- **Create: Additional Logistics** — `regex:` addresses, Package Editor, Cash Register.
  Adds address *power*, not address *safety*.

Create 6.0.1 shipped "Added a tooltip for the stock keeper address input" and a
"Safety check for unexpected string modifications in address edit boxes", so the
area is known-touchy upstream, but the sticky-address design is unchanged.

Partial vanilla mitigation worth knowing: `AddressEditBoxHelper` sources autocomplete
suggestions from **nearby Clipboards** (`NEARBY_CLIPBOARDS`). Put a clipboard listing
your addresses near the Stock Ticker and the field will suggest them.

## Status

Working in-game against Create `6.0.10-280`, Minecraft 1.21.1, NeoForge 21.1.248, and
run against every Create release from `6.0.0` to `6.0.10` without layout problems.

Also played on a multiplayer server that does **not** have it installed — uberswe's
*Gearworks 2* — which is the case the client-side claim rests on. That is one server
though, not a guarantee: a conflicting mod or an unusual setup could still break it, which
is why the mod description says *should* work rather than *works*.

```
./gradlew build      # jar in build/libs/
./gradlew runClient  # dev client with Create loaded
```
