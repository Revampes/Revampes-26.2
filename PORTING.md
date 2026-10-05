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
