# Fivefold 0.5.1-alpha verification

Target: Minecraft 26.3, NeoForge 26.3.0.39-beta, Java 25; protocol remains 10.
On 2026-10-03, all production sources compiled against the exact patched target API
using tools/build_cached.py and the available dependency cache. All 103
JUnit tests passed (including four new task scheduling regressions). The updated tools/check_mixin_targets.py passed 19 static checks.
The build produced build/libs/fivefold-0.5.1-alpha.jar.

Hand repair attempt:
- Replace the LevelExtractor tail hook with a GameRenderer.renderItemInHand HEAD hook.
  It rebuilds the controlled body's avatar, equipment and use state immediately before
  the scoped spectator-mode gate. The local follower's spectator avatar is replaced.
- Hand selection for bows/crossbows now reads the same explicit equipment/use snapshot
  as the item models, rather than a potentially lagging remote entity state.
- The marker is cleared at each render call and on state reset; only an alive, awake
  shared body viewed in first person qualifies. Personal inventory is not overwritten.
- The scoped redirect still permits native hands for linked followers only. F1,
  third-person and normal spectator behavior retain vanilla rendering gates.

Overlay changes:
- Default compact overlay contains at most three 11-pixel rows, at most 260 GUI pixels
  wide, with a translucent background sized to each line.
- F7 cycles compact/detailed/hidden. F9 opens details or advances the team input page.
- Detailed team input list is capped at 80 GUI pixels in height and paged.
- Overlay respects the native Hud.isHidden() setting.

Task scheduling:
- Full success settles and starts the next task in the same input call. A changed task
  ID triggers an immediate room-state broadcast, including the new 300-tick deadline.
- Partial results still settle at the existing deadline; owner rotation, score pools,
  penalties and paused game-time behavior are retained.
- Tests cover immediate rollover, a fresh 15-second timeout, unchanged unfinished task
  timing, stale packet rejection, held-key protection, owner rotation and pause.

No game launch, runtime Mixin transformation, multiplayer visual verification or
screenshot QA was performed. Existing JUnit tests cover core behavior, not the renderer.
The hand change is a repair attempt requiring in-game confirmation; static targets and
compilation cannot establish that the reported missing-hand symptom is resolved.
See docs/versions/Fivefold-0.5.1-说明.md for the targeted retest sequence.

## Historical 0.5.0 verification


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
