# Fivefold 0.5.0-alpha verification

Target: Minecraft 26.3, NeoForge 26.3.0.39-beta, Java 25; protocol 10.
All production sources compiled against the exact patched target API.
The supplied source archive reports 99 JUnit tests passed (23 added since 0.4.1).
The supplied source archive reports 18 bytecode target checks passed.
Raw logs and XML reports are retained locally and excluded from Git because they contain
build environment metadata. These historical checks were not rerun during publication.
These are compilation, pure logic/concurrency/geometry, and static target checks.
No actual game launch, runtime Mixin application, renderer screenshot, villager transaction,
multiplayer hand animation, underwater HUD, or client/server Wire round-trip test was performed.

Concrete findings:
- The old native-screen allowlist omitted merchant, and the state wire omitted MerchantOffers.
  The new mirror uses the native screen and offers stream codec, with server-owned trade selection.
- ServerPlayer.drop sets its remote slot cache under the vanilla prediction assumption.
  Remote-triggered drops now explicitly send ClientboundSetPlayerInventoryPacket to the body.
- Vanilla Hud chooses the spectator hotbar and suppresses survival layers for spectator clients.
  The scoped follower hook now calls vanilla camera-player HUD routines and uses explicit snapshots.
- Hand snapshots now carry main/offhand equipment and active-use data to all followers,
  independently of LOOK permission. Render state uses the explicit snapshot rather than relying
  solely on vanilla spectator equipment replication. Runtime visual success remains unverified.

Challenge authority:
- Raw challenge-held bits never feed movement, use or attack authority.
- Server checks session/epoch/revision and current task, tracks rising edges and a rolling window.
- Integer half-points avoid fractional rounding. Reward pool clears on >=4 points; punishment
  threshold is 4 and subtracts 2. State sent to each peer contains own penalty and leaders only.
- Pausing freezes game-time deadlines and clears current responses. Existing best is retained.
- Permission reassignment with the same members retains the active task and score.
- Tests exercise old IDs, held input, selected-owner requirement, 25% boundary, late responses,
  partial-to-full promotion, reward overflow, penalties, rotation and native offer hit geometry.

Remaining limitations are documented in docs/versions/Fivefold-0.5.0-说明.md. Native GUI delegation continues to use
isolated mirror menus; only the server body's menu performs actual inventory/trading actions.
