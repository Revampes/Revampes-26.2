package xyz.whatsyouss.frosty.utility.dungeon.map;

import xyz.whatsyouss.frosty.utility.dungeon.map.DungeonPositions.Component;
import xyz.whatsyouss.frosty.utility.dungeon.map.DungeonPositions.Neighbor;

import java.util.ArrayList;
import java.util.List;

/**
 * Ported from devonian ({@code features/dungeons/map/DungeonMapRoomPrediction.kt}).
 *
 * <p>Works out what the still-unopened rooms must be, from the rooms that are already known: the
 * missing trap / puzzle / yellow rooms are spread over the cells that can still be entered.
 *
 * <p>Devonian labels its x loop {@code loop} and jumps to it from inside two nested lambdas; the
 * same early exits are expressed as a {@code skipCell} flag here.
 */
public final class DungeonMapRoomPrediction {

    private DungeonMapRoomPrediction() {
    }

    public static void analyzeRooms(List<DungeonMapRoomData> rooms, DungeonDoor[] doors, FloorType floor,
                                    boolean renderHiddenRooms, int totalPuzzles) {
        boolean hasTrap = floor.floorNum >= 3;
        int puzzleCount = totalPuzzles;

        boolean foundTrap = false;
        int foundPuzzleCount = 0;
        boolean foundYellow = false;

        for (DungeonMapRoomData data : rooms) {
            if (data == null) continue;
            DungeonRoom room = data.room;
            if (room == null || !room.explored) continue;

            switch (room.type) {
                case TRAP -> foundTrap = true;
                case PUZZLE -> foundPuzzleCount++;
                case YELLOW -> foundYellow = true;
                default -> {
                }
            }
        }

        List<RoomType> missingTypes = new ArrayList<>();
        if (hasTrap && !foundTrap) missingTypes.add(RoomType.TRAP);
        if (foundPuzzleCount < puzzleCount) missingTypes.add(RoomType.PUZZLE);
        if (!foundYellow) missingTypes.add(RoomType.YELLOW);

        int missingCount = (hasTrap && !foundTrap ? 1 : 0)
                + (puzzleCount - foundPuzzleCount)
                + (foundYellow ? 0 : 1);

        List<Component> emptySpots = new ArrayList<>();

        for (int z = 0; z < floor.roomsHS; z++) {
            for (int x = 0; x < floor.roomsWS; x++) {
                Component compR = new Component(x * 2, z * 2);
                DungeonMapRoomData data = rooms.get(compR.getRoomIdx());
                if (data == null || data.room == null) continue;

                DungeonRoom room = data.room;
                if (room.explored) continue;

                if (room.type == RoomType.BLOOD) {
                    boolean neighborExplored = room.doors.stream()
                            .anyMatch(door -> door.rooms.stream().anyMatch(other -> other.explored));
                    if (neighborExplored) {
                        data.predictedAt = compR;
                        data.predictedTypes = List.of(RoomType.BLOOD);
                    }
                    continue;
                }

                if (room.doors.stream().anyMatch(door -> door.type != DoorType.NORMAL)) continue;
                if (room.doors.stream().noneMatch(door -> door.rooms.stream().anyMatch(other -> other.explored))) {
                    continue;
                }

                boolean skipCell = false;
                for (Neighbor neighbor : compR.getNeighbors()) {
                    if (neighbor.room().x() >= floor.roomsW * 2) continue;
                    if (neighbor.room().z() >= floor.roomsH * 2) continue;

                    DungeonMapRoomData neighborData = rooms.get(neighbor.room().getRoomIdx());
                    if (neighborData == null || neighborData.room == null) {
                        skipCell = true;
                        break;
                    }
                    if (neighborData.room.explored) continue;

                    boolean reachable = false;
                    for (Neighbor second : neighbor.room().getNeighbors()) {
                        int doorIdx = second.door().getDoorIdx();
                        if (doorIdx < 0 || doorIdx >= doors.length) continue;
                        if (doors[doorIdx] == null) continue;

                        DungeonMapRoomData roomData = rooms.get(second.room().getRoomIdx());
                        if (roomData != null && roomData.room != null && roomData.room.explored) {
                            reachable = true;
                            break;
                        }
                    }
                    if (!reachable) {
                        skipCell = true;
                        break;
                    }
                }

                if (!skipCell) emptySpots.add(compR);
            }
        }

        for (int z = 0; z < floor.roomsH; z++) {
            for (int x = floor.roomsWS - 1; x < floor.roomsW - 1; x++) {
                Component compD = new Component(x * 2 + 1, z * 2);
                int doorIdx = compD.getDoorIdx();
                if (doorIdx < 0 || doorIdx >= doors.length) continue;
                if (doors[doorIdx] == null) continue;

                DungeonMapRoomData data1 = rooms.get(new Component(x * 2, z * 2).getRoomIdx());
                if (data1 == null || data1.room == null || !data1.room.explored) continue;

                Component compR2 = new Component(x * 2 + 2, z * 2);
                DungeonMapRoomData data2 = rooms.get(compR2.getRoomIdx());
                if (data2 == null || data2.room == null || data2.room.explored) continue;

                emptySpots.add(compR2);
            }
        }

        if (emptySpots.size() > missingCount && puzzleCount == 5) missingTypes.add(RoomType.RARE);

        for (Component comp : emptySpots) {
            DungeonMapRoomData data = rooms.get(comp.getRoomIdx());
            if (data == null || data.room == null) continue;
            if (renderHiddenRooms && data.room.type != RoomType.UNKNOWN) continue;

            data.predictedAt = comp;
            data.predictedTypes = missingTypes;
        }
    }
}
