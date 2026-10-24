package xyz.whatsyouss.frosty.utility.dungeon.map;

import java.awt.Color;
import java.util.EnumMap;
import java.util.Map;

public class DungeonMapOptions {

    public enum MapColor {
        Background,
        Border,

        RoomEntrance,
        RoomNormal, RoomMiniboss,
        RoomFairy, RoomBlood,
        RoomPuzzle,
        RoomTrap,
        RoomYellow,
        RoomRare,
        RoomUnknown,

        DoorNormal,
        DoorEntrance,
        DoorWither,
        DoorBlood
    }

    public enum RoomInfoAlignment {
        TopLeft, TopRight,
        BottomLeft, BottomRight,
        Center, CenterL;

        public String display() {
            return switch (this) {
                case TopLeft -> "Top Left";
                case TopRight -> "Top Right";
                case BottomLeft -> "Bottom Left";
                case BottomRight -> "Bottom Right";
                case Center -> "Center";
                case CenterL -> "CenterL";
            };
        }

        public static String[] displayNames() {
            RoomInfoAlignment[] values = values();
            String[] names = new String[values.length];
            for (int i = 0; i < values.length; i++) names[i] = values[i].display();
            return names;
        }

        public static RoomInfoAlignment byDisplay(String name) {
            for (RoomInfoAlignment alignment : values()) {
                if (alignment.display().equals(name)) return alignment;
            }
            return Center;
        }
    }

    public enum TextShadow {
        None, Drop, Outline;

        public static String[] displayNames() {
            return new String[]{"None", "Drop", "Outline"};
        }

        public static TextShadow from(int index) {
            TextShadow[] values = values();
            return index >= 0 && index < values.length ? values[index] : Drop;
        }
    }

    public final Map<MapColor, Color> colors = new EnumMap<>(MapColor.class);

    public double roomWidth = 0.8;
    public double doorWidth = 0.31;
    public int dungeonWidth = 6;
    public int dungeonHeight = 6;
    public double padding = 0.12;
    public int border = 3;

    public int iconStyle;
    public boolean checkMark;
    public boolean puzzleIcon;
    public boolean roomName;
    public boolean roomNameNotEFB;
    public boolean roomNameNotYellow;
    public boolean roomNoFairyCheck;
    public boolean roomNoCheckName;
    public boolean roomCheckGreen;
    public boolean secretCount;
    public boolean no1Secret;
    public boolean renderCheckIf0Secret;
    public boolean puzzleName;
    public boolean colorRoomName;
    public boolean renderUnknownRooms;
    public boolean dungeonStarted;

    public double iconSize = 0.6;
    public double textSize = 1.33;
    public double unknownRoomsDarkenFactor = 0.7;

    public boolean usePlayerHeads = true;
    public boolean useMarkerSelf = true;
    public boolean renderNames = true;
    public boolean namesRequireLeap = true;
    public boolean useClassName;
    public boolean colorNameByClass;
    public boolean colorMarkerByClass;
    public double nameScale = 2.4;
    public double markerScale = 3.16;
    public double headScale = 2.5;

    public boolean rotate;
    public boolean showInBoss;

    public RoomInfoAlignment iconAlignment = RoomInfoAlignment.Center;
    public RoomInfoAlignment textAlignment = RoomInfoAlignment.CenterL;
    public TextShadow stringShadow = TextShadow.Drop;

    public Color color(MapColor key) {
        return colors.get(key);
    }

    public static int argb(Color color) {
        return color == null ? 0 : color.getRGB();
    }
}
