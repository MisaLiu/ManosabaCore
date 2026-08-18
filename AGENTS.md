# ManosabaCore

## Build

- This is a single Gradle Java project (`settings.gradle.kts`); there are no subprojects, tests, lint tasks, or code generation configured.
- Use Java 21. Run `./gradlew compileJava` for a focused compile or `./gradlew build` for the full build. The checked-in wrapper downloads and uses Gradle 8.11.1.
- The build produces the plugin JAR under `build/libs/`. Paper API and Simple Voice Chat API are `compileOnly`; do not bundle either dependency.

## Code Layout

- `ManosabaCore` owns plugin lifecycle, config reload, Bukkit channels, and command registration.
- `chat/` filters Paper `AsyncChatEvent` viewers and renders MiniMessage chat; `config/` is the typed immutable config snapshot; `game/DeathStatus` reads datapack scoreboard state.
- `voice/` is an optional runtime-gated Simple Voice Chat integration. `talkbubbles/` implements the `talkbubbles:bubble` plugin-message protocol for the Fabric client mod.
- `src/main/resources/plugin.yml` is filtered from `gradle.properties` for version and description. Keep plugin metadata, permissions, and command declarations in sync with the Java command implementation.

## Runtime Contracts

- Chat/game behavior depends on the main scoreboard objectives and values documented in `config.yml`: `Gaming` with holder `manosaba:data`, per-player `dead`, and per-player `Playing`.
- Simple Voice Chat is a soft dependency. The plugin must still load without it; keep SVC API references isolated behind `VoicechatBridge` so absent classes are not resolved during normal plugin startup.
- `/manosaba reload` rebuilds the chat config snapshot, but does not rebuild the voice group or reschedule its sync task. Voice group name/type and sync period require a plugin disable/enable.
- `config.yml` is the source of truth for behavior and integration defaults. TalkBubbles is optional and silently skips clients that do not register its channel.
