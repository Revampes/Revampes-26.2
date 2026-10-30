package xyz.revampes.Revamp.hud.impl;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import xyz.revampes.Revamp.hud.HudWidget;
import xyz.revampes.Revamp.modules.impl.dungeon.DungeonMap;
import xyz.revampes.Revamp.utility.dungeon.map.DungeonMapOptions;
import xyz.revampes.Revamp.utility.dungeon.map.DungeonMapRenderer;
import xyz.revampes.Revamp.utility.dungeon.map.DungeonMapRoomData;
import xyz.revampes.Revamp.utility.dungeon.map.DungeonMapRoomPrediction;
import xyz.revampes.Revamp.utility.dungeon.map.DungeonRoom;
import xyz.revampes.Revamp.utility.dungeon.map.DungeonScanner;
import xyz.revampes.Revamp.utility.dungeon.map.DungeonTracker;
import xyz.revampes.Revamp.utility.dungeon.map.FloorType;

import java.util.ArrayList;
import java.util.List;

/**
 * The dungeon map. Two instances are registered: the regular one, which follows the
 * "Show Hidden Rooms" setting, and the legal one, which always renders the map the way the
 * in-game map shows it (no hidden rooms, no predictions) so it can be shown on stream while
 * the regular map keeps the extra information.
 *
 * <p>Both are separate widgets on purpose: the HUD editor lets the player park them wherever
 * they like, including next to each other or on a different monitor region.
 */
public class DungeonMapWidget extends HudWidget {

    private static final float BASE_SIZE = 100.0f;
    private static final float LEGAL_DEFAULT_X = 116.0f;

    private final DungeonMap module;
    private final boolean legal;
    private final DungeonMapRoomData[] roomCache = new DungeonMapRoomData[DungeonScanner.rooms.length];

    public DungeonMapWidget(DungeonMap module) {
        this(module, false);
    }

    public DungeonMapWidget(DungeonMap module, boolean legal) {
        super(legal ? "dungeonMapLegal" : "dungeonMap",
                legal ? "Dungeon Map (Legal)" : "Dungeon Map",
                legal ? LEGAL_DEFAULT_X : 8.0f, 8.0f, 1.0f);
        this.module = module;
        this.legal = legal;
    }

    public boolean isLegal() {
        return legal;
    }

    @Override
    public boolean isVisible() {
        if (!module.isEnabled()) return false;
        if (!DungeonTracker.inDungeon()) return false;
        // The legal map is only drawn as a second map next to the real one.
        if (legal && (!module.renderLegalMap.isToggled() || !module.showHiddenRooms.isToggled())) return false;
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

        DungeonMapOptions options = module.buildOptions(legal, getScale());
        List<DungeonMapRoomData> rooms = collectRooms();

        if (!legal && options.showPredictions && module.predictRooms.isToggled() && DungeonTracker.started) {
            FloorType floor = DungeonTracker.floor == FloorType.None ? FloorType.M7 : DungeonTracker.floor;
            DungeonMapRoomPrediction.analyzeRooms(rooms, DungeonScanner.doors, floor,
                    options.renderUnknownRooms, DungeonTracker.totalPuzzles);
        }

        float size = BASE_SIZE * getScale();
        DungeonMapRenderer.draw(context, font, getX(), getY(), size, options, rooms,
                DungeonScanner.doors, getScale());

        if (!rooms.isEmpty()) return;

        String label = legal ? "Dungeon Map (Legal)" : "Dungeon Map";
        int width = font.width(label);
        context.text(font, label, Math.round(getX() + size / 2f - width / 2f),
                Math.round(getY() + size / 2f - 4), 0xFFFFFFFF, true);
    }

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
