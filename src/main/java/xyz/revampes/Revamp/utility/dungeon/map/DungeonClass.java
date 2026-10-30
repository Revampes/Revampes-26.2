package xyz.revampes.Revamp.utility.dungeon.map;

import java.awt.Color;

public enum DungeonClass {
    Archer("Arch", 'a', "§c", new Color(255, 85, 85, 255)),
    Berserk("Bers", 'b', "§6", new Color(255, 170, 0, 255)),
    Mage("Mage", 'm', "§3", new Color(0, 170, 170, 255)),
    Healer("Heal", 'h', "§5", new Color(170, 0, 170, 255)),
    Tank("Tank", 't', "§a", new Color(85, 255, 85, 255)),

    Unknown("Unknown", '\0', "", new Color(0, 0, 0, 255));

    public final String shortName;
    public final char singleLetter;
    public final String colorCode;
    public final Color color;

    DungeonClass(String shortName, char singleLetter, String colorCode, Color color) {
        this.shortName = shortName;
        this.singleLetter = singleLetter;
        this.colorCode = colorCode;
        this.color = color;
    }

    public int colorRgb() {
        return color.getRGB();
    }

    public static DungeonClass from(String fullName) {
        if (fullName == null) return Unknown;
        return switch (fullName) {
            case "Archer" -> Archer;
            case "Berserk" -> Berserk;
            case "Mage" -> Mage;
            case "Healer" -> Healer;
            case "Tank" -> Tank;
            default -> Unknown;
        };
    }
}
