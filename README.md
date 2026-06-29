# SculkCore

SculkCore is a modular, high-performance Survival Multiplayer (SMP) event management plugin built for Minecraft Paper 1.21 servers.

> [!NOTE]
> Deobfuscation & clean-room reimplementation by **@Cibersec**.

---

## Features

- **Event Management**: Streamlined administration tools to start server-wide events, coordinate grace periods, and handle world border shrinkage.
- **Combat Balancing**: Custom mechanics for shield cooldowns, ender pearl speed limits, wind charge delays, and customized mace/spear balancing.
- **Altar System**: Interactive structures with custom recipe triggers, lightning finishes, concentric particle systems, and live BossBar countdowns.
- **Custom Items**: Native factories for legendary items like the *Warden Heart* and custom *Golden Heads* with customizable regeneration and absorption properties.
- **Strict Anti-Exploits**: Integrated checks for block attributes, item meta, and PacketEvents-driven packet protections.

---

## Commands & Permissions

| Command | Permission | Description |
|---|---|---|
| `/start` | `sculkcore.start` | Begins the SMP event, initializes grace period & world border |
| `/stopgrace` | `sculkcore.start` | Stops the active grace period early |
| `/settings` | `sculkcore.settings` | Opens the graphical admin control panel |
| `/ritual` | `sculkcore.ritual` | Runs a ritual on the held item |
| `/sckit` | `sculkcore.sckit` | Configures and claims user kit packages |
| `/vanish` | `sculkcore.vanish` | Toggles admin vanish and pickup bypass |
| `/saltar` | `sculkcore.saltar` | Spawns or triggers launchers |

---

## Developer Guide & Architecture

A complete breakdown of the project architecture, listeners, and command mappings can be found in the [deobfworkflow.md](deobfworkflow.md) guide.

---

## Build Instructions

### Prerequisites
- Java 21 SDK
- Apache Maven

### Compiling
Run the default Maven lifecycle to build the production shaded JAR:
```bash
clean package
```
The output artifact will be placed in the `target/` directory.
