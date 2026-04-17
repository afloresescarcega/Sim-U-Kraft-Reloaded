# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Sim-U-Kraft Reloaded is a **Minecraft 1.7.10 Forge mod** (modid `ashjacksimukraftreloaded`). It spawns "Folk" NPCs that can be employed to build, mine, farm, etc. The mod targets a legacy MC version but builds under a **modern Gradle toolchain**.

- Minecraft: `1.7.10` (Forge `~10.13.4.1614` — pinned canonically by RFG; see note below)
- Build plugin: [RetroFuturaGradle (RFG) `1.4.0`](https://github.com/GTNewHorizons/RetroFuturaGradle) — GTNH's maintained fork of ForgeGradle 1.x
- Gradle: `8.8` (wrapper, pinned — RFG 1.4.0 supports 7.6–8.8)
- Java target bytecode: `8` (MC 1.7.10 classpath requirement, declared via `languageVersion = JavaLanguageVersion.of(8)` toolchain — RFG provisions a JDK 8 automatically from Azul)
- Java to invoke Gradle: `17`+ (Gradle 8.8 requires it — you can launch the build from JDK 21)

**Forge pin change**: RFG does not let you choose arbitrary Forge builds — it ships its own canonical 1.7.10 Forge (`~10.13.4.1614`). The legacy pin `1.7.10-10.13.2.1291` from the pre-RFG era is no longer user-selectable. The 1.7.10 Forge API surface is stable, but a handful of raw MCP mapping names (e.g. `world.func_147480_a`) may need fixups if they shift between those two Forge builds. See `build.gradle` for the full rationale.

Two external mod APIs are pulled in `compileOnly` so `TileEntityWindmill` can resolve — no runtime dependency:
- `curse.maven:cofh-lib-220333:2388748` → `cofh.api.energy.*` (Redstone Flux API)
- `com.github.GTNewHorizons:BuildCraft:7.1.49-pre:api` → `buildcraft.api.power.*`

CurseMaven (`https://cursemaven.com`) is added alongside the GTNH Nexus in `repositories` for the CoFH jar — it isn't mirrored in the GTNH public repo.

## Build & run

```bash
./gradlew build         # compile + reobfuscate → build/libs/ashjacksimukraftreloaded-<ver>.jar (+ -dev.jar)
./gradlew test          # run JUnit suite (see below)
./gradlew runClient     # launch dev MC client with the mod
./gradlew runServer     # launch dev dedicated server with the mod
./gradlew tasks         # full task list (RFG adds setupDecompWorkspace, reobfJar, etc.)
```

RFG handles MC source decompilation transparently — there is no manual `setupDecompWorkspace` step under 1.4.0 (the task still exists for compat, but `build` already runs the patched/decompiled MC pipeline on first invocation). The dev run directory is `./run/` (was `./eclipse/` under ForgeGradle 1.2 — both are gitignored).

There is a **minimal JUnit 4 suite** under `src/test/java/` covering the handful of domain classes that are free of Minecraft/Forge imports (currently `GameMode` and `References`). It is wired via `testImplementation 'junit:junit:4.13.2'` in `build.gradle` and runs under `./gradlew test`. Most of the mod is still "tested" by launching the client via `runClient` and exercising it in-world.

When adding unit tests, restrict targets to classes whose transitive compile graph has no `net.minecraft.*`, `cpw.mods.fml.*`, or `io.netty.*` symbols. In particular, the `folk/traits/*` classes *look* pure but the `Trait` base class declares `public FolkData theFolk` and calls `Building.getBuildingBySearch`, so it requires the Forge-deobfuscated MC jar to compile. Use `Glob`/`Grep` to confirm the transitive graph before adding a test.

**Greenfield subsystems should not taint their testable core.** `./gradlew test` runs in a vanilla JVM without Forge's `LaunchClassLoader`, tweakers, LWJGL natives, or the FML bootstrap sequence — so any class whose transitive graph pulls in MC/Forge/Netty is effectively untestable by unit test, no matter how pure its own logic looks. When introducing a new feature (a new Folk behavior, a new building-catalogue parser, a new scheduling rule, etc.), shape it as a pure-Java **core** that takes primitive inputs (coords as `int`/`double`, item names as `String`, inventory as `List<String>` or a small DTO) plus a thin **adapter** that owns the MC calls (`World.getBlock`, `MinecraftServer.getServer()`, `ItemStack`, `TileEntity`). The adapter lives next to the existing MC-coupled classes; the core lives in its own package with a `*Test.java` next to it from day one. If the core needs to *read* world state, define a tiny interface (`WorldView` with `isAir(int,int,int)` / `getBlockName(int,int,int)`) and have the adapter implement it — do not pass `World` into the core. This is the only shape that survives the `./gradlew test` constraint. Do **not** retrofit this onto existing god-objects (`SimukraftReloaded`, `FolkData`, `Building`) unless the task specifically asks — they are load-bearing and the 1.7.10 target caps the ROI on a wholesale refactor.

Version is declared in two places that must be kept in sync: `build.gradle` (`version = ...`) and `ModSimukraft.version` / `@Mod(version=...)` in [src/main/java/ashjack/simukraftreloaded/core/ModSimukraft.java](src/main/java/ashjack/simukraftreloaded/core/ModSimukraft.java). `mcmod.info` gets its version substituted by the `processResources` task.

## Architecture

### Entry point and registration flow

[ModSimukraft.java](src/main/java/ashjack/simukraftreloaded/core/ModSimukraft.java) is the `@Mod` class. `preinit()` is where almost everything wires up, and it delegates to a family of static `load*()` registrars in [core/registry/](src/main/java/ashjack/simukraftreloaded/core/registry/):

- `SimukraftReloadedConfig` — Forge `Configuration` file
- `SimukraftReloadedTabs` / `Blocks` / `Items` / `Gases` / `Entities` / `Events` / `Recipes`
- `PacketHandler.initPackets()` — builds the `SimpleNetworkWrapper`
- `Traits.loadTraits()` — Folk personality traits

Entity IDs are registered manually (`EntityRegistry.registerModEntity(...)` + `registerGlobalEntityID(...)`) in `preinit`; if you add an entity, register it there and keep the numeric id unique.

### The god-object: `SimukraftReloaded`

[core/registry/SimukraftReloaded.java](src/main/java/ashjack/simukraftreloaded/core/registry/SimukraftReloaded.java) is the runtime state holder — **not just a registry despite its package name**. All live game state hangs off static fields here:

- `theFolks`, `theBuildings`, `theCourierTasks`, `theCourierPoints`, `theMiningBoxes`, `theFarmingBoxes`, `theRelationships`, `theCommodities`
- `states` — the `GameStates` singleton (credits, dayOfWeek, gameModeNumber, lastUpdateCheck, …)

`resetAndLoadNewWorld()` is the world-load lifecycle: it clears every list, deserializes `settings.sk2` (legacy: `settings.suk`), then calls each subsystem's `load*()` static (`Building.loadAllBuildings`, `CourierTask.loadCourierTasksAndPoints`, `MiningBox.loadMiningBoxes`, `FarmingBox.loadFarmingBoxes`, `FolkData.loadAndSpawnFolks`, `Relationship.loadRelationships`). Any new persistent subsystem must plug into this method and into the day-transition logic (`dayTransitionHandler` → `evolveFolks`).

Because state is static, **there is effectively one world's worth of state globally** — this matters for SMP behavior and for world-switch/reload sequencing.

### Persistence

Two on-disk locations, both resolved relative to the MC working dir:

- `getSavesDataFolder()` → `saves/<world>/simukraft/` — per-world state (`settings.sk2`, building/folk/box/relationship files)
- `getSimukraftFolder()` → `mods/Simukraft/buildings/{residential,other,…}/PKID<n>-<title>.txt` — the shared building catalogue

File format is `.sk2` — plain-text line-oriented, read/written via `loadSK2`/`saveSK2` as `ArrayList<String>`. `.suk` is the legacy serialized-Java format; code paths still read it as a fallback but all writes go to `.sk2`. Don't introduce a new format — extend `.sk2`.

### Client/server split

Standard Forge `@SidedProxy`:

- [proxies/CommonProxy.java](src/main/java/ashjack/simukraftreloaded/proxies/CommonProxy.java) — server + shared, also hosts the inner classes `Commodity` and `V3` (V3 is the universal 3D-point-with-dimension used everywhere)
- [proxies/ClientProxy.java](src/main/java/ashjack/simukraftreloaded/proxies/ClientProxy.java) — client-only rendering/registration, also exposes `log`

Tick loops live in `common/CommonTickHandler` and `client/ClientTickHandler`.

### Networking

All packets go through `SimpleNetworkWrapper` (`PacketHandler.net`) in [packetsNEW/](src/main/java/ashjack/simukraftreloaded/packetsNEW/). Directory layout mirrors destination side:

- `packetsNEW/toServer/` → `Side.SERVER`
- `packetsNEW/toClient/` → `Side.CLIENT`

New packets must be registered in `PacketHandler.initPackets()` with a unique discriminator id and the correct `Side`. The legacy `packets/` package was removed; only `packetsNEW/` is live.

### Folk / Building / Job domain

- [folk/FolkData.java](src/main/java/ashjack/simukraftreloaded/folk/FolkData.java) — data object for a Folk (paired with `EntityFolk`); `folk.theEntity` is the currently-spawned entity
- `folk/traits/` — personality traits loaded via `Traits.loadTraits()`; each trait is its own class extending `Trait`
- `folk/genetics/` — inheritance for offspring
- [core/building/Building.java](src/main/java/ashjack/simukraftreloaded/core/building/Building.java) — a placed building instance; `Building.initialiseAllBuildings()` reads the global catalogue, `loadAllBuildings()` reads this world's placements
- `common/jobs/` + `core/jobs/` — Folk vocations (`Job.Vocation` enum, `JobSoldier`, etc.)

### GUIs, blocks, entities

- GUIs open through [client/Gui/GuiHandler.java](src/main/java/ashjack/simukraftreloaded/client/Gui/GuiHandler.java) (Forge `IGuiHandler`). Gui IDs live in [core/References.java](src/main/java/ashjack/simukraftreloaded/core/References.java).
- Block classes in [blocks/](src/main/java/ashjack/simukraftreloaded/blocks/); their tile-entity / functional counterparts live under `blocks/functionality/` (e.g. `FarmingBox`, `MiningBox`, `TileEntityWindmill`, `FluidMilk`).
- Entities in [entity/](src/main/java/ashjack/simukraftreloaded/entity/): `EntityFolk` (the NPC), `EntityConBox` (construction drone), `EntityAlignBeam`, `EntityWindmill`.

### Assets

All assets under `src/main/resources/assets/ashjacksimukraftreloaded/` — `textures/{blocks,items,gui,models}/`, `skins/` (Folk skins), `sounds/` (referenced by domain `ashjacksimukraftreloaded:...`), `lang/`.

## Gotchas when editing

- **1.7.10 API**, not modern Forge: blocks/items register via `GameRegistry.registerBlock/Item`, world access is `world.getBlock(x,y,z)` / `setBlock(x,y,z,block,meta,flag)`, and you'll see obfuscated helpers like `world.func_147480_a(...)` — leave those calls intact; they're MCP mappings for the target MC version.
- `civucraft/` appears to be an in-progress fork/experiment; don't assume it's wired into `ModSimukraft`.
- `core/Unused.java` is exactly what it says — dead code kept for reference.
- The `UpdateChecker` / `ThreadUpdate` logic in `SimukraftReloaded` points at a dead Dropbox URL; don't rely on it and don't "fix" it without being asked.
