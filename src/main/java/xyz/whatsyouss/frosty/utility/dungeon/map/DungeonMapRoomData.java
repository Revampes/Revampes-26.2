package xyz.whatsyouss.frosty.utility.dungeon.map;

import xyz.whatsyouss.frosty.utility.dungeon.map.DungeonPositions.Component;

import java.util.List;

/**
 * Ported from devonian ({@code features/dungeons/map/DungeonMapRoomRenderData.kt}).
 *
 * <p>{@code predictedTypes} is filled in by {@link DungeonMapRoomPrediction} and only applies to
 * the component it was predicted for.
 */
public class DungeonMapRoomData {

    public final DungeonRoom room;
    public Component predictedAt;
    public List<RoomType> predictedTypes;

    public DungeonMapRoomData(DungeonRoom room) {
        this.room = room;
    }
}
