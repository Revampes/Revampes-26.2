package xyz.whatsyouss.frosty.utility.kuudra;

import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Ported from IQAddons (net.iqaddons.mod.model.spot.PileLocation).
 */
public record PileLocation(
        String name,
        Vec3 position,
        int noPreValue
) {

    public static final List<PileLocation> DEFAULT_PILES = List.of(
            new PileLocation("Shop", new Vec3(-98, 78.125, -112.9375), 7),
            new PileLocation("Triangle", new Vec3(-94, 78.125, -106), 6),
            // These two piles are adjacent, so keep their labels paired with the
            // correct x-coordinate.  The migrated 26.2 table had them reversed,
            // making the Equals pile appear to be Slash (and vice versa).
            new PileLocation("Equals", new Vec3(-106, 78.125, -99.0625), 5),
            new PileLocation("Slash", new Vec3(-98, 78.125, -99.0625), 4),
            new PileLocation("X Cannon", new Vec3(-110, 78.125, -106), 2),
            new PileLocation("X", new Vec3(-106, 78.125, -112.9375), 1)
    );

    private static final double PILE_RADIUS_SQUARED = 1.5 * 1.5;

    public boolean isNoPrePile(int missingPre) {
        return noPreValue == missingPre;
    }

    public boolean isNearby(Vec3 pos) {
        double dx = position.x - pos.x;
        double dz = position.z - pos.z;
        double horizontalDistSq = dx * dx + dz * dz;

        double dy = Math.abs(position.y - pos.y);
        return horizontalDistSq <= PILE_RADIUS_SQUARED && dy <= 5.0;
    }

    public double squaredDistanceTo(Vec3 pos) {
        return position.distanceToSqr(pos);
    }
}
