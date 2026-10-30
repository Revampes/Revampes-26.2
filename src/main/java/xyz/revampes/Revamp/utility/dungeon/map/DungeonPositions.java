package xyz.revampes.Revamp.utility.dungeon.map;

import java.util.ArrayList;
import java.util.List;

public final class DungeonPositions {

    public static final int CORNER_START_X = -200;
    public static final int CORNER_START_Z = -200;
    public static final int ROOM_SIZE = 31;
    public static final int DOOR_SIZE = 1;
    public static final int COMBINED_SIZE = ROOM_SIZE + DOOR_SIZE;
    public static final int HALF_ROOM_SIZE = ROOM_SIZE / 2;
    public static final int HALF_COMBINED_SIZE = COMBINED_SIZE / 2;

    private DungeonPositions() {
    }

    public static boolean isInBounds(int x, int z) {
        return x >= 0 && x <= 11 && z >= 0 && z <= 11;
    }

    public static boolean isValid(int x, int z) {
        return x >= 0 && x <= 10 && z >= 0 && z <= 10;
    }

    public record World(int x, int z) {
        public Component toComponent() {
            return new Component(
                    (x - CORNER_START_X) / HALF_COMBINED_SIZE,
                    (z - CORNER_START_Z) / HALF_COMBINED_SIZE);
        }

        public WorldComponent withComponent() {
            Component comp = toComponent();
            return new WorldComponent(x, z, comp.x(), comp.z());
        }
    }

    public record WorldComponent(int wx, int wz, int cx, int cz) {
        public World toWorld() {
            return new World(wx, wz);
        }

        public Component toComponent() {
            return new Component(cx, cz);
        }
    }

    public record PlayerComponent(double x, double z, double r) {
        public Component toComponent() {
            return new Component((int) x, (int) z);
        }

        public static PlayerComponent fromWorld(double wx, double wz, double r) {
            return new PlayerComponent(
                    (wx - CORNER_START_X) / HALF_COMBINED_SIZE,
                    (wz - CORNER_START_Z) / HALF_COMBINED_SIZE,
                    r);
        }
    }

    public record Neighbor(Component room, Component door) {
    }

    public record Component(int x, int z) {

        public World toWorld() {
            return new World(
                    CORNER_START_X + HALF_ROOM_SIZE + HALF_COMBINED_SIZE * x,
                    CORNER_START_Z + HALF_ROOM_SIZE + HALF_COMBINED_SIZE * z);
        }

        public WorldComponent withWorld() {
            World world = toWorld();
            return new WorldComponent(world.x(), world.z(), x, z);
        }

        public Component toRoom() {
            return new Component(x & ~1, z & ~1);
        }

        public boolean isInBounds() {
            return DungeonPositions.isInBounds(x, z);
        }

        public boolean isValid() {
            return DungeonPositions.isValid(x, z);
        }

        public boolean isValidRoom() {
            return (x & 1) == 0 && (z & 1) == 0;
        }

        public boolean isValidDoor() {
            return ((x & 1) ^ (z & 1)) == 1;
        }

        public List<Component> getNeighboringRooms() {
            if (isValidDoor()) {
                List<Component> rooms = new ArrayList<>(2);
                if ((x & 1) == 1) {
                    rooms.add(new Component(x - 1, z));
                    rooms.add(new Component(x + 1, z));
                } else {
                    rooms.add(new Component(x, z - 1));
                    rooms.add(new Component(x, z + 1));
                }
                rooms.removeIf(comp -> !comp.isValid());
                return rooms;
            }
            return List.of();
        }

        public List<Component> getNeighboringDoors() {
            if (isValidRoom()) {
                List<Component> doors = new ArrayList<>(4);
                doors.add(new Component(x, z - 1));
                doors.add(new Component(x, z + 1));
                doors.add(new Component(x - 1, z));
                doors.add(new Component(x + 1, z));
                doors.removeIf(comp -> !comp.isValid());
                return doors;
            }
            return List.of();
        }

        public List<Neighbor> getNeighbors() {
            if (!isValidRoom()) return List.of();
            List<Neighbor> neighbors = new ArrayList<>(4);
            neighbors.add(new Neighbor(new Component(x, z - 2), new Component(x, z - 1)));
            neighbors.add(new Neighbor(new Component(x, z + 2), new Component(x, z + 1)));
            neighbors.add(new Neighbor(new Component(x - 2, z), new Component(x - 1, z)));
            neighbors.add(new Neighbor(new Component(x + 2, z), new Component(x + 1, z)));
            neighbors.removeIf(neighbor -> !neighbor.room().isValid());
            return neighbors;
        }

        public int getRoomIdx() {
            return (z / 2) * 6 + x / 2;
        }

        public int getDoorIdx() {
            int idx = (((x - 1) >> 1) + 6 * z);
            return idx - idx / 12;
        }
    }
}
