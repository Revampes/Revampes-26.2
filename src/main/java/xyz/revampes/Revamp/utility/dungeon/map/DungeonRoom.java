package xyz.revampes.Revamp.utility.dungeon.map;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import xyz.revampes.Revamp.utility.dungeon.map.DungeonPositions.Component;
import xyz.revampes.Revamp.utility.dungeon.map.DungeonPositions.World;
import xyz.revampes.Revamp.utility.dungeon.map.DungeonPositions.WorldComponent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class DungeonRoom {

    public static final List<World> ROOM_OFFSETS = List.of(
            new World(-DungeonPositions.HALF_ROOM_SIZE, -DungeonPositions.HALF_ROOM_SIZE),
            new World(DungeonPositions.HALF_ROOM_SIZE, -DungeonPositions.HALF_ROOM_SIZE),
            new World(DungeonPositions.HALF_ROOM_SIZE, DungeonPositions.HALF_ROOM_SIZE),
            new World(-DungeonPositions.HALF_ROOM_SIZE, DungeonPositions.HALF_ROOM_SIZE)
    );

    public final List<WorldComponent> comps = new ArrayList<>();
    private final List<PossibleCorner> possibleCorners = new ArrayList<>();

    public List<Integer> cores = List.of();
    public boolean explored;
    public boolean clientExplored;
    public long lastClient = -1;
    public String name;
    public Integer roomID;
    public World corner = new World(Integer.MIN_VALUE, Integer.MIN_VALUE);
    public int rotation = -1;
    public RoomType type = RoomType.UNKNOWN;
    public CheckmarkType checkmark = CheckmarkType.UNEXPLORED;
    public ShapeType shape = ShapeType.Shape1x1;
    public int totalSecrets;
    public int secretsCompleted = -1;
    public ClearType clear = ClearType.MOB;
    public final Set<DungeonDoor> doors = new LinkedHashSet<>();
    private String shapeIn = "";

    public int height;

    public DungeonRoom(List<WorldComponent> comps, int height) {
        this.height = height;
        List<Component> componentList = new ArrayList<>(comps.size());
        for (WorldComponent comp : comps) componentList.add(comp.toComponent());
        addComponents(componentList);
    }

    private record PossibleCorner(int idx, WorldComponent comp, World pos) {
    }

    @Override
    public String toString() {
        return "DungeonRoom[name=" + name + ", type=" + type + ", rotation=" + rotation + ", shape=" + shape + "]";
    }

    private void loadFromData(DungeonScanner.RoomData data) {
        cores = data.cores;
        name = data.name;
        roomID = data.roomID;
        type = RoomType.byName(data.type) == null ? RoomType.NORMAL : RoomType.byName(data.type);
        if ("mob".equals(data.clear)) clear = ClearType.MOB;
        else if ("miniboss".equals(data.clear)) clear = ClearType.MINIBOSS;
        else clear = ClearType.OTHER;
        totalSecrets = data.secrets;
        shapeIn = data.shape == null ? "" : data.shape;
    }

    private boolean loadFromCore(int core) {
        for (DungeonScanner.RoomData room : DungeonScanner.ROOMS_DATA) {
            if (!room.cores.contains(core)) continue;
            loadFromData(room);
            return true;
        }
        return false;
    }

    public void update() {
        comps.sort(Comparator.comparingInt(WorldComponent::cx).thenComparingInt(WorldComponent::cz));
        scan();
        shape();
    }

    public DungeonRoom scan() {
        checkmark = CheckmarkType.UNEXPLORED;
        for (WorldComponent comp : comps) {
            int x = comp.wx();
            int z = comp.wz();
            if (!DungeonScanner.isChunkLoaded(x, z)) continue;
            if (height == 0) height = DungeonScanner.getHighestY(x, z);
            loadFromCore(DungeonScanner.hashCeil(x, z));
        }
        return this;
    }

    public void addComponent(Component comp) {
        addComponent(comp, true);
    }

    public void addComponent(Component comp, boolean update) {
        for (WorldComponent existing : comps) {
            if (existing.toComponent().equals(comp)) return;
        }

        WorldComponent world = comp.withWorld();
        comps.add(world);
        for (int i = 0; i < ROOM_OFFSETS.size(); i++) {
            World offset = ROOM_OFFSETS.get(i);
            possibleCorners.add(new PossibleCorner(i, world,
                    new World(world.wx() + offset.x(), world.wz() + offset.z())));
        }

        if (update) update();
    }

    public void addComponents(List<Component> components) {
        for (Component comp : components) addComponent(comp, false);
        update();
    }

    public void findRotation() {
        if (height == 0) return;
        if ("1x4".equals(shapeIn) && comps.size() < 4) return;

        if (type == RoomType.FAIRY) {
            WorldComponent comp = comps.get(0);
            rotation = 0;
            corner = new World(comp.wx() - DungeonPositions.HALF_ROOM_SIZE,
                    comp.wz() - DungeonPositions.HALF_ROOM_SIZE);
            return;
        }

        var iterator = possibleCorners.iterator();
        while (iterator.hasNext()) {
            PossibleCorner candidate = iterator.next();
            int idx = candidate.idx();

            if ("1x4".equals(shapeIn)) {
                int cidx = comps.indexOf(candidate.comp());
                if (cidx != 0 && cidx != comps.size() - 1) {
                    iterator.remove();
                    continue;
                }
                boolean horizontal = comps.get(0).cz() == comps.get(1).cz();
                if (cidx == 0) {
                    if (horizontal) {
                        if (idx != 0 && idx != 3) {
                            iterator.remove();
                            continue;
                        }
                    } else if (idx != 0 && idx != 1) {
                        iterator.remove();
                        continue;
                    }
                } else if (horizontal) {
                    if (idx != 1 && idx != 2) {
                        iterator.remove();
                        continue;
                    }
                } else if (idx != 2 && idx != 3) {
                    iterator.remove();
                    continue;
                }
            }

            World pos = candidate.pos();
            if (!DungeonScanner.isChunkLoaded(pos.x(), pos.z())) continue;

            BlockState state = DungeonScanner.getBlockState(pos.x(), height, pos.z());
            if (state == null) continue;

            if (state.getBlock() != Blocks.DYED_TERRACOTTA.blue()) {
                iterator.remove();
                continue;
            }

            rotation = idx * 90;
            corner = pos;
            iterator.remove();
        }
    }

    private void shape() {
        int distinctX = (int) comps.stream().mapToInt(WorldComponent::cx).distinct().count();
        int distinctZ = (int) comps.stream().mapToInt(WorldComponent::cz).distinct().count();

        if (comps.isEmpty() || comps.size() > 4) shape = ShapeType.Unknown;
        else if (comps.size() == 1) shape = ShapeType.Shape1x1;
        else if (comps.size() == 2) shape = ShapeType.Shape1x2;
        else if (comps.size() == 4) {
            shape = (distinctX == 1 || distinctZ == 1) ? ShapeType.Shape1x4 : ShapeType.Shape2x2;
        } else if (distinctX == comps.size() || distinctZ == comps.size()) shape = ShapeType.Shape1x3;
        else shape = ShapeType.ShapeL;
    }

    public boolean hasRotation() {
        return rotation != -1 && corner.x() != Integer.MIN_VALUE;
    }
}
