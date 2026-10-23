package xyz.whatsyouss.frosty.utility.dungeon.map;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import xyz.whatsyouss.frosty.utility.dungeon.map.DungeonPositions.Component;
import xyz.whatsyouss.frosty.utility.dungeon.map.DungeonPositions.WorldComponent;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Ported from devonian ({@code api/dungeon/DungeonDoor.kt}).
 *
 * <p>Doors are probed at a fixed {@code y=69} — the height of every dungeon doorway — where the
 * block in the doorway tells the door type, and air / a barrier means it is already open.
 */
public class DungeonDoor {

    public final WorldComponent comp;
    public int rotation = -1;
    public boolean opened;
    public DoorType type = DoorType.NORMAL;
    public final Set<DungeonRoom> rooms = new LinkedHashSet<>();
    public final Component roomComp1;
    public final Component roomComp2;
    public boolean wasBlood;
    public boolean fairyDoorFlashing;

    public DungeonDoor(WorldComponent comp) {
        this.comp = comp;
        int cx = comp.cx();
        int cz = comp.cz();
        if ((cx & 1) == 1) {
            roomComp1 = new Component((cx - 1) >> 1, cz >> 1);
            roomComp2 = new Component((cx + 1) >> 1, cz >> 1);
        } else {
            roomComp1 = new Component(cx >> 1, (cz - 1) >> 1);
            roomComp2 = new Component(cx >> 1, (cz + 1) >> 1);
        }
    }

    @Override
    public String toString() {
        return "DungeonDoor[type=" + type + ", rotation=" + rotation + ", opened=" + opened + "]";
    }

    public void check() {
        if (opened) return;
        int x = comp.wx();
        int z = comp.wz();
        if (!DungeonScanner.isChunkLoaded(x, z)) return;

        BlockState state = DungeonScanner.getBlockState(x, 69, z);
        if (state == null) return;

        opened = state.isAir() || state.getBlock() == Blocks.BARRIER;

        var block = state.getBlock();
        if (block == Blocks.INFESTED_COBBLESTONE
                || block == Blocks.INFESTED_CHISELED_STONE_BRICKS
                || block == Blocks.INFESTED_CRACKED_STONE_BRICKS
                || block == Blocks.INFESTED_DEEPSLATE
                || block == Blocks.INFESTED_MOSSY_STONE_BRICKS
                || block == Blocks.INFESTED_STONE
                || block == Blocks.INFESTED_STONE_BRICKS) {
            type = DoorType.ENTRANCE;
        } else if (block == Blocks.COAL_BLOCK) {
            type = DoorType.WITHER;
        } else if (block == Blocks.DYED_TERRACOTTA.red()) {
            type = DoorType.BLOOD;
        } else {
            type = DoorType.NORMAL;
        }

        if (!opened && type == DoorType.BLOOD && !wasBlood) wasBlood = true;
        if (opened && fairyDoorFlashing && rooms.stream().anyMatch(room -> !room.explored)) {
            type = DoorType.WITHER;
        }
    }
}
