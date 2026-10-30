package xyz.revampes.Revamp.utility.dungeon.map;

import xyz.revampes.Revamp.utility.dungeon.map.DungeonPositions.Component;

import java.util.List;

public class DungeonMapRoomData {

    public final DungeonRoom room;
    public Component predictedAt;
    public List<RoomType> predictedTypes;

    public DungeonMapRoomData(DungeonRoom room) {
        this.room = room;
    }
}
