package xyz.whatsyouss.frosty.utility.pearl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.phys.Vec3;
import xyz.whatsyouss.frosty.Frosty;

import java.awt.Color;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Ported from IQAddons (net.iqaddons.mod.config.loader.PearlWaypointConfigLoader).
 * Editable pearl waypoints live in {@code config/frosty/new_pearl_waypoints.json};
 * the legacy {@code pearl_waypoints.json} is ignored by the reworked system.
 * When the file is missing or malformed the built-in defaults are regenerated.
 */
public final class PearlWaypointConfigLoader {

    private static final PearlWaypointConfigLoader INSTANCE = new PearlWaypointConfigLoader();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int CONFIG_SCHEMA_VERSION = 3;
    private static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve("frosty");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("new_pearl_waypoints.json");

    private volatile List<WaypointArea> cached = List.of();

    private PearlWaypointConfigLoader() {
    }

    public static PearlWaypointConfigLoader get() {
        return INSTANCE;
    }

    public List<WaypointArea> load() {
        try {
            ensureConfig();
            try (Reader reader = Files.newBufferedReader(CONFIG_FILE, StandardCharsets.UTF_8)) {
                cached = parse(JsonParser.parseReader(reader).getAsJsonObject());
                return cached;
            }
        } catch (Exception e) {
            Frosty.LOGGER.warn("[Pearl] failed to load editable Pearl Waypoints, using built-in defaults", e);
            cached = BuiltInPearlWaypoints.getDefaultAreas();
            try {
                save(cached);
            } catch (Exception saveException) {
                Frosty.LOGGER.warn("[Pearl] failed to regenerate editable Pearl Waypoints defaults", saveException);
            }
            return cached;
        }
    }

    public List<WaypointArea> reload() {
        return load();
    }

    public Path getConfigPath() {
        return CONFIG_FILE;
    }

    private void ensureConfig() throws Exception {
        Files.createDirectories(CONFIG_DIR);
        if (!Files.exists(CONFIG_FILE)) {
            save(BuiltInPearlWaypoints.getDefaultAreas());
            return;
        }
        try (Reader reader = Files.newBufferedReader(CONFIG_FILE, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            if (!root.has("schemaVersion") || root.get("schemaVersion").getAsInt() < CONFIG_SCHEMA_VERSION) {
                Frosty.LOGGER.info("[Pearl] waypoint config schema is older than bundled defaults; keeping player edits");
            }
        }
    }

    public void save(List<WaypointArea> areas) throws Exception {
        Files.createDirectories(CONFIG_DIR);
        JsonObject root = toRootJson(areas);
        try (Writer writer = Files.newBufferedWriter(CONFIG_FILE, StandardCharsets.UTF_8)) {
            GSON.toJson(root, writer);
        }
    }

    private JsonObject toRootJson(List<WaypointArea> areas) {
        JsonObject root = new JsonObject();
        root.addProperty("schemaVersion", CONFIG_SCHEMA_VERSION);
        root.addProperty("_comment", "New editable Pearl Waypoints config. Legacy pearl_waypoints.json is ignored by the reworked system.");
        root.addProperty("_format", "areas -> waypoints. Area bounds are [minX, minZ, maxX, maxZ]. Coordinates are [x, y, z].");
        JsonArray areaArray = new JsonArray();
        for (WaypointArea area : areas) {
            JsonObject areaObj = new JsonObject();
            areaObj.addProperty("name", area.name());
            JsonArray bounds = new JsonArray();
            bounds.add(area.bounds().minX());
            bounds.add(area.bounds().minZ());
            bounds.add(area.bounds().maxX());
            bounds.add(area.bounds().maxZ());
            areaObj.add("bounds", bounds);
            JsonArray waypointArray = new JsonArray();
            for (PearlWaypoint waypoint : area.waypoints()) {
                JsonObject waypointObj = toJson(waypoint);
                applyDefaultMetadata(area.name(), waypoint, waypointObj);
                waypointArray.add(waypointObj);
            }
            areaObj.add("waypoints", waypointArray);
            areaArray.add(areaObj);
        }
        root.add("areas", areaArray);
        return root;
    }

    private List<WaypointArea> parse(JsonObject root) {
        if (!bool(root, "enabled", true)) return List.of();
        JsonArray areaArray = root.getAsJsonArray("areas");
        if (areaArray == null) return BuiltInPearlWaypoints.getDefaultAreas();
        JsonArray hiddenSubcategories = root.getAsJsonArray("hiddenSubcategories");
        List<WaypointArea> areas = new ArrayList<>();
        for (JsonElement areaElement : areaArray) {
            if (!areaElement.isJsonObject()) continue;
            JsonObject areaObj = areaElement.getAsJsonObject();
            if (!bool(areaObj, "enabled", true)) continue;
            String name = string(areaObj, "name", "area");
            JsonArray boundsArray = areaObj.getAsJsonArray("bounds");
            BoundingBox2D bounds = boundsArray != null && boundsArray.size() == 4
                    ? new BoundingBox2D(boundsArray.get(0).getAsDouble(), boundsArray.get(1).getAsDouble(), boundsArray.get(2).getAsDouble(), boundsArray.get(3).getAsDouble())
                    : new BoundingBox2D(0, 0, 0, 0);
            List<PearlWaypoint> waypoints = new ArrayList<>();
            JsonArray waypointArray = areaObj.getAsJsonArray("waypoints");
            if (waypointArray != null) {
                for (JsonElement waypointElement : waypointArray) {
                    if (waypointElement.isJsonObject()) {
                        JsonObject waypointObj = waypointElement.getAsJsonObject();
                        if (!bool(waypointObj, "enabled", true)) continue;
                        if (isHiddenSubcategory(hiddenSubcategories, waypointObj)) continue;
                        waypoints.add(parseWaypoint(waypointObj));
                    }
                }
            }
            areas.add(new WaypointArea(name, bounds, waypoints, null, null));
        }
        return List.copyOf(areas);
    }

    private JsonObject toJson(PearlWaypoint waypoint) {
        JsonObject obj = new JsonObject();
        obj.addProperty("label", waypoint.label());
        obj.add("target", vec(waypoint.target()));
        if (waypoint.hasStandBlock()) obj.add("standBlock", vec(waypoint.standBlock()));
        obj.add("aimTarget", vec(waypoint.aimTarget() != null ? waypoint.aimTarget() : waypoint.target()));
        obj.addProperty("trajectoryType", waypoint.trajectoryType().name());
        obj.addProperty("projectionDistance", waypoint.projectionDistance());
        if (waypoint.landingTick() != null) obj.addProperty("landingTick", waypoint.landingTick());
        obj.addProperty("landingOffsetTicks", waypoint.landingOffsetTicks());
        if (waypoint.preSupply() != null) obj.addProperty("preSupply", waypoint.preSupply());
        if (waypoint.hideForPre() != null) obj.addProperty("hideForPre", waypoint.hideForPre());
        obj.addProperty("size", waypoint.size());
        obj.addProperty("alert", waypoint.alert());
        obj.addProperty("color", colorString(waypoint.color()));
        return obj;
    }

    private void applyDefaultMetadata(String areaName, PearlWaypoint waypoint, JsonObject obj) {
        if (waypoint.hasStandBlock()) {
            obj.addProperty("standBlockLabel", standBlockLabel(areaName, waypoint));
        }
    }

    private String standBlockLabel(String areaName, PearlWaypoint waypoint) {
        Vec3 stand = waypoint.standBlock();
        if (same(stand, -135.0, 78.0, -129.0)) return "X Cannon Coal Block";
        if (same(stand, -142.0, 77.0, -87.0)) return "Square Bottom Block";
        if (same(stand, -71.0, 79.0, -135.0)) return "Shop Long Block";
        return switch (areaName.toLowerCase()) {
            case "x" -> "X Block";
            case "x cannon" -> "X Cannon Block";
            case "square" -> "Square Block";
            case "slash" -> "Slash Block";
            case "equals" -> "Equals Block";
            case "triangle" -> "Triangle Block";
            case "shop" -> "Shop Block";
            default -> areaName + " Block";
        };
    }

    private boolean same(Vec3 vec, double x, double y, double z) {
        return Math.abs(vec.x() - x) < 0.001 && Math.abs(vec.y() - y) < 0.001 && Math.abs(vec.z() - z) < 0.001;
    }

    private PearlWaypoint parseWaypoint(JsonObject obj) {
        Vec3 target = vec(obj.getAsJsonArray("target"), Vec3.ZERO);
        Vec3 standBlock = obj.has("standBlock") ? vec(obj.getAsJsonArray("standBlock"), Vec3.ZERO) : null;
        Vec3 aimTarget = obj.has("aimTarget") ? vec(obj.getAsJsonArray("aimTarget"), target) : target;
        PearlTrajectoryType type = enumValue(obj, "trajectoryType", PearlTrajectoryType.FLAT);
        Integer landingTick = obj.has("landingTick") ? obj.get("landingTick").getAsInt() : null;
        Integer preSupply = obj.has("preSupply") ? obj.get("preSupply").getAsInt() : null;
        Integer hideForPre = obj.has("hideForPre") ? obj.get("hideForPre").getAsInt() : null;
        return new PearlWaypoint(
                target,
                color(obj, "color"),
                standBlock,
                aimTarget,
                type,
                number(obj, "projectionDistance", 13.0),
                landingTick,
                number(obj, "landingOffsetTicks", 0.0),
                preSupply,
                hideForPre,
                (float) number(obj, "size", PearlWaypoint.DEFAULT_SIZE),
                string(obj, "label", ""),
                bool(obj, "alert", true)
        );
    }

    private JsonArray vec(Vec3 vec) {
        JsonArray arr = new JsonArray();
        arr.add(vec.x());
        arr.add(vec.y());
        arr.add(vec.z());
        return arr;
    }

    private Vec3 vec(JsonArray arr, Vec3 fallback) {
        if (arr == null || arr.size() != 3) return fallback;
        return new Vec3(arr.get(0).getAsDouble(), arr.get(1).getAsDouble(), arr.get(2).getAsDouble());
    }

    private String string(JsonObject obj, String key, String fallback) {
        return obj.has(key) ? obj.get(key).getAsString() : fallback;
    }

    private double number(JsonObject obj, String key, double fallback) {
        return obj.has(key) ? obj.get(key).getAsDouble() : fallback;
    }

    private boolean bool(JsonObject obj, String key, boolean fallback) {
        return obj.has(key) ? obj.get(key).getAsBoolean() : fallback;
    }

    private boolean isHiddenSubcategory(JsonArray hidden, JsonObject waypoint) {
        if (hidden == null || hidden.isEmpty()) return false;
        for (JsonElement element : hidden) {
            if (!element.isJsonPrimitive()) continue;
            if (matchesHiddenSubcategory(element.getAsString(), waypoint)) return true;
        }
        return false;
    }

    private boolean matchesHiddenSubcategory(String subcategory, JsonObject waypoint) {
        String custom = string(waypoint, "subcategory", "");
        if (!custom.isBlank()) return subcategory.equalsIgnoreCase(custom);
        if (subcategory.equals("Stand Blocks")) return waypoint.has("standBlock") && string(waypoint, "label", "").isBlank();
        String trajectory = string(waypoint, "trajectoryType", "FLAT");
        if (subcategory.contains("Sky")) return trajectory.equals("SKY");
        if (subcategory.contains("Double")) return trajectory.startsWith("DOUBLE");
        if (subcategory.contains("Flat")) return trajectory.equals("FLAT") && !waypoint.has("standBlock");
        return false;
    }

    private String colorString(Color color) {
        int a = color.getAlpha();
        if (a >= 255) {
            return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
        }
        return String.format("#%02X%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue(), a);
    }

    private Color color(JsonObject obj, String key) {
        if (!obj.has(key)) return new Color(0, 0, 0, 0);
        String raw = obj.get(key).getAsString().replace("#", "");
        try {
            if (raw.length() == 8) {
                long rgba = Long.parseLong(raw, 16);
                return new Color(
                        (int) ((rgba >> 24) & 0xFF),
                        (int) ((rgba >> 16) & 0xFF),
                        (int) ((rgba >> 8) & 0xFF),
                        (int) (rgba & 0xFF)
                );
            }
            return new Color(Integer.parseInt(raw, 16));
        } catch (Exception ignored) {
            return new Color(0, 0, 0, 0);
        }
    }

    private <T extends Enum<T>> T enumValue(JsonObject obj, String key, T fallback) {
        if (!obj.has(key)) return fallback;
        try {
            return Enum.valueOf(fallback.getDeclaringClass(), obj.get(key).getAsString());
        } catch (Exception ignored) {
            return fallback;
        }
    }
}
