package xyz.whatsyouss.frosty.hud;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Registry and layout store for the on-screen widgets.
 *
 * <p>Positions are kept in {@code config/Frosty/hud.json} and can only be changed by
 * dragging a widget inside {@link HudEditorScreen}.
 */
public final class HudManager {

    private static final List<HudWidget> WIDGETS = new ArrayList<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static boolean loaded;

    private HudManager() {
    }

    private static Path configFile() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("config/Frosty/hud.json");
    }

    public static void register(HudWidget widget) {
        WIDGETS.add(widget);
    }

    public static List<HudWidget> widgets() {
        return WIDGETS;
    }

    public static HudWidget byId(String id) {
        for (HudWidget widget : WIDGETS) {
            if (widget.getId().equals(id)) return widget;
        }
        return null;
    }

    public static void tick() {
        for (HudWidget widget : WIDGETS) {
            widget.tick();
        }
    }

    public static void render(GuiGraphicsExtractor context, Font font) {
        render(context, font, false);
    }

    public static void render(GuiGraphicsExtractor context, Font font, boolean editing) {
        ensureDefaults(font);

        for (HudWidget widget : WIDGETS) {
            if (!editing && !widget.isVisible()) continue;
            widget.render(context, font, 1.0f);
        }
    }

    /** Places any widget that has no saved layout entry once its content is known. */
    public static void ensureDefaults(Font font) {
        for (HudWidget widget : WIDGETS) {
            if (!widget.isPositionInitialized()) {
                widget.applyDefaultPosition(font);
            }
        }
    }

    public static HudWidget topmostAt(double mouseX, double mouseY, Font font) {
        ensureDefaults(font);

        for (int i = WIDGETS.size() - 1; i >= 0; i--) {
            HudWidget widget = WIDGETS.get(i);

            float width = Math.max(12.0f, widget.getWidth(font) + 4.0f);
            float height = Math.max(10.0f, widget.getHeight(font) + 4.0f);
            if (mouseX >= widget.getX() - 2 && mouseX <= widget.getX() + width
                    && mouseY >= widget.getY() - 2 && mouseY <= widget.getY() + height) {
                return widget;
            }
        }
        return null;
    }

    public static void load() {
        if (loaded) return;
        loaded = true;

        Path path = configFile();
        if (!Files.exists(path)) return;

        try (Reader reader = Files.newBufferedReader(path)) {
            JsonElement element = GSON.fromJson(reader, JsonElement.class);
            if (element == null || !element.isJsonObject()) return;

            JsonObject root = element.getAsJsonObject();
            for (HudWidget widget : WIDGETS) {
                if (!root.has(widget.getId())) continue;

                JsonObject entry = root.getAsJsonObject(widget.getId());
                if (entry.has("x")) widget.setPosition(entry.get("x").getAsFloat(), entry.get("y").getAsFloat());
                if (entry.has("scale")) widget.setScale(entry.get("scale").getAsFloat());
            }
        } catch (Exception e) {
            System.err.println("Failed to load HUD layout: " + e.getMessage());
        }
    }

    public static void save() {
        JsonObject root = new JsonObject();
        for (HudWidget widget : WIDGETS) {
            JsonObject entry = new JsonObject();
            entry.addProperty("x", widget.getX());
            entry.addProperty("y", widget.getY());
            entry.addProperty("scale", widget.getScale());
            root.add(widget.getId(), entry);
        }

        try {
            Path path = configFile();
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(root, writer);
            }
        } catch (Exception e) {
            System.err.println("Failed to save HUD layout: " + e.getMessage());
        }
    }
}
