package xyz.revampes.Revamp.utility.dungeon.map;

import com.google.gson.Gson;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import xyz.revampes.Revamp.utility.dungeon.map.DungeonPositions.Component;
import xyz.revampes.Revamp.utility.dungeon.map.DungeonPositions.Neighbor;
import xyz.revampes.Revamp.utility.dungeon.map.DungeonPositions.World;
import xyz.revampes.Revamp.utility.dungeon.map.DungeonPositions.WorldComponent;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static xyz.revampes.Revamp.Frosty.mc;

public final class DungeonScanner {

    public static final class RoomData {
        public String name;
        public String type;
        public int secrets;
        public List<Integer> cores = List.of();
        public int roomID;
        public String clear;
        public int crypts;
        public String shape;
    }

    public static final List<RoomData> ROOMS_DATA = loadRoomsData();

    public static final DungeonRoom[] rooms = new DungeonRoom[36];
    public static final DungeonDoor[] doors = new DungeonDoor[60];

    public static Integer lastIdx;
    public static DungeonRoom currentRoom;
    public static List<WorldComponent> availablePos = findAvailablePos();

    private static int worldChangeCooldown = 20;
    private static int foundEntrance = 20;
    private static boolean wasInEntrance;

    public static long ticks;

    private DungeonScanner() {
    }

    private static List<RoomData> loadRoomsData() {
        try (InputStream stream = DungeonScanner.class.getResourceAsStream("/assets/frosty/dungeons/rooms.json")) {
            if (stream == null) {
                System.err.println("Frosty: could not find dungeons/rooms.json");
                return List.of();
            }
            RoomData[] data = new Gson().fromJson(
                    new InputStreamReader(stream, StandardCharsets.UTF_8), RoomData[].class);
            if (data == null) return List.of();
            for (RoomData room : data) {
                if (room.cores == null) room.cores = List.of();
            }
            return List.of(data);
        } catch (Exception e) {
            System.err.println("Frosty: failed to read dungeons/rooms.json: " + e.getMessage());
            return List.of();
        }
    }

    public static boolean isChunkLoaded(int x, int z) {
        if (mc.level == null) return false;
        return mc.level.isLoaded(new BlockPos(x, 0, z));
    }

    public static BlockState getBlockState(int x, int y, int z) {
        if (mc.level == null) return null;
        if (y < mc.level.getMinY() || y >= mc.level.getMaxY()) return null;
        return mc.level.getBlockState(new BlockPos(x, y, z));
    }

    public static String registryName(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block).toString();
    }

    public static int getBlockId(Block block) {
        Integer id = LegacyRegistry.BLOCKS.get(registryName(block));
        return id == null ? 0 : id;
    }

    private static Integer getLegacyId(BlockState state) {
        Block block = state.getBlock();
        String name = registryName(block);

        FluidState fluid = state.getFluidState();
        if (!fluid.isEmpty()) {
            if (fluid.is(FluidTags.WATER)) return fluid.isSource() ? 9 : 8;
            if (fluid.is(FluidTags.LAVA)) return fluid.isSource() ? 11 : 10;
        }

        if (block instanceof SlabBlock) {
            name += "[type=" + state.getValue(SlabBlock.TYPE).name().toLowerCase(java.util.Locale.ROOT) + "]";
        }
        return LegacyRegistry.BLOCKS.get(name);
    }

    public static int getHighestY(int x, int z) {
        if (mc.level == null) return -1;
        int maxY = Math.min(255, mc.level.getMaxY() - 1);
        for (int y = maxY; y >= 0; y--) {
            BlockState state = getBlockState(x, y, z);
            if (state == null) continue;
            if (state.isAir() || state.getBlock() == Blocks.GOLD_BLOCK) continue;
            return y;
        }
        return 0;
    }

    public static int hashCeil(int x, int z) {
        StringBuilder builder = new StringBuilder();

        for (int y = 140; y >= 12; y--) {
            BlockState state = getBlockState(x, y, z);
            if (state == null) continue;
            Block block = state.getBlock();
            Integer id = getLegacyId(state);
            if (id == null) continue;
            if (block == Blocks.IRON_BARS || block == Blocks.CHEST) {
                builder.append('0');
                continue;
            }
            builder.append(id);
        }

        return builder.toString().hashCode();
    }

    private static List<WorldComponent> findAvailablePos() {
        List<WorldComponent> positions = new ArrayList<>();
        for (int z = 0; z <= 10; z++) {
            for (int x = 0; x <= 10; x++) {
                if (x % 2 != 0 && z % 2 != 0) continue;
                positions.add(new Component(x, z).withWorld());
            }
        }
        return positions;
    }

    public static void reset() {
        worldChangeCooldown = 5;
        foundEntrance = 5;
        wasInEntrance = false;
        Arrays.fill(rooms, null);
        Arrays.fill(doors, null);
        lastIdx = null;
        currentRoom = null;
        availablePos = new ArrayList<>(findAvailablePos().reversed());
    }

    public static void tick() {
        ticks++;
        if (--worldChangeCooldown >= 0) return;
        if (DungeonTracker.inBoss) return;
        if (mc.player == null) return;

        double px = mc.player.getX();
        double pz = mc.player.getZ();
        if (!isChunkLoaded((int) px, (int) pz)) return;

        Component comp = new World((int) px, (int) pz).toComponent();
        if (!comp.isInBounds()) return;

        int jdx = comp.getRoomIdx();
        if (jdx < 0 || jdx > 35) return;

        scan();

        DungeonRoom newRoom = rooms[jdx];
        if (!wasInEntrance) {
            if (newRoom != null && newRoom.type == RoomType.ENTRANCE) wasInEntrance = true;
            else if (foundEntrance > 0) return;
        }

        for (DungeonRoom room : rooms) {
            if (room == null || room.rotation != -1) continue;
            room.findRotation();
        }
        for (DungeonDoor door : doors) {
            if (door == null || door.opened) continue;
            door.check();
        }

        DungeonRoom room = rooms[jdx];
        currentRoom = room;
        if (room != null) {
            if (!room.clientExplored && !room.explored) {
                room.clientExplored = true;
                room.explored = true;
                room.lastClient = ticks + 10;
            }
            if (room.checkmark == CheckmarkType.UNEXPLORED) {
                room.checkmark = CheckmarkType.NONE;
            }
        }

        if (lastIdx != null && lastIdx == jdx) return;
        lastIdx = jdx;
        if (currentRoom != null) DungeonTracker.currentRoom = currentRoom;
    }

    public static boolean scan() {
        foundEntrance--;
        if (availablePos.isEmpty()) return false;

        int startLen = availablePos.size();
        var iterator = availablePos.iterator();
        while (iterator.hasNext()) {
            WorldComponent pos = iterator.next();
            int wx = pos.wx();
            int wz = pos.wz();
            Component comp = pos.toComponent();
            if (!isChunkLoaded(wx, wz)) continue;

            int roofHeight = getHighestY(wx, wz);
            if (roofHeight < 0) {
                iterator.remove();
                continue;
            }

            boolean remove = true;

            if (comp.isValidDoor()) {
                if (roofHeight != 0 && roofHeight < 85) {
                    DungeonDoor door = new DungeonDoor(pos);
                    if (wz % 2 == 1) door.rotation = 0;
                    addDoor(door);
                }
            } else if (roofHeight <= 0) {

            } else {
                DungeonRoom room = new DungeonRoom(new ArrayList<>(List.of(pos)), roofHeight).scan();
                addRoom(comp, room);

                if (room.type == RoomType.ENTRANCE) {
                    room.explored = true;
                    room.checkmark = CheckmarkType.NONE;
                    foundEntrance = 0;
                } else if (foundEntrance > 0) {
                    remove = false;
                }

                if (remove) {
                    for (Neighbor neighbor : comp.getNeighbors()) {
                        WorldComponent doorPos = neighbor.door().withWorld();
                        int nx = doorPos.wx();
                        int nz = doorPos.wz();

                        BlockState heightBlock = getBlockState(nx, roofHeight, nz);
                        if (heightBlock == null) continue;
                        BlockState aboveHeightBlock = getBlockState(nx, roofHeight + 1, nz);
                        if (aboveHeightBlock == null) continue;

                        int heightBlockId = getBlockId(heightBlock.getBlock());
                        int aboveHeightId = getBlockId(aboveHeightBlock.getBlock());

                        if (room.type == RoomType.ENTRANCE && heightBlockId != 0) {
                            BlockState aboveDoor = getBlockState(nx, 76, nz);
                            if (aboveDoor == null || aboveDoor.isAir()) continue;
                            int doorIdx = neighbor.door().getDoorIdx();
                            if (doorIdx >= 0 && doorIdx <= 60) {
                                DungeonDoor door = new DungeonDoor(doorPos);
                                door.type = DoorType.ENTRANCE;
                                addDoor(door);
                            }
                            continue;
                        }

                        if (heightBlockId == 0 || aboveHeightId != 0) continue;

                        int ndx = neighbor.room().getRoomIdx();
                        if (ndx < 0 || ndx > 35) continue;

                        DungeonRoom neighborRoom = rooms[ndx];
                        if (neighborRoom == null) {
                            room.addComponent(neighbor.room());
                            addRoom(neighbor.room(), room);
                            continue;
                        }

                        if (neighborRoom.type == RoomType.ENTRANCE || neighborRoom == room) continue;

                        mergeRooms(neighborRoom, room);
                        room = neighborRoom;
                    }
                }
            }

            if (remove) iterator.remove();
        }

        return availablePos.size() != startLen;
    }

    public static void addDoor(DungeonDoor door) {
        Component comp = door.comp.toComponent();
        int idx = comp.getDoorIdx();
        if (idx < 0 || idx > 59) return;

        doors[idx] = door;
        for (Component roomComp : comp.getNeighboringRooms()) {
            int roomIdx = roomComp.getRoomIdx();
            if (roomIdx < 0 || roomIdx >= rooms.length) continue;
            DungeonRoom room = rooms[roomIdx];
            if (room == null) continue;
            room.doors.add(door);
            door.rooms.add(room);
        }

    }

    public static void addRoom(Component comp, DungeonRoom room) {
        addRoom(comp, room, false);
    }

    public static void addRoom(Component comp, DungeonRoom room, boolean force) {
        int idx = comp.getRoomIdx();
        if (idx < 0 || idx > 35) return;

        if (!force && rooms[idx] != null) {
            DungeonRoom existing = rooms[idx];
            if (room.name == null) mergeRooms(existing, room);
            else mergeRooms(room, existing);
            return;
        }
        rooms[idx] = room;

        for (Component doorComp : comp.getNeighboringDoors()) {
            int doorIdx = doorComp.getDoorIdx();
            if (doorIdx < 0 || doorIdx >= doors.length) continue;
            DungeonDoor door = doors[doorIdx];
            if (door == null) continue;
            door.rooms.add(room);
            room.doors.add(door);
        }

    }

    public static boolean mergeRooms(Component comp1, Component comp2) {
        int i1 = comp1.getRoomIdx();
        int i2 = comp2.getRoomIdx();
        DungeonRoom r1 = rooms[i1];
        DungeonRoom r2 = rooms[i2];

        if (r1 != null && r2 != null) {
            if (i1 < i2) mergeRooms(r1, r2);
            else mergeRooms(r2, r1);
            return true;
        }
        if (r1 == null && r2 == null) return false;

        DungeonRoom room;
        Component comp;
        if (r1 == null) {
            room = r2;
            comp = comp1;
        } else {
            room = r1;
            comp = comp2;
        }

        room.addComponent(comp);
        addRoom(comp, room);
        return true;
    }

    public static void mergeRooms(DungeonRoom room1, DungeonRoom room2) {
        if (room1 == room2) return;
        if (room1.type == RoomType.ENTRANCE || room2.type == RoomType.ENTRANCE) return;

        for (WorldComponent comp : new ArrayList<>(room2.comps)) {
            Component component = comp.toComponent();
            room1.addComponent(component, false);
            addRoom(component, room1, true);
        }

        room1.update();
        if (room2.explored) room1.explored = true;

        for (DungeonDoor door : room2.doors) door.rooms.remove(room2);
    }

    public static Optional<DungeonRoom> roomAt(int idx) {
        if (idx < 0 || idx >= rooms.length) return Optional.empty();
        return Optional.ofNullable(rooms[idx]);
    }
}
