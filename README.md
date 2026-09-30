# CmdTree

A small Fabric client mod that dumps the command tree you can actually see (the
Brigadier command tree the server sent your client, already filtered down to whatever
you're permitted to run) to a JSON file on disk. Works identically in singleplayer
and on any remote server, since it never talks to a `MinecraftServer` instance.

## Usage

```
/cmdtree getcommands
```

Running this writes a pretty-printed JSON dump of the full command tree to:

```
<game directory>/cmdtree/cmdtree_commands_<context>_<timestamp>.json
```

- `<context>` is the server's address (sanitized for filesystem safety) when playing
  on a remote multiplayer server, or the singleplayer/LAN world's save name when playing
  solo or hosting a LAN game.
- `<timestamp>` is `yyyy-MM-dd_HH-mm-ss`.

On success, CmdTree reports the full file path in chat. On failure, it reports a short
error in chat and logs the full stack trace to the game log.

Vanilla's own tree-to-JSON serializer will throw if it encounters a Brigadier argument
type it doesn't recognize, which happens with some other mods' client-only commands
(e.g. the `dev.xpple.clientarguments` library used by several QoL mods), since those
argument types are never meant to be sent over the network and so are never registered
with vanilla's `ArgumentTypeInfos`. CmdTree catches this per-node and falls back to a
reduced description (just the node's name and its argument type's class) for the
specific node that can't be fully described, instead of losing the rest of the tree.

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/) for one of the supported
   Minecraft versions below.
2. Download the matching build of [Fabric API](https://modrinth.com/mod/fabric-api)
   for your Minecraft version and drop it in your `mods` folder.
3. Download the CmdTree jar for your Minecraft version and drop it in your `mods` folder.

## Supported Minecraft versions

All eight versions below were actually test-compiled against the real Fabric Loader,
Fabric API, and Minecraft dependencies for that version (not just declared); see
[Notes on version support](#notes-on-version-support) below for how.

| Minecraft | Fabric Loader | Fabric API      | Mappings                  |
|-----------|----------------|------------------|----------------------------|
| 1.20.1    | 0.19.3         | 0.92.11+1.20.1   | Mojang official mappings  |
| 1.20.4    | 0.19.3         | 0.97.3+1.20.4    | Mojang official mappings  |
| 1.21.1    | 0.19.3         | 0.116.15+1.21.1  | Mojang official mappings  |
| 1.21.4    | 0.19.3         | 0.119.4+1.21.4   | Mojang official mappings  |
| 1.21.8    | 0.19.3         | 0.136.1+1.21.8   | Mojang official mappings  |
| 1.21.11   | 0.19.3         | 0.141.6+1.21.11  | Mojang official mappings  |
| 26.1.2    | 0.19.3         | 0.155.2+26.1.2   | none needed (unobfuscated) |
| 26.2      | 0.19.3         | 0.158.0+26.2     | none needed (unobfuscated) |

No candidate versions were dropped: every version originally targeted (spanning
1.20.x, several 1.21.x points, and the current 26.x line) built successfully. 26.1.2
was added after the fact since it's a distinct release from 26.2 in the same 26.x
line. Each `26.x.y` build is declared and compiled separately; it isn't covered by
a version range the way `1.20.4` or `1.21.1` are.

### Notes on version support

- This project builds against **Mojang's official mappings** for every version, not
  Yarn, including versions before 1.21.11, which are technically obfuscated. Loom can
  remap an obfuscated jar using Mojang's own official mappings just as well as Yarn's,
  and doing so meant the mod code below didn't need any version-specific handling for
  the vast majority of the Minecraft API surface it touches (`Minecraft`,
  `ClientPacketListener`, `ArgumentUtils`, `Component`, `ServerData`, `IntegratedServer`,
  `WorldData`: all identical names and signatures from 1.20.1 straight through 26.2).
- **Minecraft 26.1 was the first release Mojang shipped unobfuscated.** Fabric
  discontinued Yarn and Intermediary mappings from 26.1 onward, so any project still
  targeting Yarn mappings cannot support 26.x at all. Building against official mappings
  throughout, as this project does, is what makes supporting both sides of that line in
  one codebase practical.
- The **only** genuine version-specific difference this mod needed to handle: Fabric
  API renamed its client-command helper class from `ClientCommandManager` to
  `ClientCommands` starting with the Fabric API build for Minecraft 26.1 (part of a
  broader Fabric API renaming pass to match Mojang's official terminology now that it's
  public). The two versions of that class have identical static method signatures, so a
  single Stonecutter-swapped `import static` line is the entire fix; see
  `GetCommandsCommand.java`.
- 1.21.8 only ever had a single Yarn mappings build published upstream (`+build.1`)
  before Fabric moved past it. That's irrelevant here since we don't depend on Yarn, but
  worth knowing if you go looking for Yarn mappings for that version.

## Building from source

This project uses [Stonecutter](https://stonecutter.kikugie.dev/) to maintain one
shared source tree across every supported Minecraft version.

```
./gradlew buildAndCollect
```

produces a jar (and sources jar) for every supported version under
`build/libs/<mod version>/` (Gradle runs the `buildAndCollect` task in every version
subproject when invoked unqualified at the root). To work on a single version in an
IDE, switch the active version first:

```
./gradlew "Set active project to 1.21.11"
```

## License

MIT. See [LICENSE](LICENSE).