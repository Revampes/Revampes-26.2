package xyz.whatsyouss.frosty.hud.impl;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import xyz.whatsyouss.frosty.hud.HudWidget;
import xyz.whatsyouss.frosty.modules.impl.dungeon.DungeonMap;
import xyz.whatsyouss.frosty.utility.dungeon.map.DungeonMapOptions;
import xyz.whatsyouss.frosty.utility.dungeon.map.DungeonMapRenderer;
import xyz.whatsyouss.frosty.utility.dungeon.map.DungeonMapRoomData;
import xyz.whatsyouss.frosty.utility.dungeon.map.DungeonMapRoomPrediction;
import xyz.whatsyouss.frosty.utility.dungeon.map.DungeonRoom;
import xyz.whatsyouss.frosty.utility.dungeon.map.DungeonScanner;
import xyz.whatsyouss.frosty.utility.dungeon.map.DungeonTracker;
import xyz.whatsyouss.frosty.utility.dungeon.map.FloorType;

import java.util.ArrayList;
import java.util.List;

/**
 * The on-screen dungeon map.
 *
 * <p>Devonian draws it as its own HUD feature with a private editor; here it is a normal
 * {@link HudWidget}, so it shows up in the mod's <em>Edit HUD</em> screen and can be dragged and
 * resized like every other element.
 */
public class DungeonMapWidget extends HudWidget {

    /** The element is a square, like devonian's fixed 100x100 (before the HUD scale). */
    private static final float BASE_SIZE = 100.0f;

    private final DungeonMap module;
    private final DungeonMapRoomData[] roomCache = new DungeonMapRoomData[DungeonScanner.rooms.length];

    public DungeonMapWidget(DungeonMap module) {
        super("dungeonMap", "Dungeon Map", 8.0f, 8.0f, 1.0f);
        this.module = module;
    }

    @Override
    public boolean isVisible() {
        if (!module.isEnabled()) return false;
        if (!DungeonTracker.inDungeon()) return false;
        return !DungeonTracker.inBoss || module.showInBoss.isToggled();
    }

    @Override
    public List<String> lines() {
        return List.of();
    }

    @Override
    public int getUnscaledWidth(Font font) {
        return Math.round(BASE_SIZE);
    }

    @Override
    public int getUnscaledHeight(Font font) {
        return Math.round(BASE_SIZE);
    }

    @Override
    public void render(GuiGraphicsExtractor context, Font font, float alpha) {
        drawMap(context, font, true);
    }

    @Override
    public void renderForEditor(GuiGraphicsExtractor context, Font font) {
        drawMap(context, font, false);
    }

    private void drawMap(GuiGraphicsExtractor context, Font font, boolean live) {
        if (live && !isVisible()) return;

        DungeonMapOptions options = module.buildOptions();
        List<DungeonMapRoomData> rooms = collectRooms();

        if (module.predictRooms.isToggled() && DungeonTracker.started) {
            FloorType floor = DungeonTracker.floor == FloorType.None ? FloorType.M7 : DungeonTracker.floor;
            DungeonMapRoomPrediction.analyzeRooms(rooms, DungeonScanner.doors, floor,
                    options.renderUnknownRooms, DungeonTracker.totalPuzzles);
        }

        float size = BASE_SIZE * getScale();
        DungeonMapRenderer.draw(context, font, getX(), getY(), size, options, rooms,
                DungeonScanner.doors, getScale());

        if (!rooms.isEmpty()) return;

        String label = "Dungeon Map";
        int width = font.width(label);
        context.text(font, label, Math.round(getX() + size / 2f - width / 2f),
                Math.round(getY() + size / 2f - 4), 0xFFFFFFFF, true);
    }

    /**
     * Wraps the scanner's rooms into per-frame render data. The wrappers are cached so a room that
     * did not change keeps its identity, which matters because the prediction is keyed on it.
     *
     * <p>The list keeps the scanner's indexing (empty slots stay {@code null}) because
     * {@link DungeonMapRoomPrediction} looks rooms up by {@code Component#getRoomIdx()}.
     */
    private List<DungeonMapRoomData> collectRooms() {
        List<DungeonMapRoomData> rooms = new ArrayList<>(DungeonScanner.rooms.length);

        for (int i = 0; i < DungeonScanner.rooms.length; i++) {
            DungeonRoom room = DungeonScanner.rooms[i];
            if (room == null) {
                roomCache[i] = null;
                rooms.add(null);
                continue;
            }

            DungeonMapRoomData data = roomCache[i];
            if (data == null || data.room != room) data = new DungeonMapRoomData(room);
            data.predictedAt = null;
            data.predictedTypes = null;
            roomCache[i] = data;
            rooms.add(data);
        }

        return rooms;
    }
}
