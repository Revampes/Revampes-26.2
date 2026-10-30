package xyz.whatsyouss.frosty.utility.dungeon.map;

import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import xyz.whatsyouss.frosty.utility.dungeon.map.DungeonPositions.Component;
import xyz.whatsyouss.frosty.utility.dungeon.map.DungeonPositions.PlayerComponent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static xyz.whatsyouss.frosty.Frosty.mc;

public final class DungeonMapScanner {

    private static final int COLOR_SIZE = 16384;
    private static final int SCAN = 128;
    private static final int ROOM_SPACING = 4;

    public static int roomSize = -1;
    public static int roomGap = -1;
    public static int mapOffsetX = -1;
    public static int mapOffsetZ = -1;
    public static int mapWidth = -1;
    public static int mapHeight = -1;

    private static final Set<Component> unscannedDoors = new HashSet<>();
    private static MapId lastMapId;
    private static long swapCooldown;

    private DungeonMapScanner() {
    }

    private enum MapColors {
        EMPTY(0),

        CHECK_WHITE(34),
        CHECK_GREEN(30),
        CHECK_FAIL(18),
        CHECK_UNKNOWN(119),

        ROOM_ENTRANCE(30),
        ROOM_NORMAL(63),
        ROOM_UNOPENED(85),
        ROOM_TRAP(62),
        ROOM_BOSS(74),
        ROOM_PUZZLE(66),
        ROOM_FAIRY(82),
        ROOM_BLOOD(18),

        DOOR_WITHER(119),
        DOOR_BLOOD(18);

        private final byte color;

        MapColors(int color) {
            this.color = (byte) color;
        }
    }

    public static void reset() {
        roomSize = -1;
        roomGap = -1;
        mapOffsetX = -1;
        mapOffsetZ = -1;
        mapWidth = -1;
        mapHeight = -1;

        unscannedDoors.clear();
        for (int x = 0; x <= 10; x++) {
            for (int z = ((x & 1) ^ 1); z <= 10; z += 2) {
                unscannedDoors.add(new Component(x, z));
            }
        }

        MapId id = lastMapId;
        if (id != null && mc.level != null) {
            try {
                mc.level.overrideMapData(id, null);
            } catch (Exception e) {
                System.err.println("Frosty: failed to drop dungeon map data: " + e.getMessage());
            }
        }
        lastMapId = null;
        swapCooldown = System.currentTimeMillis() + 6000L;
    }

    public static void onMapPacket(ClientboundMapItemDataPacket packet) {
        if (!DungeonTracker.active) return;
        if (mc.level == null) return;
        if (DungeonTracker.inBoss) return;

        MapId mapId = packet.mapId();
        if ((mapId.id() & 1000) != 0) return;

        MapItemSavedData data = MapItem.getSavedData(mapId, mc.level);
        if (data == null || data.colors == null) return;

        byte[] colors = data.colors.clone();
        if (colors.length < COLOR_SIZE) return;
        if (colors[0] != MapColors.EMPTY.color) lastMapId = mapId;

        if (roomSize == -1 && !scanMapDimensions(colors)) return;
        if (System.currentTimeMillis() < swapCooldown) return;

        updatePlayerIcons(data);
        updateRooms(colors);
    }

    private static boolean scanMapDimensions(byte[] colors) {
        FloorType floor = DungeonTracker.floor;
        if (floor == FloorType.None) return false;

        int entranceIdx = 0;
        int i = 0;
        while (entranceIdx < colors.length && colors[entranceIdx] != MapColors.ROOM_ENTRANCE.color) {
            i++;
            entranceIdx = ((i & 7) << 4) + ((i >> 3) << 11);
        }
        if (entranceIdx >= colors.length) return false;

        int l = entranceIdx;
        int r = entranceIdx;
        while (l > 0 && colors[l - 1] == MapColors.ROOM_ENTRANCE.color) l--;
        while (r < colors.length - 1 && colors[r + 1] == MapColors.ROOM_ENTRANCE.color) r++;

        int t = entranceIdx;
        int b = entranceIdx;
        while (t - SCAN >= 0 && colors[t - SCAN] == MapColors.ROOM_ENTRANCE.color) t -= SCAN;
        while (b + SCAN < colors.length && colors[b + SCAN] == MapColors.ROOM_ENTRANCE.color) b += SCAN;

        l = l & 127;
        r = r & 127;
        t = t >> 7;
        b = b >> 7;

        roomSize = r - l + 1;
        roomGap = roomSize + ROOM_SPACING;

        mapOffsetX = l % roomGap;
        mapOffsetZ = t % roomGap;

        mapWidth = roomGap * (floor.roomsW - 1) + roomSize;
        mapHeight = roomGap * (floor.roomsH - 1) + roomSize;

        if (SCAN - mapWidth >= roomGap * 2) mapOffsetX += roomGap;
        if (SCAN - mapHeight >= roomGap * 2) mapOffsetZ += roomGap;

        return true;
    }

    private static void updatePlayerIcons(MapItemSavedData data) {
        FloorType floor = DungeonTracker.floor;
        if (floor == FloorType.None) return;
        if (roomGap <= 0) return;

        // Hypixel does not send a decoration for the local player, so the decoration list maps
        // onto the rest of the party in tab-list order.
        List<DungeonPlayer> teammates = new ArrayList<>();
        for (DungeonPlayer player : DungeonTracker.players.values()) {
            if (player == DungeonTracker.selfPlayer || player.isDead || player.isDisconnected) continue;
            // Players we can see client side are tracked from their entity, which is both more
            // accurate and keeps the marker from jumping between the two position sources.
            if (player.entity != null && !player.entity.isRemoved()) continue;
            teammates.add(player);
        }

        List<MapDecoration> decorations = new ArrayList<>();
        for (MapDecoration decoration : data.getDecorations()) {
            if (decoration == null) continue;
            if (decoration.type() != null
                    && decoration.type().value() == MapDecorationTypes.FRAME.value()) continue;
            decorations.add(decoration);
        }

        if (teammates.isEmpty() || decorations.isEmpty()) return;

        // Tolerate a count mismatch: a teammate dying or leaving can leave a stale decoration
        // behind for a moment, and bailing out here used to drop every marker on the map.
        int count = Math.min(decorations.size(), teammates.size());
        for (int i = 0; i < count; i++) {
            MapDecoration decoration = decorations.get(i);
            DungeonPlayer player = teammates.get(i);

            double x = MapMath.rescale(
                    (decoration.x() + 128.0) * 0.5,
                    mapOffsetX, mapOffsetX + roomGap * floor.roomsW,
                    0.0, floor.roomsW * 2.0);
            double z = MapMath.rescale(
                    (decoration.y() + 128.0) * 0.5,
                    mapOffsetZ, mapOffsetZ + roomGap * floor.roomsH,
                    0.0, floor.roomsH * 2.0);
            double r = -Math.toRadians(decoration.rot() / 16.0 * 360.0 + 90.0);

            player.updatePosition(new PlayerComponent(x, z, r));
        }
    }

    private static void updateRooms(byte[] colors) {
        if (colors.length < COLOR_SIZE) return;
        if (colors[0] != MapColors.EMPTY.color) return;

        Set<DungeonRoom> visited = new HashSet<>();
        for (int idx = 0; idx < DungeonScanner.rooms.length; idx++) {
            DungeonRoom existing = DungeonScanner.rooms[idx];
            if (existing != null && !visited.add(existing)) continue;

            int x = idx % 6;
            int z = idx / 6;
            int mapRoomX = mapOffsetX + x * roomGap;
            int mapRoomZ = mapOffsetZ + z * roomGap;
            int mapCenterX = mapRoomX + roomSize / 2 - 1;
            int mapCenterZ = mapRoomZ + roomSize / 2 - 1 + 2;
            int roomIdx = mapRoomX + mapRoomZ * SCAN;
            int centerIdx = mapCenterX + mapCenterZ * SCAN;

            if (roomIdx >= colors.length || centerIdx >= colors.length) continue;
            byte roomCol = colors[roomIdx];
            byte centerCol = colors[centerIdx];

            if (roomCol == MapColors.EMPTY.color) continue;

            DungeonRoom room;
            if (existing == null) {
                Component comp = new Component(x * 2, z * 2);
                room = new DungeonRoom(new ArrayList<>(List.of(comp.withWorld())), 0);
                DungeonScanner.addRoom(comp, room);
            } else {
                room = existing;
            }

            if (roomCol == MapColors.ROOM_ENTRANCE.color) room.type = RoomType.ENTRANCE;
            else if (roomCol == MapColors.ROOM_BLOOD.color) room.type = RoomType.BLOOD;
            else if (roomCol == MapColors.ROOM_BOSS.color) room.type = RoomType.YELLOW;
            else if (roomCol == MapColors.ROOM_FAIRY.color) room.type = RoomType.FAIRY;
            else if (roomCol == MapColors.ROOM_NORMAL.color) {
                if (room.type != RoomType.RARE) room.type = RoomType.NORMAL;
            } else if (roomCol == MapColors.ROOM_PUZZLE.color) room.type = RoomType.PUZZLE;
            else if (roomCol == MapColors.ROOM_TRAP.color) room.type = RoomType.TRAP;
            else room.type = RoomType.UNKNOWN;

            if (!room.explored || room.clientExplored) {
                boolean nowExplored = roomCol != MapColors.ROOM_UNOPENED.color;
                if (room.clientExplored) {
                    boolean canUpdate = DungeonScanner.ticks >= room.lastClient;
                    if (canUpdate) room.explored = nowExplored;
                    room.clientExplored = !canUpdate;
                } else {
                    room.explored = nowExplored;
                }
            }

            if (room.checkmark != CheckmarkType.GREEN) {
                if (roomCol == centerCol) {
                    room.checkmark = CheckmarkType.NONE;
                } else if (centerCol == MapColors.CHECK_WHITE.color) {
                    room.checkmark = CheckmarkType.WHITE;
                } else if (centerCol == MapColors.CHECK_GREEN.color) {
                    room.checkmark = CheckmarkType.GREEN;
                } else if (centerCol == MapColors.CHECK_FAIL.color) {
                    room.checkmark = CheckmarkType.FAILED;
                } else if (centerCol == MapColors.CHECK_UNKNOWN.color) {
                    room.checkmark = room.checkmark == CheckmarkType.NONE
                            ? CheckmarkType.NONE : CheckmarkType.UNEXPLORED;
                } else {
                    room.checkmark = CheckmarkType.NONE;
                }
            }

            // A green check means every secret in the room has been found, so trust it even when
            // the action bar never reported the count (or the room data has no secret total).
            if (room.checkmark == CheckmarkType.GREEN && room.totalSecrets > 0
                    && room.secretsCompleted != room.totalSecrets) {
                room.secretsCompleted = room.totalSecrets;
            }
        }

        Iterator<Component> doorIterator = unscannedDoors.iterator();
        while (doorIterator.hasNext()) {
            Component comp = doorIterator.next();
            int idx = comp.getDoorIdx();

            int mapJointX = mapOffsetX + (comp.x() / 2) * roomGap + (comp.x() & 1) * roomSize;
            int mapJointZ = mapOffsetZ + (comp.z() / 2) * roomGap + (comp.z() & 1) * roomSize;
            int mapDoorX = mapJointX + (comp.z() & 1) * roomSize / 2;
            int mapDoorZ = mapJointZ + (comp.x() & 1) * roomSize / 2;
            int jointIdx = mapJointX + mapJointZ * SCAN;
            int doorIdx = mapDoorX + mapDoorZ * SCAN;

            if (jointIdx >= colors.length || doorIdx >= colors.length) continue;
            byte joinedCol = colors[jointIdx];
            byte doorCol = colors[doorIdx];

            if (doorCol == MapColors.EMPTY.color) continue;

            if (joinedCol == doorCol) {
                if (idx >= 0 && idx < DungeonScanner.doors.length) {
                    DungeonDoor existing = DungeonScanner.doors[idx];
                    if (existing != null) {
                        for (DungeonRoom room : existing.rooms) room.doors.remove(existing);
                        DungeonScanner.doors[idx] = null;
                    }
                }

                List<Component> neighboringRooms = comp.getNeighboringRooms();
                if (neighboringRooms.size() == 2
                        && DungeonScanner.mergeRooms(neighboringRooms.get(0), neighboringRooms.get(1))) {
                    doorIterator.remove();
                }
                continue;
            }

            DungeonDoor door;
            if (idx >= 0 && idx < DungeonScanner.doors.length) door = DungeonScanner.doors[idx];
            else continue;

            if (door == null) {
                door = new DungeonDoor(comp.withWorld());
                DungeonScanner.addDoor(door);
            }

            if (doorCol == MapColors.DOOR_WITHER.color) {
                door.type = DoorType.WITHER;
                door.opened = false;
                for (Component roomComp : comp.getNeighboringRooms()) {
                    int roomIdx = roomComp.getRoomIdx();
                    if (roomIdx < 0 || roomIdx >= DungeonScanner.rooms.length) continue;
                    DungeonRoom room = DungeonScanner.rooms[roomIdx];
                    if (room != null && room.type == RoomType.FAIRY && !room.explored) {
                        door.fairyDoorFlashing = true;
                    }
                }
                continue;
            }

            if (doorCol == MapColors.DOOR_BLOOD.color) {
                door.type = DoorType.BLOOD;
                doorIterator.remove();
                continue;
            }

            door.type = DoorType.NORMAL;
            door.opened = true;
            doorIterator.remove();
        }
    }
}
