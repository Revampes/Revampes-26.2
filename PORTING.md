# Porting Frosty-Master modules onto the 26.2 base

This project is a fresh base built from WhatsYouss/Frosty 1.3.0-Beta1 (Minecraft 26.2).
The original fork's source lives in `..\Frosty-master` (untouched) and is the reference
for the fork-unique modules.

## Already ported (from the fork)
- `AutoGFS` (dungeon)
- `KeyHighlight` (dungeon) — `KeyHighlighter`
- `MobHighlight` (dungeon) — `MobHighlighter`
- `Etherwarp` (render)
- `BlockAnimation` (render, state/toggle only — see note below)

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

## BlockAnimation visual-effect note
The first-person sword-block *renderer* was already commented out in the fork and the 26.2
base has no held-item (first-person) render hook. The `BlockAnimation` module and its
`BlockAnimationUtils` are ported and compile, but the visual animation requires a 26.2
held-item mixin that has not been implemented yet. State tracking works; visuals are pending.
