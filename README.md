# Create: Stock Keeper Bookmarks

A client-side [Create](https://modrinth.com/mod/create) addon that replaces the Stock Keeper's
single sticky address prompt with a column of one-click destination bookmarks — so a forgotten
address can't quietly ship your order to the crusher.

**This branch holds no code.** Each supported Minecraft version is its own branch, because the
loader APIs are mutually exclusive and cannot be merged into a single source tree.

## Branches

| Branch | Minecraft | Loader | Create | Java |
| --- | --- | --- | --- | --- |
| [`1.21.1-neoforge`](../../tree/1.21.1-neoforge) | 1.21.1 | NeoForge | `[6.0.0,6.1.0)` | 21 |
| [`1.20.1-forge`](../../tree/1.20.1-forge) | 1.20.1 | Forge **and** NeoForge | `[6.0.7,6.1.0)` | 17 |

Those are the ranges the branches' mod metadata actually declares: every `6.0.x` at or above
the floor, and nothing in `6.1`. The 1.20.1 floor is higher for a reason rather than caution —
Create `6.0.0`–`6.0.6` on that line fail to load, in two distinct ways, and the branch's
`DEVELOPMENT.md` breaks down which versions fail how. The two Create lines share the `6.0.x`
scheme but are separate releases, so the version numbers are not comparable between rows.

The 1.20.1 build runs on both loaders from one jar: NeoForge's 1.20.1 line is a compatibility
backport that keeps Forge's `net.minecraftforge` namespace and registers itself as `forge`, so a
Forge-targeted jar loads unmodified on either. That is specific to 1.20.1 — NeoForge diverged
fully from 1.20.2 onward, which is why 1.21.1 needs a separate branch and a different toolchain.

## Downloads

**[CurseForge](https://www.curseforge.com/minecraft/mc-mods/create-stock-keeper-bookmarks)** —
both the 1.21.1 and 1.20.1 builds are published there.

## Why the branches never merge

They differ at the import level — `net.minecraftforge` against `net.neoforged`,
`ForgeConfigSpec` against `ModConfigSpec`, `new ResourceLocation(...)` against
`ResourceLocation.fromNamespaceAndPath(...)`, `mouseScrolled` with three parameters against
four. Each is an either/or, so a merged file would compile against neither loader. They also
need different build toolchains: ForgeGradle 6 on Gradle 8.1.1 and JDK 17 for 1.20.1,
ModDevGradle on Gradle 9 and JDK 21 for 1.21.1.

Not everything diverges, though. These are byte-identical across both branches, so a fix to
label layout, display modes, or user-facing strings cherry-picks cleanly:

```
DisplayMode.java · AddressButton.java · FooterPatch.java · en_us.json · icon.png
```

Two more differ only in imports — `BookmarkStore.java` by a single line, `ModKeys.java` by its
import block plus the shape of one annotation — so fixes there cherry-pick with a small fixup.
The mixin, the config class and the mod entrypoint are where the loaders genuinely part ways,
and fixes there have to be written twice. The metadata differs by construction: `mods.toml`
against `neoforge.mods.toml`, a different `mixins.json`, and a `pack.mcmeta` that only the
1.20.1 build needs.

Each branch's own `README.md` documents that version's features and controls, and its
`DEVELOPMENT.md` covers mixin injection points and the compatibility testing behind its
declared Create range.

## License

[LGPL-3.0-or-later](COPYING.LESSER) — see [`COPYING`](COPYING) for the GPL text it builds on.
Forks and modified redistributions stay open under the same terms; mods that merely depend on
this one are unaffected. If you want to do something the licence doesn't allow, ask.
