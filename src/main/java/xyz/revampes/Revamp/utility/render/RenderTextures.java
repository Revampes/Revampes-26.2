package xyz.revampes.Revamp.utility.render;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import java.util.HashSet;
import java.util.Set;

/**
 * Cached "does this UI texture actually exist" check.
 *
 * <p>Blitting an identifier that the resource manager cannot resolve silently draws the purple
 * and black missing texture, which is exactly how the dungeon map icons looked before. Every
 * custom UI texture the mod ships goes through here first so a renamed or dropped asset degrades
 * to "not drawn" instead of a checkerboard.
 */
public final class RenderTextures {

    private static final Set<Identifier> AVAILABLE = new HashSet<>();
    private static final Set<Identifier> MISSING = new HashSet<>();

    private RenderTextures() {
    }

    public static boolean available(Identifier texture) {
        if (AVAILABLE.contains(texture)) return true;
        if (MISSING.contains(texture)) return false;

        var resources = Minecraft.getInstance().getResourceManager();
        if (resources == null) return true; // not ready yet, do not cache

        if (resources.getResource(texture).isPresent()) {
            AVAILABLE.add(texture);
            return true;
        }

        MISSING.add(texture);
        System.err.println("Revampes: missing UI texture '" + texture + "'");
        return false;
    }
}
