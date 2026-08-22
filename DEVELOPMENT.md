# Create: Stock Keeper Bookmarks — Development Notes

Durable context for this mod, kept in the repo (same convention as the Foghorn
project) so a fresh checkout gives a new dev the whole picture without needing
the conversation that produced it.

Verified against **Create `6.0.10-280`**, Minecraft **1.21.1**, NeoForge
**21.1.248**. Client-side only.

---

## Overview

Create's Stock Keeper has one free-text "Package Address" field, seeded from
`StockTickerBlockEntity.previouslyUsedAddress` — a field on the *block entity*,
not on the player. You inherit whatever was typed last, including by other
players on a server, and `sendIt()` does not validate the address. The result is
orders shipped to the wrong machine.

This mod replaces that flow with a column of one-click destination bookmarks
beside the panel, and stops the address field inheriting a stale value.

Everything is client-side. No packets, no server component, no mixins into
anything but `StockKeeperRequestScreen`.

---

## Environment setup

```
./gradlew build      # jar in build/libs/
./gradlew runClient  # dev client with Create loaded
```

- Create is pulled from the Modrinth maven as the **`slim`** classifier with
  `transitive = false`; its own hard deps (Ponder, Registrate, Flywheel) are
  declared separately so the dev client actually boots.
- **No refmap.** NeoForge 1.21.1 runs Mojang official mappings in production,
  the same as in dev, so mixin targets are written against those names directly.
- `neoforge.mods.toml` is **generated** from `src/main/templates/META-INF/` by
  the `generateModMetadata` task — edit the template, not `build/`.
- Local testing is a copy of `build/libs/stockkeeperbookmarks-0.1.0.jar` into a
  Modrinth App instance's `mods` folder. **Verify the copy landed**: if the game
  is running, Windows refuses the overwrite and `cp` still reports success, so
  compare hashes rather than trusting the exit code.

---

## Version-coupled constants

This is the part that breaks on a Create update, and the reason each number is a
named constant rather than inline. Every value below was read out of Create
`6.0.10-280` — from `javap` of `StockKeeperRequestScreen` / `AllGuiTextures`, or
by sampling `create:textures/gui/stock_keeper.png` directly. **Re-derive these
before widening the supported Create range.**

| Constant | Value | Meaning | How it was derived |
| --- | --- | --- | --- |
| `SHEET_ORIGIN_DX` | `-15` | Where the panel sprites draw from | `renderBg` renders HEADER, BODY and FOOTER all at `guiLeft - 15` |
| `BODY_EDGE_U` | `24` | First drawn column of the body sprite | Sampled `stock_keeper.png` rows `v=48..67`: first opaque pixel is column 24 (black outline), then grey `141,143,141`. Header rows flare out to column 16 — that flare is the wider top/bottom, and aligning to it is what looked "overlapping" |
| `GRID_TOP_PADDING` | `4` | Top of the first item slot row | `renderBg` lays rows out at `itemsY + (categories.isEmpty() ? 4 : 20) + (i / 9) * 20` |
| `ROW_DX`, `ROW_DY` | `145`, `-41` | Send button origin | Create draws Send 80×20 at `(guiLeft + 145, guiTop + windowHeight - 41)` |
| `BTN_W`, `ROW_H` | `80`, `20` | Send button size | as above |
| `BASE_U/V` | `160, 119` | Unhovered Send artwork, baked into the FOOTER sheet | |
| `HOVER_U/V` | `55, 200` | `STOCK_KEEPER_REQUEST_SEND_HOVER` sprite | Both are 80×20, so one set of slice offsets addresses either state |
| `BG_U/V` | `156, 119` | Plain footer background | Used to paint the gutter between the pin button and Send so it matches exactly |

Sprite rects from `AllGuiTextures`' static initialiser, for reference:
`STOCK_KEEPER_REQUEST_HEADER` = `(0, 0, 256, 36)`,
`STOCK_KEEPER_REQUEST_BODY` = `(0, 48, 256, 20)`.

---

## Mixin injection points

All in `StockKeeperRequestScreenMixin`, all against `StockKeeperRequestScreen`.

| Target | Where | Why there |
| --- | --- | --- |
| `init` | TAIL | Build the column. Runs again on window resize, hence the `openedOnce` latch so only a genuine open clears the address |
| `renderBg` | `CONSTANT` `gui.stock_keeper.title` | Repaint the footer row *after* Create has drawn the footer and Send hover but *before* the Send caption, so the caption and its "sent" fade still land on top untouched |
| `renderBg` | `@ModifyArg` on `drawString`, sliced | Shift the Send caption to the shortened button's centre |
| `renderForeground` | **HEAD** | Draw the column, scissored, above the widget list. Must not be TAIL — see below |
| `isConfirmHovered` | HEAD, cancel | Shrink Create's own Send hit test to the shortened button |
| `mouseClicked` / `mouseDragged` / `mouseReleased` / `mouseScrolled` | HEAD, cancel | Own the sidebar's input. Each only cancels when our own state says the event is ours |
| `getExtraAreas` | RETURN | Reserve the column from JEI/EMI |
| `keyPressed` | HEAD, cancel | Keyboard route to the pin action, so pinning does not depend on the footer artwork |
| `charTyped` | HEAD, cancel | Drop the character of a keystroke already consumed as a pin — GLFW delivers it through a separate callback, so cancelling `keyPressed` alone does not keep it out of the focused address box |

**The two `renderBg` injectors are `require = 0`; everything else is mandatory.**
They are the only ones anchored to a string constant inside a method rather than to
a method name, which makes them the first thing a Create update will move. With the
config's `defaultRequire: 1` they would take the whole game down on startup — over a
button that is purely cosmetic.

Both constants have in fact held stable across every Create release from `6.0.0` to
`6.0.10`, which is real evidence that patch releases do not disturb them. The guard is
kept anyway because the declared range `[6.0.0,6.1.0)` also admits *future* 6.0.x
builds, and the cost of being wrong is asymmetric: `require = 0` costs a missing
button, `require = 1` costs a game that will not start.

Making them optional is not enough on its own, because two other injectors assume
that artwork exists: the `+` hit box in `mouseClicked` would answer clicks for an
invisible button, and `isConfirmHovered` would shrink the hit test of a Send button
still drawn at full width. Both now gate on `FooterPatch`, a flag the repaint sets on
its first frame. If the repaint is dropped the footer behaves exactly like Create's
own, and the keybind carries the pin action.

Order is safe: `renderBg` runs before any input event on a screen, so the flag is
always resolved before anything reads it.

**When an injector may be made optional.** The test is not how important it is —
it is whether its failure is *contained*. Something else has to cover the loss,
and nothing may be left in a half-state. The footer pair qualifies on both counts:
`FooterPatch` gates the `+` hit box and the narrowed Send, and the pin keybind is
an independent route to the one action that would otherwise become impossible.

Nothing else qualifies, and dropping `require` on the rest would trade a loud
failure for a quieter, worse one. If `init` did not apply while `renderForeground`
did, `drawHeader` would dereference a null `mode` and call `renderItem(null, ...)`
— still a crash, only later and pointing at rendering rather than at the cause. If
`renderForeground` dropped while `mouseClicked` survived, the column would be
invisible and still clickable, which is the exact trap `FooterPatch` exists to
prevent. Covering those would mean null-guarding the whole renderer to defend
against something that cannot happen.

**Every mandatory target is verified present at both ends of the declared range.**
`javap` against Create `6.0.0` and `6.0.10` confirms all sixteen members this mixin
binds to exist in both:

- **injected** — `init`, `renderBg`, `renderForeground`, `isConfirmHovered`,
  `getExtraAreas`, `keyPressed`, `charTyped`, `mouseClicked`, `mouseDragged`,
  `mouseReleased`, `mouseScrolled`
- **`@Shadow`** — `sendIt()`, and the fields `searchBox`, `addressBox`,
  `windowHeight`, `itemsY`

Shadows belong in that audit: a missing shadowed field fails the mixin just as hard
as a missing injection target, and is the half that is easy to forget. Re-run it
with `javap -p -cp <create.jar> <class>` after any Create version bump — it needs
no game launch, and it is the cheap way to know a mandatory binding cannot fail.

So `defaultRequire: 1` stays, as a decision rather than an inherited default.
Separately, `"required": true` at the top of the config is a different setting — it
governs whether the mixin must apply to its target class at all, not the individual
injectors. Create is a hard dependency, so that class is always present and `true`
is correct.

**Always `graphics.flush()` before `enableScissor`.** This is the load-bearing line
in the whole renderer, and it is not obvious.

`GuiGraphics.applyScissor` calls `flushIfManaged()`, which flushes **only when
`this.managed` is true** — and `managed` is set solely inside `drawManaged(Runnable)`.
Normal screen rendering never wraps in that, so during `renderForeground` the buffer
is *unmanaged* and changing the scissor does **not** flush. Any geometry another mod
still has queued at that moment is therefore emitted clipped to this column's ~76px
rect, and vanishes.

The symptom is blank tooltips, and it is not limited to one victim. It first showed
up as Create's address-box tooltip rendering empty; moving the inject from TAIL to
HEAD made that stop, which looked like a fix but was not — it only changed *whose*
geometry was in flight when the scissor narrowed. The bug stayed live and moved to
**every JEI tooltip in the screen**, which is how it was eventually caught. An
explicit `flush()` is unconditional and fixes it at any inject position.

Reproducing it needs at least one bookmark: `drawColumn` returns early on an empty
list, before the scissor is ever touched, so a keeper with no bookmarks looks fine.
That is what made it look intermittent.

**Draw the column at `renderForeground` HEAD** — but for layering, not for the above.
Create renders all four of its tooltips at the end of that method, so a TAIL inject
would paint the column over them. HEAD puts tooltips above the column, which is the
correct order.

The address-box tooltip is easy to miss in testing, because it only shows when the
field is blank, unfocused and hovered — and `clearAddressOnOpen` means **this mod is
what makes it appear regularly**. Without the mod the field is usually pre-filled
from `previouslyUsedAddress`, so vanilla rarely renders it at all.

**The `@ModifyArg` slice must stay closed at both ends.** Create draws the Send
caption twice (a plain branch and a faded just-sent branch) and then draws the
"Request Sent" ribbon through the *same* `drawString` overload nine bytecode
offsets later. An open-ended slice shifts that ribbon off-centre too. The slice
runs from the `gui.stock_keeper.send` constant to the
`gui.stock_keeper.request_sent` constant.

---

## Architecture

### Bookmarks are per Stock Keeper, with no server help

`StockKeeperRequestMenu` extends `MenuBase<StockTickerBlockEntity>`, and its
`createOnClient` reads a `BlockPos` off the buffer then calls
`ClientLevel.getBlockEntity(pos)`. So `menu.contentHolder` is the **real** synced
block entity, not a stub — identifying which keeper you are standing at is a
field read, no packet needed.

Storage is `config/stockkeeperbookmarks-bookmarks.json`, keyed
`worldScope | dimension | x,y,z`. The world scope (`server/<ip>` or
`local/<save folder>`) matters: without it, keepers at the same coordinates in two
different saves would share bookmarks.

The singleplayer half of that scope is the save's **folder**, not
`getWorldData().getLevelName()`. Minecraft disambiguates the folder rather than the
display name, so two saves both created as "New World" live in `New World` and
`New World (1)` while both reporting the name "New World" — keying by name defeated
the very collision the scope exists to prevent. The server half is lowercased and has
an explicit `:25565` stripped, so the same host reached two ways stays one scope; a
host entered once by name and once by raw IP still cannot be reconciled.

A keeper with no entry falls back to the config `addresses` list; the first edit
promotes it to its own entry. Untouched keepers cost nothing on disk.

> A **network**-scoped key is also available if per-keeper ever proves wrong:
> `StockCheckingBlockEntity.behaviour` is a public `LogisticallyLinkedBehaviour`
> whose `freqId` UUID identifies the logistics network, and it *is* client-synced
> — `write` puts `Freq` into NBT unconditionally, ignoring the `clientPacket`
> flag. Per-block was chosen deliberately (see below).

### The buttons are not widgets

`AddressButton` is a plain class, not an `AbstractWidget`, and is not in the
screen's renderable list. The screen scissors the column to a scrolling viewport
and draws it in `renderForeground`. Registering them as widgets would fight that
on two fronts: vanilla draws renderables unclipped, and `isMouseOver` would hover
a button that had scrolled out of sight.

**Cost:** no keyboard focus and no narration for the sidebar. In practice they
were never usable that way — all input has been screen-level since drag landed.

### Click acts on release, not press

A press cannot know yet whether it is a click or the start of a drag. Firing on
press would send an order every time you began dragging. Under
`DRAG_SLOP` (3px) of movement a press is still a click.

Input cannot go through `AbstractWidget`'s drag callbacks at all:
`AbstractContainerScreen.mouseDragged` handles slot quick-crafting and returns
without forwarding to children, so widget-level drags never arrive in a container
screen. Hence the screen-level injects.

### Scroll and drag

- Viewport height is `windowHeight * 0.6`, clamped so it cannot run past the
  panel's bottom edge — on short windows that clamp binds first.
- `columnTop` is the column's topmost **drawn** pixel (the first button's border
  line); `contentTop()` is one below it, where fills begin. Buttons paint their
  border one pixel above their origin, so without that split the first button's
  top border falls outside the scissor and vanishes.
- The drag also advances **once per frame** from the render pass, not only from
  `mouseDragged`. When the cursor is held against the viewport edge and stops
  moving, `mouseDragged` stops firing but auto-scroll has to keep going —
  otherwise reordering across a scroll boundary is impossible.
- The drop index is computed in content space (`ghostY - contentTop + scroll`)
  so it stays correct as the offset changes underneath.

---

## Performance pass

Recorded so it is not re-litigated later.

**Fixed:** `AddressButton` was calling `Font#width(String)` — which walks the
whole string — plus sometimes `plainSubstrByWidth`, for **every visible button on
every frame**, to compute a label and scale that never change. Address, width and
font are all fixed for the button's life, so the label, scale and vertical offset
are now resolved once in the constructor. The pose push/scale/pop is also skipped
entirely when the label fits at 1.0.

**Checked, and fine:**

- `ModConfigSpec.ConfigValue#get()` **is cached** — a null-check over a cached
  field after first read, not a re-parse (confirmed against NeoForge's source,
  same finding as Foghorn's own performance pass). The per-frame
  `BUTTON_WIDTH.get()` calls were not a sink. They were hoisted into
  `buttonWidth` at init anyway, but for **correctness**, not speed: `columnX` is
  derived from that width, so re-reading it per frame could leave the two
  disagreeing after a config reload.
- `buttons.indexOf(...)` during drag and the per-frame `buttonAt(...)` hover scan
  are both O(n) over a list that is realistically under ~20 entries, only while a
  Stock Keeper GUI is open. Not worth a map.
- `BookmarkStore.save()` rewrites the whole file on each pin/remove/drop. These
  are discrete user actions, not per-frame, and the file is a few hundred bytes.

---

## Considered and deliberately not changed

- **Splitting the mixin into sidebar + footer mixins.** The file is long and
  covers two concerns, but both halves need `mouseClicked` at HEAD, and ordering
  between two mixins targeting the same method is not guaranteed without explicit
  priorities. Trading a real correctness hazard for tidiness is a bad deal.
- **Per-network bookmarks instead of per-block.** The `freqId` UUID is available
  and is arguably the more semantically correct unit (addresses *are*
  network-wide destinations), and it would survive moving the block. Per-block
  was chosen because it is the only option that lets a workshop keeper and a
  smelter keeper on the same network show different lists.
- **Following `itemsY + 20` when categories are present.** Create pushes its
  first slot row down by 16px to fit a category header. Tracking that would make
  the sidebar jump up and down as categories appear and disappear with the stock;
  the stable anchor at `+4` is worth the misalignment in that state.
- **A cap on bookmarks per keeper.** Existed briefly (10) before scrolling; the
  scrolling viewport makes it unnecessary.
- **Guarding `sendIt()` against an empty address.** Tempting, and an earlier
  draft of the README wrongly claimed it was there. It would be **wrong**: a
  minimalistic storage setup has a single, *unaddressed* destination, so an empty
  address field is the normal case there, not a mistake. Blocking it would break
  those builds to protect against a misfire the empty-on-open behaviour already
  handles. Leave `sendIt()` alone.
- **Pruning an entry when its block is destroyed.** The key is the Stock
  *Ticker* block's position, and an entry for a broken ticker stays in the JSON
  forever. This cannot be fixed client-side: the client is only told about blocks
  in chunks loaded near the player, so a ticker broken by someone else while you
  are away — or offline — is never observed at all. Detection would only ever
  catch the case where you stood and watched it break, which is the case least in
  need of automation. The alternatives that sidestep detection, an LRU cap or
  ageing out entries not opened recently, can each delete a live keeper you simply
  do not visit often. For a file of a few hundred bytes, unbounded growth is the
  better trade than a rule that can eat real data.
- **Narrator support on hover.** `Minecraft.getInstance().getNarrator().say(...)`
  would work, given a check of `options.narrator()` to respect an Off or
  chat-only setting, and a hover-*change* guard so it does not stutter every
  frame. It is left out because it would be half a feature: hover narration only
  reaches someone using a mouse *and* the narrator, while a keyboard user still
  cannot reach a bookmark at all — the buttons are not widgets, so the narration
  framework has nothing to focus. Real support means focus and narration
  together, and focus is exactly what the not-a-widget decision above rules out.
  Create's own Stock Keeper screen is not keyboard-navigable either, so this sits
  level with the surrounding UI rather than below it.

---

## Known gaps

- **The declared Create range is now verified across every released 6.0.x.**
  The constants in the table above were read from `6.0.10-280`, but the mod has
  since been run against each Create release from `6.0.0` to `6.0.10` with no
  layout problems — so the sprite sheet and the string constants have held stable
  for the whole line so far. What `[6.0.0,6.1.0)` still covers that nobody can test
  is *future* 6.0.x patch releases; that residual exposure is what the two
  `require = 0` injectors exist for.

  That run predates the `keyPressed` / `charTyped` injectors, so it does not cover
  them. They were checked separately and statically instead: `javap` on Create
  `6.0.0` confirms `StockKeeperRequestScreen` overrides **every** method this mixin
  targets — `init`, `renderBg`, `renderForeground`, `isConfirmHovered`, `sendIt`,
  all four mouse handlers, `getExtraAreas`, and both `keyPressed(int,int,int)` and
  `charTyped(char,int)` — and that all three anchor constants
  (`gui.stock_keeper.title`, `.send`, `.request_sent`) are present in its `renderBg`.
  Oldest and newest 6.0.x therefore agree on the entire injection surface.

  **Worth repeating for any future injector**: `javap -p -cp <create.jar>` on the
  target class is the cheap way to prove a mandatory injection cannot fail to find
  its method, and it needs no game launch.
- **No automated tests.** Everything here is GUI geometry against a third-party
  mod's private layout; verification has been visual, in-game.
