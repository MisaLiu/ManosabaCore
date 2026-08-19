# ManosabaCore

## Build

- This is a single Gradle Java project (`settings.gradle.kts`); there are no subprojects, lint tasks, or code generation configured. Unit tests use JUnit 5.
- Use Java 21. Run `./gradlew compileJava` for a focused compile or `./gradlew build` for the full build. The checked-in wrapper downloads and uses Gradle 8.11.1.
- The build produces the plugin JAR under `build/libs/`. Paper API, Simple Voice Chat API, and ProtocolLib are `compileOnly`; do not bundle these dependencies.

## Code Layout

- `ManosabaCore` owns plugin lifecycle, config reload, Bukkit channels, and command registration.
- `chat/` filters Paper `AsyncChatEvent` viewers and renders MiniMessage chat; `config/` is the typed immutable config snapshot; `game/DeathStatus` reads datapack scoreboard state.
- `voice/` is an optional runtime-gated Simple Voice Chat integration. `talkbubbles/` implements the `talkbubbles:bubble` plugin-message protocol for the Fabric client mod.
- `tablist/` contains the pure spectator-masking decision, state snapshot, Bukkit bridge, and isolated ProtocolLib adapter. Runtime support targets Paper 1.21.10 with a ProtocolLib build verified for that server version.
- `src/main/resources/plugin.yml` is filtered from `gradle.properties` for version and description. Keep plugin metadata, permissions, and command declarations in sync with the Java command implementation.

## Runtime Contracts

- Chat/game behavior depends on the main scoreboard objectives and values documented in `config.yml`: `Gaming` with holder `manosaba:data`, per-player `dead`, and per-player `Playing`.
- Simple Voice Chat is a soft dependency. The plugin must still load without it; keep SVC API references isolated behind `VoicechatBridge` so absent classes are not resolved during normal plugin startup.
- `/manosaba reload` rebuilds the chat config snapshot, but does not rebuild the voice group or reschedule its sync task. Voice group name/type and sync period require a plugin disable/enable.
- `config.yml` is the source of truth for behavior and integration defaults. TalkBubbles is optional and silently skips clients that do not register its channel.
- Tablist masking requires ProtocolLib and defaults on. It only masks `dead=true`, `Playing=1`, real `SPECTATOR` players from active/alive viewers without `manosaba.tablist.reveal`; other plugins that modify tablist game modes are unsupported conflicts.
- Compile against `net.dmulloy2:ProtocolLib:5.4.0`, but deploy a ProtocolLib build verified for Paper 1.21.10; runtime capability failure disables only the tablist module and `/manosaba reload` retries it.
