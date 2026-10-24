package xyz.whatsyouss.frosty.utility.dungeon.map;

public enum RoomType {
    BLOOD(0),
    ENTRANCE(10),
    PUZZLE(20),
    RARE(30),
    YELLOW(40),
    TRAP(50),
    UNKNOWN(60),
    FAIRY(70),
    NORMAL(80);

    public final int prio;

    RoomType(int prio) {
        this.prio = prio;
    }

    public static RoomType byName(String name) {
        if (name == null) return null;
        for (RoomType type : values()) {
            if (type.name().equalsIgnoreCase(name)) return type;
        }
        return null;
    }
}
