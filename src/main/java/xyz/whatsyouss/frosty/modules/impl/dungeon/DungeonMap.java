package xyz.whatsyouss.frosty.modules.impl.dungeon;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.KeyboardHandler;
import xyz.whatsyouss.frosty.hud.HudManager;
import xyz.whatsyouss.frosty.events.impl.ReceiveMessageEvent;
import xyz.whatsyouss.frosty.hud.impl.DungeonMapWidget;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.settings.Setting;
import xyz.whatsyouss.frosty.settings.impl.ButtonSetting;
import xyz.whatsyouss.frosty.settings.impl.ColorSetting;
import xyz.whatsyouss.frosty.settings.impl.SelectSetting;
import xyz.whatsyouss.frosty.settings.impl.SliderSetting;
import xyz.whatsyouss.frosty.utility.Utils;
import xyz.whatsyouss.frosty.utility.dungeon.map.DungeonMapOptions;
import xyz.whatsyouss.frosty.utility.dungeon.map.DungeonTracker;

import java.awt.Color;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class DungeonMap extends Module {

    private static final String PRESETS = "Presets";
    private static final String STYLE = "Style";
    private static final String BEHAVIOR = "Behavior";
    private static final String COLORS = "Colors";
    private static final String MARKERS = "Markers";

    private final DungeonMapWidget widget = new DungeonMapWidget(this);

    public final ButtonSetting renderPlayerHeads = add(new ButtonSetting("Render Player Heads", true), MARKERS);
    public final ButtonSetting useMarkerSelf = add(new ButtonSetting("Use Marker for Self", true), MARKERS);
    public final ButtonSetting renderNames = add(new ButtonSetting("Render Names", true), MARKERS);
    public final ButtonSetting namesRequireLeap =
            add(new ButtonSetting("Render Names When Holding Leap", true), MARKERS);
    public final ButtonSetting useClassName = add(new ButtonSetting("Render Class Name", false), MARKERS);
    public final ButtonSetting colorNameByClass = add(new ButtonSetting("Color Player Names", false), MARKERS);
    public final ButtonSetting colorMarkerByClass = add(new ButtonSetting("Color Player Markers", false), MARKERS);
    public final SliderSetting nameScale =
            add(new SliderSetting("Player Name Scale", 2.4, 0.0, 10.0, 0.01), MARKERS);
    public final SliderSetting markerScale = add(new SliderSetting("Marker Scale", 3.16, 0.0, 10.0, 0.01), MARKERS);
    public final SliderSetting headScale = add(new SliderSetting("Head Scale", 2.5, 0.0, 10.0, 0.01), MARKERS);

    public final SliderSetting roomSize = add(new SliderSetting("Room Size", 0.8, 0.0, 1.0, 0.01), STYLE);
    public final SliderSetting doorSize = add(new SliderSetting("Door Size", 0.31, 0.0, 1.0, 0.01), STYLE);
    public final SliderSetting mapPadding = add(new SliderSetting("Map Padding", 0.12, 0.0, 2.0, 0.01), STYLE);
    public final SliderSetting mapBorder = add(new SliderSetting("Map Border Width", 3.55, 0.0, 20.0, 0.01), STYLE);
    public final SliderSetting iconSize = add(new SliderSetting("Icon Size", 0.6, 0.0, 2.0, 0.01), STYLE);
    public final SliderSetting textSize = add(new SliderSetting("Text Size", 1.33, 0.0, 2.0, 0.01), STYLE);
    public final SliderSetting hiddenRoomDarken =
            add(new SliderSetting("Hidden Room Darken Factor", 0.7, 0.0, 1.0, 0.01), STYLE);
    public final SelectSetting iconStyle = add(new SelectSetting("Icon Style", 0,
            new String[]{"IllegalMap", "NEU", "Vanilla New", "Vanilla Old"}), STYLE);
    public final SelectSetting iconAlignment = add(new SelectSetting("Icon Alignment",
            DungeonMapOptions.RoomInfoAlignment.Center.ordinal(),
            DungeonMapOptions.RoomInfoAlignment.displayNames()), STYLE);
    public final SelectSetting textAlignment = add(new SelectSetting("Text Alignment",
            DungeonMapOptions.RoomInfoAlignment.CenterL.ordinal(),
            DungeonMapOptions.RoomInfoAlignment.displayNames()), STYLE);
    public final SelectSetting textShadow =
            add(new SelectSetting("Text Shadow", 1, DungeonMapOptions.TextShadow.displayNames()), STYLE);
    public final ButtonSetting colorRoomText = add(new ButtonSetting("Color Room Name", true), STYLE);

    public final ButtonSetting renderCheckmark = add(new ButtonSetting("Render Checkmarks", false), BEHAVIOR);
    public final ButtonSetting renderPuzzleIcon = add(new ButtonSetting("Render Puzzle Icon", false), BEHAVIOR);
    public final ButtonSetting renderPuzzleName = add(new ButtonSetting("Render Puzzle Name", true), BEHAVIOR);
    public final ButtonSetting renderRoomNames = add(new ButtonSetting("Render Room Names", true), BEHAVIOR);
    public final ButtonSetting namesNotEntranceFairyBlood =
            add(new ButtonSetting("Don't Render Names for Entrance/Fairy/Blood", true), BEHAVIOR);
    public final ButtonSetting dontRenderYellowName =
            add(new ButtonSetting("Don't Render Yellow Room Name", false), BEHAVIOR);
    public final ButtonSetting dontRenderFairyCheck =
            add(new ButtonSetting("Don't Render Fairy Checkmark", false), BEHAVIOR);
    public final ButtonSetting dontRenderCheckIfName =
            add(new ButtonSetting("Don't Render Checkmark and Name", false), BEHAVIOR);
    public final ButtonSetting checkIfGreen = add(new ButtonSetting("Render Check If Green", false), BEHAVIOR);
    public final ButtonSetting renderSecretCount = add(new ButtonSetting("Render Secret Count", true), BEHAVIOR);
    public final ButtonSetting dontRenderSecret1 =
            add(new ButtonSetting("Don't Render 1 Secret Count", false), BEHAVIOR);
    public final ButtonSetting renderCheckIf0 = add(new ButtonSetting("Render Checkmark If 0", false), BEHAVIOR);
    public final ButtonSetting rotate = add(new ButtonSetting("Rotate Map Around Player", false), BEHAVIOR);
    public final ButtonSetting showInBoss = add(new ButtonSetting("Show In Boss", false), BEHAVIOR);
    public final ButtonSetting predictRooms =
            add(new ButtonSetting("Predict Room Types", true), BEHAVIOR);

    public final ButtonSetting showHiddenRooms = add(new ButtonSetting("Show Hidden Rooms", false), BEHAVIOR);

    public final ColorSetting backgroundColor =
            add(new ColorSetting("Map Background Color", new Color(1577189633, true)), COLORS);
    public final ColorSetting borderColor =
            add(new ColorSetting("Map Border Color", new Color(0, 0, 0)), COLORS);
    public final ColorSetting entranceRoomColor =
            add(new ColorSetting("Entrance Room Color", new Color(20, 133, 0)), COLORS);
    public final ColorSetting normalRoomColor =
            add(new ColorSetting("Normal Room Color", new Color(107, 58, 17)), COLORS);
    public final ColorSetting minibossRoomColor =
            add(new ColorSetting("Miniboss Room Color", new Color(107, 58, 17)), COLORS);
    public final ColorSetting fairyRoomColor =
            add(new ColorSetting("Fairy Room Color", new Color(224, 0, 255)), COLORS);
    public final ColorSetting bloodRoomColor =
            add(new ColorSetting("Blood Room Color", new Color(255, 0, 0)), COLORS);
    public final ColorSetting puzzleRoomColor =
            add(new ColorSetting("Puzzle Room Color", new Color(117, 0, 133)), COLORS);
    public final ColorSetting trapRoomColor =
            add(new ColorSetting("Trap Room Color", new Color(216, 127, 51)), COLORS);
    public final ColorSetting yellowRoomColor =
            add(new ColorSetting("Yellow Room Color", new Color(254, 223, 0)), COLORS);
    public final ColorSetting rareRoomColor =
            add(new ColorSetting("Rare Room Color", new Color(255, 203, 89)), COLORS);
    public final ColorSetting unknownRoomColor =
            add(new ColorSetting("Unknown Room Color", new Color(65, 65, 65)), COLORS);
    public final ColorSetting normalDoorColor =
            add(new ColorSetting("Normal Door Color", new Color(107, 58, 17)), COLORS);
    public final ColorSetting entranceDoorColor =
            add(new ColorSetting("Entrance Door Color", new Color(20, 133, 0)), COLORS);
    public final ColorSetting witherDoorColor =
            add(new ColorSetting("Wither Door Color", new Color(0, 0, 0)), COLORS);
    public final ColorSetting bloodDoorColor =
            add(new ColorSetting("Blood Door Color", new Color(255, 0, 0)), COLORS);
    public final ButtonSetting normalDoorDynamic = add(new ButtonSetting("Normal Door Dynamic Color", true), COLORS);

    private final ButtonSetting defaultStyle = add(
            new ButtonSetting("Default Style", this::applyDefaultStyle), PRESETS);
    private final ButtonSetting docStyle = add(new ButtonSetting("Doc Style", this::applyDocStyle), PRESETS);
    private final ButtonSetting legStyle = add(new ButtonSetting("Leg Style", this::applyLegStyle), PRESETS);
    private final ButtonSetting hypixelColors =
            add(new ButtonSetting("Hypixel Colors", this::applyHypixelColors), PRESETS);
    private final ButtonSetting legalMapColors =
            add(new ButtonSetting("LegalMap Colors", this::applyLegalMapColors), PRESETS);
    private final ButtonSetting betterMapColors =
            add(new ButtonSetting("BetterMap Colors", this::applyBetterMapColors), PRESETS);
    private final ButtonSetting catlasColors =
            add(new ButtonSetting("Catlas Colors", this::applyCatlasColors), PRESETS);
    private final ButtonSetting importStyle = add(new ButtonSetting("Import Style", this::importStyle), PRESETS);
    private final ButtonSetting exportStyle = add(new ButtonSetting("Export Style", this::exportStyle), PRESETS);

    public DungeonMap() {
        super("DungeonMap", category.Dungeon);
        HudManager.register(widget);
    }

    private <T extends Setting> T add(T setting, String group) {
        setting.group(group);
        registerSetting(setting);
        return setting;
    }

    public DungeonMapWidget getWidget() {
        return widget;
    }

    @Override
    public String getDesc() {
        return "Catacombs map with room types, secrets, checkmarks and player markers";
    }

    @Override
    public void onEnable() {
        DungeonTracker.active = true;
        DungeonTracker.init();
    }

    @Override
    public void onDisable() {
        DungeonTracker.active = false;
        DungeonTracker.reset();
    }

    @Override
    public void onUpdate() {
        DungeonTracker.tick();
    }

    @EventHandler
    public void onMessage(ReceiveMessageEvent event) {
        DungeonTracker.onChat(event.getMessage().getString());
    }

    public DungeonMapOptions buildOptions() {
        DungeonMapOptions options = new DungeonMapOptions();

        options.roomWidth = roomSize.getInput();
        options.doorWidth = doorSize.getInput();
        options.padding = mapPadding.getInput();
        options.border = (int) Math.ceil(mapBorder.getInput() * widget.getScale());
        options.iconStyle = (int) iconStyle.getValue();
        options.iconSize = iconSize.getInput();
        options.textSize = textSize.getInput();
        options.iconAlignment = DungeonMapOptions.RoomInfoAlignment.values()[(int) iconAlignment.getValue()];
        options.textAlignment = DungeonMapOptions.RoomInfoAlignment.values()[(int) textAlignment.getValue()];
        options.stringShadow = DungeonMapOptions.TextShadow.from((int) textShadow.getValue());

        options.checkMark = renderCheckmark.isToggled();
        options.puzzleIcon = renderPuzzleIcon.isToggled();
        options.puzzleName = renderPuzzleName.isToggled();
        options.roomName = renderRoomNames.isToggled();
        options.roomNameNotEFB = namesNotEntranceFairyBlood.isToggled();
        options.roomNameNotYellow = dontRenderYellowName.isToggled();
        options.roomNoFairyCheck = dontRenderFairyCheck.isToggled();
        options.roomNoCheckName = dontRenderCheckIfName.isToggled();
        options.roomCheckGreen = checkIfGreen.isToggled();
        options.secretCount = renderSecretCount.isToggled();
        options.no1Secret = dontRenderSecret1.isToggled();
        options.renderCheckIf0Secret = renderCheckIf0.isToggled();
        options.colorRoomName = colorRoomText.isToggled();
        options.renderUnknownRooms = showHiddenRooms.isToggled();
        options.dungeonStarted = DungeonTracker.started;
        options.unknownRoomsDarkenFactor = hiddenRoomDarken.getInput();
        options.rotate = rotate.isToggled();
        options.showInBoss = showInBoss.isToggled();

        options.usePlayerHeads = renderPlayerHeads.isToggled();
        options.useMarkerSelf = useMarkerSelf.isToggled();
        options.renderNames = renderNames.isToggled();
        options.namesRequireLeap = namesRequireLeap.isToggled();
        options.useClassName = useClassName.isToggled();
        options.colorNameByClass = colorNameByClass.isToggled();
        options.colorMarkerByClass = colorMarkerByClass.isToggled();
        options.nameScale = nameScale.getInput();
        options.markerScale = markerScale.getInput();
        options.headScale = headScale.getInput();

        options.colors.put(DungeonMapOptions.MapColor.Background, backgroundColor.getColor());
        options.colors.put(DungeonMapOptions.MapColor.Border, borderColor.getColor());
        options.colors.put(DungeonMapOptions.MapColor.RoomEntrance, entranceRoomColor.getColor());
        options.colors.put(DungeonMapOptions.MapColor.RoomNormal, normalRoomColor.getColor());
        options.colors.put(DungeonMapOptions.MapColor.RoomMiniboss, minibossRoomColor.getColor());
        options.colors.put(DungeonMapOptions.MapColor.RoomFairy, fairyRoomColor.getColor());
        options.colors.put(DungeonMapOptions.MapColor.RoomBlood, bloodRoomColor.getColor());
        options.colors.put(DungeonMapOptions.MapColor.RoomPuzzle, puzzleRoomColor.getColor());
        options.colors.put(DungeonMapOptions.MapColor.RoomTrap, trapRoomColor.getColor());
        options.colors.put(DungeonMapOptions.MapColor.RoomYellow, yellowRoomColor.getColor());
        options.colors.put(DungeonMapOptions.MapColor.RoomRare, rareRoomColor.getColor());
        options.colors.put(DungeonMapOptions.MapColor.RoomUnknown, unknownRoomColor.getColor());
        if (!normalDoorDynamic.isToggled()) {
            options.colors.put(DungeonMapOptions.MapColor.DoorNormal, normalDoorColor.getColor());
        }
        options.colors.put(DungeonMapOptions.MapColor.DoorEntrance, entranceDoorColor.getColor());
        options.colors.put(DungeonMapOptions.MapColor.DoorWither, witherDoorColor.getColor());
        options.colors.put(DungeonMapOptions.MapColor.DoorBlood, bloodDoorColor.getColor());

        return options;
    }

    private void applyDocStyle() {
        doorSize.setInput(0.36);
        markerScale.setInput(2.4);
        headScale.setInput(2.46);
        nameScale.setInput(2.4);
        textSize.setInput(1.27);
        mapBorder.setInput(0.0);
        mapPadding.setInput(0.0);
        roomSize.setInput(0.8);
        iconSize.setInput(0.6);
        hiddenRoomDarken.setInput(0.7);
        textAlignment.setValue(DungeonMapOptions.RoomInfoAlignment.CenterL.ordinal());
        iconAlignment.setValue(DungeonMapOptions.RoomInfoAlignment.TopLeft.ordinal());
        textShadow.setValue(DungeonMapOptions.TextShadow.Outline.ordinal());
        namesRequireLeap.setEnabled(false);
        renderPlayerHeads.setEnabled(true);
        useMarkerSelf.setEnabled(false);
        renderRoomNames.setEnabled(false);
        renderPuzzleIcon.setEnabled(false);
        renderPuzzleName.setEnabled(true);
        normalDoorDynamic.setEnabled(true);
        dontRenderFairyCheck.setEnabled(false);
        namesNotEntranceFairyBlood.setEnabled(false);
        dontRenderYellowName.setEnabled(false);
        renderNames.setEnabled(true);
        useClassName.setEnabled(true);
        colorNameByClass.setEnabled(true);
        colorMarkerByClass.setEnabled(true);
        renderCheckmark.setEnabled(false);
        renderSecretCount.setEnabled(true);
        colorRoomText.setEnabled(true);
        rotate.setEnabled(false);
        dontRenderCheckIfName.setEnabled(false);
        dontRenderSecret1.setEnabled(false);
        checkIfGreen.setEnabled(false);
        renderCheckIf0.setEnabled(true);
        backgroundColor.setColor(new Color(50462977, true));
    }

    private void applyLegStyle() {
        doorSize.setInput(0.4);
        markerScale.setInput(2.2);
        headScale.setInput(2.5);
        nameScale.setInput(1.9);
        textSize.setInput(1.08);
        mapBorder.setInput(0.0);
        mapPadding.setInput(0.0);
        roomSize.setInput(0.8);
        iconSize.setInput(0.4);
        hiddenRoomDarken.setInput(0.61);
        textAlignment.setValue(DungeonMapOptions.RoomInfoAlignment.Center.ordinal());
        iconAlignment.setValue(DungeonMapOptions.RoomInfoAlignment.TopLeft.ordinal());
        textShadow.setValue(DungeonMapOptions.TextShadow.Outline.ordinal());
        namesRequireLeap.setEnabled(true);
        renderPlayerHeads.setEnabled(true);
        useMarkerSelf.setEnabled(true);
        renderRoomNames.setEnabled(true);
        renderPuzzleIcon.setEnabled(false);
        renderPuzzleName.setEnabled(true);
        normalDoorDynamic.setEnabled(false);
        dontRenderFairyCheck.setEnabled(false);
        namesNotEntranceFairyBlood.setEnabled(true);
        dontRenderYellowName.setEnabled(false);
        renderNames.setEnabled(true);
        useClassName.setEnabled(true);
        colorNameByClass.setEnabled(true);
        colorMarkerByClass.setEnabled(true);
        renderCheckmark.setEnabled(false);
        renderSecretCount.setEnabled(true);
        colorRoomText.setEnabled(true);
        rotate.setEnabled(false);
        dontRenderCheckIfName.setEnabled(true);
        dontRenderSecret1.setEnabled(true);
        checkIfGreen.setEnabled(false);
        renderCheckIf0.setEnabled(false);
        backgroundColor.setColor(new Color(772723641, true));
    }

    private void applyDefaultStyle() {
        doorSize.setInput(0.31);
        markerScale.setInput(3.16);
        headScale.setInput(2.5);
        nameScale.setInput(2.4);
        textSize.setInput(1.33);
        mapBorder.setInput(3.53);
        mapPadding.setInput(0.12);
        roomSize.setInput(0.8);
        iconSize.setInput(0.6);
        hiddenRoomDarken.setInput(0.7);
        textAlignment.setValue(DungeonMapOptions.RoomInfoAlignment.CenterL.ordinal());
        iconAlignment.setValue(DungeonMapOptions.RoomInfoAlignment.TopLeft.ordinal());
        textShadow.setValue(DungeonMapOptions.TextShadow.Drop.ordinal());
        namesRequireLeap.setEnabled(true);
        renderPlayerHeads.setEnabled(true);
        useMarkerSelf.setEnabled(true);
        renderRoomNames.setEnabled(true);
        renderPuzzleIcon.setEnabled(false);
        renderPuzzleName.setEnabled(true);
        normalDoorDynamic.setEnabled(false);
        dontRenderFairyCheck.setEnabled(false);
        namesNotEntranceFairyBlood.setEnabled(true);
        dontRenderYellowName.setEnabled(false);
        renderNames.setEnabled(true);
        useClassName.setEnabled(false);
        colorNameByClass.setEnabled(false);
        colorMarkerByClass.setEnabled(false);
        renderCheckmark.setEnabled(false);
        renderSecretCount.setEnabled(true);
        colorRoomText.setEnabled(true);
        rotate.setEnabled(false);
        dontRenderCheckIfName.setEnabled(false);
        dontRenderSecret1.setEnabled(false);
        checkIfGreen.setEnabled(false);
        renderCheckIf0.setEnabled(false);
        backgroundColor.setColor(new Color(1577189633, true));

        entranceRoomColor.setColor(new Color(20, 133, 0));
        normalRoomColor.setColor(new Color(107, 58, 17));
        minibossRoomColor.setColor(new Color(107, 58, 17));
        normalDoorColor.setColor(new Color(92, 52, 14));
        puzzleRoomColor.setColor(new Color(117, 0, 133));
        yellowRoomColor.setColor(new Color(254, 223, 0));
        fairyRoomColor.setColor(new Color(224, 0, 255));
        bloodRoomColor.setColor(new Color(255, 0, 0));
        trapRoomColor.setColor(new Color(216, 127, 51));
        unknownRoomColor.setColor(new Color(65, 65, 65));
        witherDoorColor.setColor(new Color(0, 0, 0));
    }

    private void applyHypixelColors() {
        entranceRoomColor.setColor(new Color(0, 124, 0));
        normalRoomColor.setColor(new Color(114, 67, 27));
        minibossRoomColor.setColor(new Color(114, 67, 27));
        normalDoorColor.setColor(new Color(114, 67, 27));
        puzzleRoomColor.setColor(new Color(178, 76, 216));
        yellowRoomColor.setColor(new Color(229, 229, 51));
        fairyRoomColor.setColor(new Color(242, 127, 165));
        bloodRoomColor.setColor(new Color(255, 0, 0));
        trapRoomColor.setColor(new Color(216, 127, 51));
        unknownRoomColor.setColor(new Color(65, 65, 65));
        witherDoorColor.setColor(new Color(0, 0, 0));
    }

    private void applyLegalMapColors() {
        entranceRoomColor.setColor(new Color(20, 133, 0));
        normalRoomColor.setColor(new Color(107, 58, 17));
        minibossRoomColor.setColor(new Color(107, 58, 17));
        normalDoorColor.setColor(new Color(92, 52, 14));
        puzzleRoomColor.setColor(new Color(117, 0, 133));
        yellowRoomColor.setColor(new Color(254, 223, 0));
        fairyRoomColor.setColor(new Color(224, 0, 255));
        bloodRoomColor.setColor(new Color(255, 0, 0));
        trapRoomColor.setColor(new Color(216, 127, 51));
        unknownRoomColor.setColor(new Color(65, 65, 65));
        witherDoorColor.setColor(new Color(0, 0, 0));
    }

    private void applyBetterMapColors() {
        entranceRoomColor.setColor(new Color(0, 123, 0));
        normalRoomColor.setColor(new Color(107, 58, 17));
        minibossRoomColor.setColor(new Color(85, 51, 19));
        normalDoorColor.setColor(new Color(85, 51, 19));
        puzzleRoomColor.setColor(new Color(176, 75, 213));
        yellowRoomColor.setColor(new Color(226, 226, 50));
        fairyRoomColor.setColor(new Color(239, 126, 163));
        bloodRoomColor.setColor(new Color(255, 0, 0));
        trapRoomColor.setColor(new Color(213, 126, 50));
        unknownRoomColor.setColor(new Color(64, 64, 64));
        witherDoorColor.setColor(new Color(0, 0, 0));
    }

    private void applyCatlasColors() {
        applyLegalMapColors();
    }

    private void exportStyle() {
        JsonObject json = new JsonObject();
        for (Setting setting : getSettings()) {
            JsonElement value = serialize(setting);
            if (value != null) json.add(setting.getName(), value);
        }

        String encoded = Base64.getEncoder()
                .encodeToString(json.toString().getBytes(StandardCharsets.UTF_8));
        KeyboardHandler keyboard = mc.keyboardHandler;
        if (keyboard != null) keyboard.setClipboard(encoded);
        Utils.addChatMessage("§aExported §6DungeonMap§a style to clipboard");
    }

    private void importStyle() {
        KeyboardHandler keyboard = mc.keyboardHandler;
        String encoded = keyboard == null ? "" : keyboard.getClipboard();
        if (encoded == null || encoded.isEmpty()) {
            Utils.addChatMessage("§cDungeonMap invalid import");
            return;
        }

        try {
            String decoded = new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
            JsonElement element = JsonParser.parseString(decoded);
            if (!element.isJsonObject()) {
                Utils.addChatMessage("§cDungeonMap invalid import");
                return;
            }

            JsonObject json = element.getAsJsonObject();
            for (Setting setting : getSettings()) {
                if (!json.has(setting.getName())) continue;
                deserialize(setting, json.get(setting.getName()));
            }
            Utils.addChatMessage("§aSuccessfully imported §6DungeonMap§a preset");
        } catch (Exception e) {
            Utils.addChatMessage("§cDungeonMap invalid import");
        }
    }

    private static JsonElement serialize(Setting setting) {
        if (setting instanceof SliderSetting slider && !slider.isRange()) {
            return new com.google.gson.JsonPrimitive(slider.getInput());
        }
        if (setting instanceof SelectSetting select) {
            return new com.google.gson.JsonPrimitive(select.getValue());
        }
        if (setting instanceof ButtonSetting button && !button.isMethodButton) {
            return new com.google.gson.JsonPrimitive(button.isToggled());
        }
        if (setting instanceof ColorSetting color) {
            return new com.google.gson.JsonPrimitive(color.getHex());
        }
        return null;
    }

    private static void deserialize(Setting setting, JsonElement value) {
        try {
            if (setting instanceof SliderSetting slider && !slider.isRange()) {
                slider.setInput(value.getAsDouble());
            } else if (setting instanceof SelectSetting select) {
                select.setValue(value.getAsDouble());
            } else if (setting instanceof ButtonSetting button && !button.isMethodButton) {
                button.setEnabled(value.getAsBoolean());
            } else if (setting instanceof ColorSetting color) {
                color.setHex(value.getAsString());
            }
        } catch (Exception ignored) {
        }
    }
}
