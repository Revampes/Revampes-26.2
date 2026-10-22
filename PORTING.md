# Porting Frosty-Master modules onto the 26.2 base

This project is a fresh base built from WhatsYouss/Frosty 1.3.0-Beta1 (Minecraft 26.2).
The original fork's source lives in `..\Frosty-master` (untouched) and is the reference
for the fork-unique modules.

## Already ported (from the fork)
- `AutoGFS` (dungeon)
- `KeyHighlight` (dungeon) — `KeyHighlighter`
- `MobHighlight` (dungeon) — `MobHighlighter`
- `Etherwarp` (render)
- `BlockAnimation` (render) — see note below

Supporting pieces ported once: `Module.category.Dungeon`, `settings/impl/ColorSetting`,
`utility/HotbarSwapUtils`, `utility/LocationUtils`, `utility/DungeonUtils` (minimal),
`utility/BlockAnimationUtils`, `utility/skyblock/HeadTextures`.

`AutoFish` and `AutoExperiment` already ship with the base (no port needed).

## How to add another fork module one-by-one
1. Find the fork file under `..\Frosty-master\src\main\java\com\revampes\Fault\modules\impl\...`.
2. Copy it into `src/main/java/xyz/whatsyouss/frosty/modules/impl/<category>/`, then:
   - Rewrite `package com.revampes.Fault...` → `package xyz.whatsyouss.frosty...`.
   - Rewrite imports from `com.revampes.Fault.*` → `xyz.whatsyouss.frosty.*`.
   - Map removed/changed 26.2 API (see the checklist below).
3. If any module-specific utility/setting/event/mixin is missing from the base, port it
   the same way (prefer the base's existing class when one exists — e.g. use the base's
   `RenderUtils.drawBoxFilled/drawBox/drawLine3D`, `ItemUtils.getHeadTexture`, etc.).
4. Register it in `ModuleManager.register()`:
   - add `import ...<Module>;`
   - add `public static <Module> <var> <name>;` field
   - add `this.addModule(<var> = new <Module>());`
5. `./gradlew build` and iterate until green. Commit.

## Common 26.2 API changes to apply when porting
- `mc.world` → `mc.level`
- `mc.interactionManager` → `mc.gameMode`
- `mc.textRenderer` → `mc.font`
- `mc.currentScreen` → `mc.gui.screen()`; `mc.setScreen(x)` → `mc.gui.setScreen(x)`
- `mc.inGameHud` → `mc.gui`
- `getYaw()/getPitch()` → `getYRot()/getXRot()` (on Entity/Player)
- `isOnGround()` → `onGround()`; `isSneaking()` → `isShiftKeyDown()`
- `getBlockPos()` (entity) → `blockPosition()`; `.add(x,y,z)` (BlockPos) → `.offset(x,y,z)`
- `getVelocity()/setVelocity()` → `getDeltaMovement()/setDeltaMovement()`
- `getUuid()` → `getUUID()`; `.isOf(item)` → `.is(item)`
- `swapHand/getMainHandStack/swingInteractionHand` → `getMainHandItem()/getMainHandItem()/swing()`
- `getConnection().getPlayerList()` → `getOnlinePlayers()`; `connection.sendPacket(p)` → `getConnection().send(p)`
- `BlockPos.ofFloored(...)` → `BlockPos.containing(...)`; `BlockPos.Mutable` → `BlockPos.MutableBlockPos`
- `Vec3.ofCenter(...)` → `Vec3.atCenterOf(...)`; `.multiply(double)` → `.scale(double)`
- `mc.level.getEntityById(id)` → `getEntity(id)`; iterate entities via `getEntities(origin, aabb, predicate)`
- `level.raycast(ctx)` → `level.clip(ctx)` (returns `BlockHitResult`)
- `ClipContext.ShapeType.COLLIDER` → `ClipContext.Block.COLLIDER`; `.FluidInteractionHandling.NONE` → `.Fluid.NONE`
- `getStack(int)`/`size()` (Inventory) → `getItem(int)`/`getContainerSize()`
- `NbtCompound/NbtElement` → `CompoundTag`/`Tag`; NBT reads return `Optional`
- `getEquippedStack(slot)` → `getItemBySlot(slot)`
- `state.getOutlineShape(...)` → `state.getShape(...)`; `shape.getBoundingBox()` → `shape.bounds()`
- `keyUse/keyAttack/keySprint` (options) → `keyUse/keyAttack/keySprint` (name change: `useKey`→`keyUse`, etc.); `.isPressed()/.setPressed()/.setKeyPressed()` → `.isDown()/.setDown(...)`
- `ItemStack.getName()` → `getHoverName()`
- `hasMobEffect(...)` → `hasEffect(...)`
- `ServerboundMovePlayerPacket.PositionAndOnGround(pos, onGround, horizColl)` → `PosRot(pos, yRot, xRot, onGround, horizColl)`

## Ported from IQAddons

The IQ modules live in `..\IQ-ver-26.2\IQ-ver-26.2\src\client\java\net\iqaddons\mod` and are
Fabric "features" (a config flag plus event subscriptions). They are mapped onto
`Module` + `@EventHandler` here, with the shared state moved into `utility/kuudra`:

- `FireVeilOverlay` — `features/kuudra/miscellaneous/FireVeilOverlayFeature` +
  `manager/FireVeilOverlayManager` + `features/widgets/FireVeilOverlayWidget`.
- `KuudraHealth` — `KuudraHealthFeature` + `features/widgets/KuudraHealthWidget`. Boss
  tracking lives in `KuudraLocationUtil` / `KuudraBossInfo`, driven by `KuudraState#tickBossCheck`.
- `KuudraHitbox` — `KuudraHitboxFeature` (`KuudraRenderUtil#drawStyledHitbox`).
- `KuudraPhaseAlert` — `alerts/KuudraPhaseAlertFeature`, using the ported
  `HudNotificationEvent` + `hud/impl/KuudraNotificationsWidget` alert.
- `KuudraProfit` — `tracker/KuudraProfitTrackerFeature` + `manager/pricing/*` +
  `manager/calculator/*` + `utils/ChestProfitUtil` + `ChestInteractionDetector` +
  `features/widgets/KuudraProfitTrackerWidget` + `features/widgets/ChestValueWidget` (the
  chest reward values shown while a paid / free chest window is open). State is persisted to
  `config/Frosty/kuudra_profit.json`.
- Pricing: the bazaar bulk feed is unchanged; lowest BIN is resolved on demand from
  `sky.coflnet.com/api/item/price/<id>/bin` because both endpoints IQAddons used are gone
  (`moulberry.codes/lowestbin.json` no longer serves HTTPS, the hosted service answers 401).

Modules whose IQAddons counterpart ships enabled (`KuudraProfit`, `CroesusHelper`,
`FireVeilOverlay`, `KuudraHealth`, `KuudraHitbox`) set `Module#defaultEnabled`, which
`ConfigManager` applies for any module that has no entry in the saved config yet.

`KuudraState` also owns the run lifecycle now (phase durations + `KuudraRunEndEvent`), which
is what lets the profit tracker tell a completed run from a failed or abandoned one.

## HUD widgets

Every on-screen element is a `hud/HudWidget` registered with `HudManager` and drawn by
`HudRenderer`. Positions live in `config/Frosty/hud.json` and are only editable by dragging:
open the ClickGui and press **Edit HUD**, then drag an element (the scroll wheel resizes the
hovered one) and press Esc to save and return. The enabled-module list is itself a widget, so
its drawing moved out of `HUD#onRender2D` into `HUD.ModuleListWidget`.

## World render layers

IQ's camera facing outlines use a `DEBUG_LINE_STRIP` pipeline and its filled shapes a
`TRIANGLES` pipeline with culling off. `ShaderPipelines.WORLD_TRIANGLES` /
`RenderLayers.getTriangles` mirror the latter; the billboard square outline is emitted as
four independent segments on the existing paired `LINES` layer, because that layer keeps the
line-width attribute the strips would lose.

## BlockAnimation (implemented on 26.2)
`BlockAnimation` reproduces the 1.7 sword block animation in both perspectives:

- first person = `ItemRenderer#transformFirstPersonItem(equip, swingProgress)` +
  `doBlockTransformations()`. 26.2 still ships that exact pose as the
  `ItemUseAnimation.BLOCK` branch of `ItemInHandRenderer#submitArmWithItem` (for non-shield
  items), so `mixin/ItemInHandRendererMixin` renders it via `applyItemArmTransform` +
  `applyItemArmAttackTransform` (the live swing progress, which is what makes it 1.7 instead
  of 1.8.9's frozen `0.0F`) + the vanilla block numbers.
- third person = `RenderPlayer#setModelVisibilities` sets `ModelBiped.heldItemRight = 3`.
  The equivalent here is `mixin/AvatarRendererMixin` forcing
  `HumanoidModel.ArmPose.BLOCK` (+ `isUsingItem`) on the arm holding the sword in
  `AvatarRenderer#extractRenderState`, which also covers third person front.

Both are driven by `BlockAnimation#getBlockingHand` (module on, use key held, sword in hand,
no other item in use). Only the local player can be animated: a remote player's right click
is not sent to other clients.

## BetterTerminal (yqloss UI + Odin solver)

`modules/impl/dungeon/BetterTerminal` ports
[Necron-Dev/YqlossClientMixin](https://github.com/Necron-Dev/YqlossClientMixin/tree/master/src/main/kotlin/yqloss/yqlossclientmixinkt/module/betterterminal)
for the visuals and
[odtheking/Odin](https://github.com/odtheking/Odin/tree/main/src/main/kotlin/com/odtheking/odin/features/impl/boss)
for the solver, because the Floor 7 terminals changed with 26.x: panes are per-colour items
instead of `ItemStack` metadata, and a click is only safe to send as `ContainerInput.CLONE`
(a middle click) rather than yqloss' window-id-matched queue.

### Solver side (Odin)
- `utility/dungeon/terminal/TerminalHandler` — Odin's `TerminalHandler`: a `solution` list of
  the slots still to click, a `clickedSlots` prediction queue, first-click protection
  (`firstClickProt` ms + optional lag ticks) and a resolve timeout that drops the queue and
  re-solves when the server never acknowledges a click.
- `terminalhandler` counterparts live in `PanesTerminalHandler`, `NumbersTerminalHandler`,
  `RubixTerminalHandler`, `SelectAllTerminalHandler`, `StartsWithTerminalHandler`,
  `MelodyTerminalHandler`.
- Every click is `mc.gameMode.handleContainerInput(containerId, slot, button, input, player)`:
  `CLONE`/button 2 normally, `PICKUP`/button 1 for Rubix right clicks.
- `ClientPacketListenerMixin` feeds slot updates in *after* the client menu applied them
  (this is Odin's `SetSlotEvent`); hooking the packet on receive would read a stale menu.
  This is only a same-tick fast path — click confirmation is transport-agnostic: `recompute()`
  fingerprints the container (`TerminalItems#signature`) and treats a changed slot as the
  acknowledgement of the clicks queued up to it. That keeps the panel and its prediction queue
  correct even when the menu is mutated locally and no packet is ever sent, which is exactly
  what Odin's terminal simulator does (`TermSimGUI` writes into its own `ChestMenu` after a
  synthesised `ping / 50` tick delay).
- 26.x item lookups live in `TerminalItems`: `Items.STAINED_GLASS_PANE.pick(DyeColor.X)`
  replaces the 1.8.9 metadata ids, and a pane's colour comes from `StainedGlassPaneBlock#getColor`.

### UI side (yqloss)
- `TerminalType` carries yqloss' geometry (`lines` / `beginLine` / `chestLines` plus the
  grid rows/cols), so the panel is laid out exactly like the 1.8.9 one: a 212-wide rounded
  background, a 9-wide grid of 16×16 cells at an 18px pitch, an offset of 8/18 inside the
  panel, and a panel height of `40 + 18 * (lines - beginLine)`.
- The panel is drawn from `ContainerScreenRenderEvent` (posted at the tail of
  `AbstractContainerScreen#extractRenderState`). 26.2 draws a screen in two steps, so hiding
  the vanilla chest needs both `ContainerScreenMixin` (the chest texture comes from
  `ContainerScreen#extractBackground`, called from
  `Screen#extractRenderStateWithTooltipAndSubtitles`) and `AbstractContainerScreenMixin`
  (`extractContents`: labels, slots, highlights). Both scale instead of hiding when
  "Show Vanilla Chest GUI" is on. Tooltips are muted while the panel is up.
- `ContainerMouseClickEvent` (posted at the head of `mouseClicked`) lets the panel consume
  clicks, so the hidden chest slots never see them.
- `SlotType` / `SlotRenderInfo` mirror yqloss' enums; `ExponentialSmooth`, `TerminalColors`
  and the module's `TerminalButton` reproduce the `Button`/`Fade` animation (colour easing,
  the hover/press HSL lightness bump, the 0.9 press scale) and the cross-fading title.
- Melody is the one place the two sources disagree: yqloss assumes six rows while Odin's
  26.x layout has five (target rows 0 and 4, lanes 1-3, buttons at slots 16/25/34). The Odin
  geometry is used for solving *and* drawing, with yqloss' target-marker / active-lane /
  button styling layered on top.

