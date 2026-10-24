package xyz.whatsyouss.frosty.utility.dungeon.map;

public enum FloorType {
    None(0, false, "", 0, 0, 0, 0),

    Entrance(0, false, "E", 4, 4, 4, 4),

    F1(1, false, "F1", 4, 5, 4, 5),
    F2(2, false, "F2", 5, 5, 5, 5),
    F3(3, false, "F3", 5, 5, 5, 5),
    F4(4, false, "F4", 6, 5, 5, 5),
    F5(5, false, "F5", 6, 6, 5, 6),
    F6(6, false, "F6", 6, 6, 5, 6),
    F7(7, false, "F7", 6, 6, 6, 6),

    M1(1, true, "M1", 4, 5, 4, 5),
    M2(2, true, "M2", 5, 5, 5, 5),
    M3(3, true, "M3", 5, 5, 5, 5),
    M4(4, true, "M4", 6, 5, 5, 5),
    M5(5, true, "M5", 6, 6, 5, 6),
    M6(6, true, "M6", 6, 6, 5, 6),
    M7(7, true, "M7", 6, 6, 6, 6);

    public final int floorNum;
    public final boolean masterMode;
    public final String shortName;

    public final int roomsW;
    public final int roomsH;

    public final int roomsWS;
    public final int roomsHS;

    FloorType(int floorNum, boolean masterMode, String shortName, int roomsW, int roomsH, int roomsWS, int roomsHS) {
        this.floorNum = floorNum;
        this.masterMode = masterMode;
        this.shortName = shortName;
        this.roomsW = roomsW;
        this.roomsH = roomsH;
        this.roomsWS = roomsWS;
        this.roomsHS = roomsHS;
    }

    public int maxDim() {
        return Math.max(roomsW, roomsH);
    }

    public static FloorType byShortName(String name) {
        if (name == null) return None;
        for (FloorType floor : values()) {
            if (floor.shortName.equalsIgnoreCase(name)) return floor;
        }
        return None;
    }
}
