package xyz.revampes.Revamp.modules.impl.dungeon;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import meteordevelopment.orbit.EventHandler;
import xyz.revampes.Revamp.events.impl.ContainerMouseClickEvent;
import xyz.revampes.Revamp.events.impl.ContainerScreenRenderEvent;
import xyz.revampes.Revamp.events.impl.ScreenKeyPressEvent;
import xyz.revampes.Revamp.modules.Module;
import xyz.revampes.Revamp.settings.impl.ButtonSetting;
import xyz.revampes.Revamp.settings.impl.ColorSetting;
import xyz.revampes.Revamp.settings.impl.KeyBindSetting;
import xyz.revampes.Revamp.settings.impl.SelectSetting;
import xyz.revampes.Revamp.settings.impl.SliderSetting;
import xyz.revampes.Revamp.utility.Utils;
import xyz.revampes.Revamp.utility.dungeon.leap.LeapMenuRenderer;
import xyz.revampes.Revamp.utility.dungeon.leap.LeapTarget;
import xyz.revampes.Revamp.utility.dungeon.map.DungeonClass;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Circular Spirit Leap menu.
 *
 * <p>The UI is a port of yqloss' {@code YCLeapMenuScreen}: a ring of five segments with the team
 * mates' heads on it and a centre disc that offers the last hovered player
 * (see {@link LeapMenuRenderer}). The functional half follows Odin's {@code LeapMenu}: which
 * players are leaplable comes from the chest itself, classes come from the dungeon tab list, the
 * list is sorted by class/name, and the leap is a container click on the head's slot.
 */
public class LeapMenu extends Module {

    /** Titles Hypixel uses for the two leap interfaces. */
    private static final List<String> LEAP_TITLES = List.of("Spirit Leap", "Teleport to Player");

    /** Same tab list shape the dungeon map uses: {@code [31] [MVP+] Name (Mage XL)}. */
    private static final Pattern PLAYER_INFO = Pattern.compile(
            "^\\[(\\d+)] (?:\\[[^\\]]+] )*(\\w+) .*?\\((\\w+)(?: (\\w+))*\\)$");
    private static final Pattern PLAYER_NAME = Pattern.compile("[A-Za-z0-9_]{1,16}");
    private static final Pattern BRACKETS = Pattern.compile("\\[.*?]");

    private static final String SORT_CLASS = "Class (Revampes)";
    private static final String SORT_CLASS_AZ = "Class A-Z";
    private static final String SORT_NAME_AZ = "Name A-Z";
    private static final String SORT_NONE = "None";

    // ------------------------------------------------------------------ settings

    public final SelectSetting sorting = new SelectSetting("Sorting", 0,
            new String[]{SORT_CLASS, SORT_CLASS_AZ, SORT_NAME_AZ, SORT_NONE});
    public final SelectSetting clickButton = new SelectSetting("Click Button", 0,
            new String[]{"Left", "Right", "Middle"});
    public final ButtonSetting smoothGui = new ButtonSetting("Smooth GUI", true);
    public final SliderSetting guiScale = new SliderSetting("GUI Scale", 1.0, 0.5, 2.0, 0.05);
    public final ButtonSetting hideChest = new ButtonSetting("Hide Vanilla Chest", true);
    public final ButtonSetting announceLeap = new ButtonSetting("Leap Announce", false);

    public final ColorSetting arcColor = new ColorSetting("Segment Color", new Color(0xFF212529, true));
    public final ColorSetting centerColor = new ColorSetting("Center Color", new Color(0xFF212529, true));
    public final ColorSetting textColor = new ColorSetting("Text Color", new Color(0xFFDEE2E6, true));
    public final ColorSetting deadColor = new ColorSetting("Dead Segment Color", new Color(0xFFE03131, true));

    public final KeyBindSetting keySlot1 = new KeyBindSetting("Hotkey 1", 49);
    public final KeyBindSetting keySlot2 = new KeyBindSetting("Hotkey 2", 50);
    public final KeyBindSetting keySlot3 = new KeyBindSetting("Hotkey 3", 51);
    public final KeyBindSetting keySlot4 = new KeyBindSetting("Hotkey 4", 52);
    public final KeyBindSetting keySlot5 = new KeyBindSetting("Hotkey 5", 53);

    // ------------------------------------------------------------------ runtime

    private final List<LeapTarget> targets = new ArrayList<>();
    private final Map<String, DungeonClass> roles = new LinkedHashMap<>();
    private final List<String> deadNames = new ArrayList<>();

    private AbstractContainerScreen<?> openScreen;
    private long lastScan;

    public LeapMenu() {
        this("Leap Menu", category.Dungeon);
    }

    protected LeapMenu(String name, category moduleCategory) {
        this(name, moduleCategory, 0);
    }

    protected LeapMenu(String name, category moduleCategory, int keycode) {
        super(name, moduleCategory, keycode);

        registerSetting(sorting);
        registerSetting(clickButton);
        registerSetting(smoothGui);
        registerSetting(guiScale);
        registerSetting(hideChest);
        registerSetting(announceLeap);

        registerSetting(arcColor);
        registerSetting(centerColor);
        registerSetting(textColor);
        registerSetting(deadColor);

        registerSetting(keySlot1);
        registerSetting(keySlot2);
        registerSetting(keySlot3);
        registerSetting(keySlot4);
        registerSetting(keySlot5);
    }

    @Override
    public String getDesc() {
        return "Renders Revampes' circular leap menu over the Spirit Leap chest";
    }

    @Override
    public void onDisable() {
        close();
    }

    // ------------------------------------------------------------------ screen state

    /** @return the open screen when it is a leap interface, otherwise {@code null}. */
    public AbstractContainerScreen<?> leapScreen() {
        if (!isEnabled()) return null;
        if (!(mc.gui.screen() instanceof AbstractContainerScreen<?> screen)) return null;

        String title = screen.getTitle().getString();
        for (String candidate : LEAP_TITLES) {
            if (candidate.equalsIgnoreCase(title)) return screen;
        }
        return null;
    }

    /** Whether the vanilla chest (and its tooltips) should be replaced by the menu. */
    public boolean hidesVanillaChest(AbstractContainerScreen<?> screen) {
        return hideChest.isToggled() && leapScreen() == screen;
    }

    public boolean isMenuOpen() {
        return leapScreen() != null;
    }

    public List<LeapTarget> targets() {
        return targets;
    }

    private void close() {
        openScreen = null;
        targets.clear();
        roles.clear();
        deadNames.clear();
        LeapMenuRenderer.reset();
    }

    // ------------------------------------------------------------------ events

    @EventHandler
    public void onContainerRender(ContainerScreenRenderEvent event) {
        AbstractContainerScreen<?> screen = leapScreen();
        if (screen == null || screen != event.screen()) {
            if (openScreen != null) close();
            return;
        }

        if (openScreen != screen) {
            openScreen = screen;
            LeapMenuRenderer.reset();
        }

        // The chest contents change while the menu is open (players dying, items moving), so the
        // target list is refreshed on a short interval rather than every frame.
        long now = System.currentTimeMillis();
        if (now - lastScan > 200L) {
            lastScan = now;
            refreshTargets(screen);
        }

        LeapMenuRenderer.draw(event.drawContext(), mc.font, this, targets, event.mouseX(), event.mouseY());
    }

    @EventHandler
    public void onMouseClick(ContainerMouseClickEvent event) {
        AbstractContainerScreen<?> screen = leapScreen();
        if (screen == null || screen != event.getScreen()) return;

        // Consume every click while the menu is up, so no vanilla slot click lands on the hidden
        // chest, and leap when the click hit a segment or the centre disc.
        event.setCancelled(true);

        double scale = Math.max(0.1, guiScale.getInput());
        double centerX = mc.getWindow().getGuiScaledWidth() / 2.0;
        double centerY = mc.getWindow().getGuiScaledHeight() / 2.0;
        double localX = (event.getMouseX() - centerX) / scale;
        double localY = (event.getMouseY() - centerY) / scale;

        int hovered = LeapMenuRenderer.hoveredSegment(targets, localX, localY);
        if (hovered >= 0) {
            leapTo(targets.get(hovered));
            return;
        }

        LeapTarget preferred = LeapMenuRenderer.preferredTarget();
        if (preferred != null && LeapMenuRenderer.hitsCenter(localX, localY)) {
            leapTo(preferred);
        }
    }

    @EventHandler
    public void onKeyPress(ScreenKeyPressEvent event) {
        AbstractContainerScreen<?> screen = leapScreen();
        if (screen == null || screen != event.getScreen()) return;

        KeyBindSetting[] hotkeys = {keySlot1, keySlot2, keySlot3, keySlot4, keySlot5};
        for (int i = 0; i < hotkeys.length; i++) {
            if (hotkeys[i].getKeycode() == 0 || hotkeys[i].getKeycode() != event.getKeyCode()) continue;
            event.setCancelled(true);

            if (i < targets.size()) {
                leapTo(targets.get(i));
            }
            return;
        }
    }

    // ------------------------------------------------------------------ data

    /** Collects the leaplable players from the chest and their classes from the tab list. */
    private void refreshTargets(AbstractContainerScreen<?> screen) {
        targets.clear();

        Map<String, Integer> slots = chestHeads(screen);
        if (slots.isEmpty()) return;

        refreshTabList();

        List<LeapTarget> found = new ArrayList<>(slots.size());
        for (Map.Entry<String, Integer> entry : slots.entrySet()) {
            DungeonClass role = roles.getOrDefault(entry.getKey(), DungeonClass.Unknown);
            boolean dead = isDead(entry.getKey());
            found.add(new LeapTarget(entry.getKey(), role, dead, entry.getValue()));
        }

        targets.addAll(sortTargets(found));
    }

    /** Players offered by the chest, name to menu slot id. */
    private Map<String, Integer> chestHeads(AbstractContainerScreen<?> screen) {
        Map<String, Integer> heads = new LinkedHashMap<>();
        List<Slot> slots = screen.getMenu().slots;

        // The chest area is everything except the 36 player inventory slots at the end.
        int chestSlots = Math.max(0, slots.size() - 36);
        for (int i = 0; i < chestSlots; i++) {
            Slot slot = slots.get(i);
            ItemStack stack = slot.getItem();
            if (stack.isEmpty() || !stack.is(Items.PLAYER_HEAD)) continue;

            String name = playerName(stack.getHoverName().getString());
            if (name == null) continue;

            heads.putIfAbsent(name, slot.index);
        }
        return heads;
    }

    /** Last word of a head name, e.g. {@code "Spirit Leap (Odtheking)"} to {@code "Odtheking"}. */
    private static String playerName(String displayName) {
        String stripped = Utils.stripColor(displayName).replace("\u200B", "").trim();
        String candidate = stripped;
        int space = candidate.lastIndexOf(' ');
        if (space >= 0) candidate = candidate.substring(space + 1);

        return PLAYER_NAME.matcher(candidate).matches() ? candidate : null;
    }

    /** Classes and dead flags come from the same dungeon tab list the rest of the mod reads. */
    private void refreshTabList() {
        roles.clear();
        deadNames.clear();
        if (mc.getConnection() == null) return;

        for (var info : new ArrayList<>(mc.getConnection().getOnlinePlayers())) {
            Component display = info.getTabListDisplayName();
            String raw = display != null ? display.getString()
                    : info.getProfile() == null ? null : info.getProfile().name();
            if (raw == null) continue;

            String line = Utils.stripColor(raw).replace("\u200B", "").trim();
            Matcher matcher = PLAYER_INFO.matcher(line);
            if (!matcher.find()) continue;

            String name = matcher.group(2);
            String role = matcher.group(3);

            if ("DEAD".equals(role)) {
                deadNames.add(name);
                continue;
            }

            DungeonClass clazz = DungeonClass.from(role);
            if (clazz != DungeonClass.Unknown) roles.put(name, clazz);
        }
    }

    private boolean isDead(String name) {
        for (String dead : deadNames) {
            if (dead.equalsIgnoreCase(name)) return true;
        }
        return false;
    }

    private List<LeapTarget> sortTargets(List<LeapTarget> found) {
        List<LeapTarget> sorted = new ArrayList<>(found);
        String mode = sorting.getOption();

        if (SORT_NONE.equals(mode)) {
            return sorted;
        }
        if (SORT_NAME_AZ.equals(mode)) {
            sorted.sort(Comparator.comparing(target -> target.name().toLowerCase(Locale.ROOT)));
            return sorted;
        }
        if (SORT_CLASS_AZ.equals(mode)) {
            sorted.sort(Comparator
                    .comparing((LeapTarget target) -> target.classLabel().toLowerCase(Locale.ROOT))
                    .thenComparing(target -> target.name().toLowerCase(Locale.ROOT)));
            return sorted;
        }

        // yqloss: one slot per class in the fixed order, then fill the gaps alphabetically.
        sorted.sort(Comparator
                .comparingInt(LeapTarget::classPriority)
                .thenComparing(target -> target.name().toLowerCase(Locale.ROOT)));
        return sorted;
    }

    // ------------------------------------------------------------------ leaping

    /** Clicks the head of {@code target} in the chest, which is how the leap is triggered. */
    private void leapTo(LeapTarget target) {
        if (target == null) return;
        if (target.dead()) {
            Utils.addModuleMessage("Leap Menu", "§c" + target.name() + " is dead");
            return;
        }

        AbstractContainerScreen<?> screen = leapScreen();
        if (screen == null) return;
        if (mc.player == null || mc.gameMode == null) return;

        int button = clickButtonIndex();
        mc.gameMode.handleContainerInput(screen.getMenu().containerId, target.slot(), button,
                ContainerInput.PICKUP, mc.player);

        if (announceLeap.isToggled()) {
            if (mc.getConnection() != null) mc.getConnection().sendCommand("pc Leaped to " + target.name() + "!");
        }
    }

    private int clickButtonIndex() {
        String mode = clickButton.getOption();
        if ("Right".equals(mode)) return 1;
        if ("Middle".equals(mode)) return 2;
        return 0;
    }
}
