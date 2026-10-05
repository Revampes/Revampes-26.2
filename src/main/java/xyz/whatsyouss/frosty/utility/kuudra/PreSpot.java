package xyz.whatsyouss.frosty.utility.kuudra;

import net.minecraft.world.phys.Vec3;

/**
 * Ported from IQAddons (net.iqaddons.mod.model.spot.PreSpot).
 */
public enum PreSpot {

    TRIANGLE(
            "Triangle",
            new Vec3(-67.5, 77, -122.5),
            "Shop",
            new Vec3(-81, 76, -143),
            15.0,
            6,
            7
    ),
    X(
            "X",
            new Vec3(-142.5, 77, -151),
            "X Cannon",
            new Vec3(-143, 76, -125),
            30.0,
            1,
            2
    ),
    EQUALS(
            "Equals",
            new Vec3(-65.5, 76, -87.5),
            null,
            null,
            15.0,
            5,
            -1
    ),
    SLASH(
            "Slash",
            new Vec3(-113.5, 77, -68.5),
            "Square",
            new Vec3(-143, 76, -80),
            15.0,
            4,
            3
    );

    private final String displayName;
    private final Vec3 location;

    private final String secondaryName;
    private final Vec3 secondaryLocation;
    private final double detectionRadius;

    private final int missingPreValue;
    private final int secondaryMissingValue;

    PreSpot(String displayName, Vec3 location, String secondaryName, Vec3 secondaryLocation,
            double detectionRadius, int missingPreValue, int secondaryMissingValue) {
        this.displayName = displayName;
        this.location = location;
        this.secondaryName = secondaryName;
        this.secondaryLocation = secondaryLocation;
        this.detectionRadius = detectionRadius;
        this.missingPreValue = missingPreValue;
        this.secondaryMissingValue = secondaryMissingValue;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Vec3 getLocation() {
        return location;
    }

    public String getSecondaryName() {
        return secondaryName;
    }

    public Vec3 getSecondaryLocation() {
        return secondaryLocation;
    }

    public double getDetectionRadius() {
        return detectionRadius;
    }

    public int getMissingPreValue() {
        return missingPreValue;
    }

    public int getSecondaryMissingValue() {
        return secondaryMissingValue;
    }

    public static PreSpot fromPlayerPosition(Vec3 playerPos) {
        for (PreSpot spot : values()) {
            if (spot.isPlayerNearby(playerPos)) {
                return spot;
            }
        }
        return null;
    }

    public static PreSpot fromMessage(String message) {
        String normalized = message.toLowerCase().trim();

        if (normalized.contains("x cannon") || normalized.equals("xc")) {
            return PreSpot.X;
        }
        if (normalized.contains("square")) {
            return PreSpot.SLASH;
        }
        if (normalized.contains("shop")) {
            return PreSpot.TRIANGLE;
        }

        return switch (normalized) {
            case "triangle", "tri" -> PreSpot.TRIANGLE;
            case "x" -> PreSpot.X;
            case "equals", "eq" -> PreSpot.EQUALS;
            case "slash" -> PreSpot.SLASH;
            default -> null;
        };
    }

    public static int getMissingPreValueFromPileName(String pileName) {
        String normalized = pileName.toLowerCase().trim();

        return switch (normalized) {
            case "triangle", "tri" -> 6;
            case "x" -> 1;
            case "x cannon", "xc", "xcannon" -> 2;
            case "equals", "eq" -> 5;
            case "slash" -> 4;
            case "shop" -> 7;
            case "square" -> 3;
            default -> 0;
        };
    }

    public boolean isPlayerNearby(Vec3 playerPos) {
        return playerPos.distanceToSqr(location) < detectionRadius * detectionRadius;
    }

    public boolean hasSecondaryLocation() {
        return secondaryLocation != null;
    }

    public double getSecondaryCheckRadius() {
        return switch (this) {
            case TRIANGLE -> 18.0;
            case X -> 16.0;
            case SLASH -> 20.0;
            default -> 15.0;
        };
    }
}
