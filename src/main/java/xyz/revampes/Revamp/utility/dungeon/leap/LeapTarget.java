package xyz.revampes.Revamp.utility.dungeon.leap;

import xyz.revampes.Revamp.utility.dungeon.map.DungeonClass;

/**
 * One leaplable team mate, as resolved from the Spirit Leap chest.
 *
 * @param name  the player name, i.e. the last word of the head's display name
 * @param role  class from the dungeon tab list, {@link DungeonClass#Unknown} when unknown
 * @param dead  whether the player is dead, in which case leaping is not allowed
 * @param slot  menu slot id of the head, used when the click is finally sent
 */
public record LeapTarget(String name, DungeonClass role, boolean dead, int slot) {

    /** yqloss shows the full class name under the head, "???" when it could not be resolved. */
    public String classLabel() {
        return role == DungeonClass.Unknown ? "???" : role.name();
    }

    /** Class order used by yqloss: Archer, Berserk, Mage, Healer, Tank, then everything else. */
    public int classPriority() {
        return switch (role) {
            case Archer -> 0;
            case Berserk -> 1;
            case Mage -> 2;
            case Healer -> 3;
            case Tank -> 4;
            default -> 5;
        };
    }
}
