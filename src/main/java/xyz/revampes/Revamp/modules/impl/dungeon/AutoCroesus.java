package xyz.revampes.Revamp.modules.impl.dungeon;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import xyz.revampes.Revamp.modules.Module;
import xyz.revampes.Revamp.settings.impl.ButtonSetting;
import xyz.revampes.Revamp.settings.impl.MultiSelectSetting;
import xyz.revampes.Revamp.settings.impl.SliderSetting;
import xyz.revampes.Revamp.utility.LocationUtils;
import xyz.revampes.Revamp.utility.StringUtils;
import xyz.revampes.Revamp.utility.Utils;
import xyz.revampes.Revamp.utility.dungeon.croesus.CroesusChest;
import xyz.revampes.Revamp.utility.dungeon.croesus.CroesusData;
import xyz.revampes.Revamp.utility.dungeon.croesus.SkyblockItemIndex;
import xyz.revampes.Revamp.utility.kuudra.profit.ItemPriceManager;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AutoCroesus extends Module {

    private static final String[] FLOORS = {
            "F1", "F2", "F3", "F4", "F5", "F6", "F7",
            "M1", "M2", "M3", "M4", "M5", "M6", "M7"
    };

    private static final double SEARCH_RANGE = 3.5;

    private static final Pattern COST_PATTERN = Pattern.compile("^([\\d,]+) Coins$");
    private static final Pattern BOOK_PATTERN = Pattern.compile("^Enchanted Book \\(([\\w ]+?) ([\\w]+)\\)$");
    private static final Pattern ESSENCE_PATTERN = Pattern.compile("^(.+?) Essence x(\\d+)$");

    private static final Map<String, String> ITEM_REPLACEMENTS = Map.ofEntries(
            Map.entry("Shiny Wither Boots", "WITHER_BOOTS"),
            Map.entry("Shiny Wither Leggings", "WITHER_LEGGINGS"),
            Map.entry("Shiny Wither Chestplate", "WITHER_CHESTPLATE"),
            Map.entry("Shiny Wither Helmet", "WITHER_HELMET"),
            Map.entry("Shiny Necron's Handle", "NECRON_HANDLE"),
            Map.entry("Wither Shard", "SHARD_WITHER"),
            Map.entry("Thorn Shard", "SHARD_THORN"),
            Map.entry("Apex Dragon Shard", "SHARD_APEX_DRAGON"),
            Map.entry("Power Dragon Shard", "SHARD_POWER_DRAGON"),
            Map.entry("Scarf Shard", "SHARD_SCARF"),
            Map.entry("Necron Dye", "DYE_NECRON"),
            Map.entry("Livid Dye", "DYE_LIVID")
    );

    private final SliderSetting clickDelay = new SliderSetting("Click Delay", 300.0, 100.0, 1000.0, 25.0);
    private final ButtonSetting chestKeys = new ButtonSetting("Use Chest Keys", false);
    private final SliderSetting chestKeyMinProfit = new SliderSetting("Key Min Profit", "m", 0.5, 0.0, 2.0, 0.01);
    private final ButtonSetting kismets = new ButtonSetting("Use Kismet", false);
    private final SliderSetting kismetsMinProfit =
            new SliderSetting("Kismet Min Profit", "m", 2.0, 1.0, 3.5, 0.05);
    private final MultiSelectSetting kismetFloors = new MultiSelectSetting("Kismet Floors", FLOORS);

    private final Deque<PendingTask> pending = new ArrayDeque<>();

    private boolean running;
    private Action action = Action.IDLE;
    private boolean kismetting;
    private int currentPage = 1;
    private String lastMenuKey;
    private int rewardRetries;

    public AutoCroesus() {
        super("AutoCroesus", category.Dungeon);
        registerSetting(clickDelay);
        registerSetting(chestKeys);
        registerSetting(chestKeyMinProfit);
        registerSetting(kismets);
        registerSetting(kismetsMinProfit);
        registerSetting(kismetFloors);
        CroesusData.load();
    }

    @Override
    public String getDesc() {
        return "Claims every Croesus chest in the dungeon hub and buys the most profitable one *;ac go* to work";
    }

    public void start() {
        start(true);
    }

    public void start(boolean checkPrice) {
        if (!isEnabled()) {
            modMessage("Module is not Enabled!");
            return;
        }
        if (!inDungeonHub()) {
            modMessage("You are not in the dungeon hub!");
            return;
        }        if (running) {
            modMessage("Already claiming!");
            return;
        }
        if (checkPrice && !ItemPriceManager.get().isBazaarReady()) {
            modMessage("Waiting for price data, try again in a moment");
            return;
        }

        SkyblockItemIndex.ensureLoaded();
        if (checkPrice && !SkyblockItemIndex.isLoaded()) {
            modMessage("Loading item names, try again in a moment");
            return;
        }

        running = true;
        action = Action.CROESUS;
        rewardRetries = 0;
        if (!clickCroesus()) {
            running = false;
            modMessage("Failed to click Croesus!");
            reset();
        }
    }

    public void reset() {
        running = false;
        action = Action.IDLE;
        kismetting = false;
        currentPage = 1;
        lastMenuKey = null;
        rewardRetries = 0;
        pending.clear();
    }

    @Override
    public void onDisable() {
        reset();
    }

    @Override
    public void onUpdate() {
        runPending();

        if (!running) return;
        if (!Utils.nullCheck()) return;

        String area = LocationUtils.getCurrentArea();
        if (area != null && !area.equalsIgnoreCase("Unknown")
                && !area.toLowerCase(Locale.ROOT).contains("dungeon hub")) {
            modMessage("Stopped!");
            reset();
            return;
        }

        AbstractContainerScreen<?> screen = containerScreen();
        if (screen == null) {
            lastMenuKey = null;
            return;
        }

        AbstractContainerMenu menu = screen.getMenu();
        String title = StringUtils.stripFormatting(screen.getTitle().getString()).trim();
        String key = menu.containerId + "|" + title;
        if (key.equals(lastMenuKey)) return;

        lastMenuKey = key;

        switch (action) {
            case CROESUS -> handleCroesus(menu, title);
            case REWARDS -> handleRewards(menu, title);
            case CHEST -> handleChest(menu, title);
            default -> {
            }
        }
    }

    private void handleCroesus(AbstractContainerMenu menu, String title) {
        if (!title.equals("Croesus")) return;

        currentPage = getPage(menu.slots);

        for (Slot slot : menu.slots) {
            ItemStack stack = slot.getItem();
            if (!stack.is(Items.PLAYER_HEAD)) continue;

            RunType type = RunType.findByDisplayName(stack.getHoverName().getString());
            if (type == RunType.NONE) continue;
            if (lore(stack).stream().noneMatch(line -> line.contains("No chests opened yet!"))) continue;

            schedule(clickDelayMs(), () -> {
                action = Action.REWARDS;
                rewardRetries = 0;
                click(slot.index);
            });
            return;
        }

        ItemStack nextArrow = slotItem(menu, 53);
        if (nextArrow.is(Items.ARROW)) {
            clickOnDelay(53);
        } else {
            modMessage("All chests looted!");
            reset();
            close();
        }
    }

    private void handleRewards(AbstractContainerMenu menu, String title) {
        RunType type = RunType.findByTitle(title);
        if (type == RunType.NONE) return;

        String floorName = floorName(type, title);
        List<Reward> chests = new ArrayList<>();

        for (Slot slot : menu.slots) {
            if (slot.index > 45) break;

            ItemStack stack = slot.getItem();
            if (!stack.is(Items.PLAYER_HEAD)) continue;

            CroesusChest.Type chestType = chestType(stack.getHoverName().getString());
            if (chestType == CroesusChest.Type.NONE) continue;

            List<String> lines = lore(stack);
            int costLine = lines.indexOf("Cost");
            if (costLine < 2 || costLine + 1 >= lines.size()) continue;

            CroesusChest.Info info = getRewards(lines.subList(1, costLine - 1), lines.get(costLine + 1));
            if (info == null) {
                if (!retryRewards(menu, title)) {
                    reset();
                    close();
                }
                return;
            }

            info.type = chestType;
            chests.add(new Reward(info, slot.index, stack.getHoverName().getString()));
        }

        if (chests.isEmpty()) {
            if (!retryRewards(menu, title)) {
                reset();
                close();
            }
            return;
        }

        Optional<Reward> bedrock = chests.stream()
                .filter(chest -> chest.info.type == CroesusChest.Type.BEDROCK)
                .findFirst();
        Optional<Reward> alwaysBuy = chests.stream().filter(Reward::alwaysBuy).findFirst();
        boolean canKismet = canUseKismet(menu);

        if (bedrock.isPresent()
                && kismets.isToggled()
                && canKismet
                && kismetFloors.isSelected(floorName)
                && bedrock.get().info.profit < kismetsMinProfit.getInput() * 1_000_000.0) {
            kismetting = true;
            action = Action.CHEST;
            if (validSlot(bedrock.get().slot)) {
                clickOnDelay(bedrock.get().slot);
            } else {
                modMessage("§4Invalid slot! (" + bedrock.get().slot + ")");
                reset();
            }
            return;
        }

        Reward best = alwaysBuy.orElseGet(() -> getBestProfit(chests));
        if (best == null) {
            reset();
            close();
            return;
        }

        if (validSlot(best.slot)) {
            modMessage("Claiming the " + best.name + "§r chest, Profit: " + Math.round(best.info.profit));
            action = Action.CHEST;
            clickOnDelay(best.slot);
            CroesusData.addRunLog(best.info);
        } else {
            modMessage("§4Invalid slot! (" + best.slot + ")");
            reset();
        }
    }

    private void handleChest(AbstractContainerMenu menu, String title) {
        String first = title.split(" ")[0];
        CroesusChest.Type chestType = chestType(first);
        if (chestType == CroesusChest.Type.NONE) return;

        if (kismetting) {
            kismetting = false;

            ItemStack kismetStack = slotItem(menu, 50);
            if (lore(kismetStack).stream().anyMatch(line -> line.contains("Bring a Kismet Feather"))) {
                modMessage("§cNo kismets!");
                reset();
                close();
                return;
            }

            action = Action.REWARDS;
            rewardRetries = 0;
            clickOnDelay(50);
            return;
        }

        clickOnDelay(31);
        action = Action.CROESUS;
        schedule(clickDelayMs() * 2, () -> {
            if (clickCroesus()) return;
            modMessage("Stopped!");
            reset();
        });
    }

    private boolean retryRewards(AbstractContainerMenu menu, String title) {
        if (rewardRetries++ >= 3) {
            modMessage("§cCould not price the rewards, stopping");
            return false;
        }

        schedule(1000, () -> {
            if (!running || action != Action.REWARDS) return;
            AbstractContainerScreen<?> screen = containerScreen();
            if (screen == null) return;
            handleRewards(screen.getMenu(), StringUtils.stripFormatting(screen.getTitle().getString()).trim());
        });
        return true;
    }

    private boolean clickCroesus() {
        if (action != Action.CROESUS) return false;
        if (mc.level == null || mc.player == null || mc.gameMode == null) return false;

        Player entity = findCroesus();
        if (entity == null) {
            modMessage("No Croesus entity returned!");
            return false;
        }

        if (mc.player.distanceToSqr(entity) > 16.0) {
            modMessage("Croesus too far! " + Math.round(Math.sqrt(mc.player.distanceToSqr(entity))));
            return false;
        }

        if (!(mc.hitResult instanceof EntityHitResult hit) || hit.getType() == HitResult.Type.MISS) {
            modMessage("§cNot looking at an entity");
            return false;
        }

        Entity looking = hit.getEntity();
        if (entity.distanceToSqr(looking) > 9.0) {
            modMessage("§cBlocked by entity!");
            return false;
        }

        mc.gameMode.attack(mc.player, entity);
        return true;
    }

    private Player findCroesus() {
        Vec3 eye = mc.player.position().add(0.0, mc.player.getEyeHeight(), 0.0);
        AABB box = new AABB(eye, eye).inflate(SEARCH_RANGE, SEARCH_RANGE, SEARCH_RANGE);

        List<ArmorStand> stands = mc.level.getEntitiesOfClass(ArmorStand.class, box,
                stand -> stand.getName().getString().contains("Croesus"));
        if (stands.isEmpty()) {
            modMessage("Failed to find an entity named Croesus!");
            return null;
        }
        if (stands.size() > 1) {
            modMessage("Found more than one Croesus stand?");
            return null;
        }

        ArmorStand stand = stands.getFirst();
        List<Player> players = mc.level.getEntitiesOfClass(Player.class, box,
                player -> player.distanceToSqr(stand) < 0.01);
        if (players.isEmpty()) {
            modMessage("No Croesus entity!");
            return null;
        }
        if (players.size() > 1) {
            modMessage("Found multiple Croesus entities!");
            return null;
        }

        return players.getFirst();
    }

    private CroesusChest.Info getRewards(List<String> itemLines, String cost) {
        CroesusChest.Info info = new CroesusChest.Info();

        if (!cost.contains("FREE")) {
            Matcher matcher = COST_PATTERN.matcher(StringUtils.stripFormatting(cost).trim());
            if (matcher.find()) {
                try {
                    info.cost = Integer.parseInt(matcher.group(1).replace(",", ""));
                } catch (NumberFormatException ignored) {
                }
            }
        }

        for (String line : itemLines) {
            CroesusChest.Item item = parseItem(line);
            if (item == null) continue;

            double price = getSellPrice(item.getId());
            if (price < 0) {
                modMessage("§4Failed to get a price for " + item.getId());
                return null;
            }

            info.value += price * item.getQuantity();
            info.items.add(item);
        }

        info.profit = info.value - info.cost;
        return info;
    }

    private double getSellPrice(String itemId) {
        if (CroesusData.getWorthless().contains(itemId)) return 0.0;

        long price = ItemPriceManager.get().getItemPrice(itemId);
        return price > 0 ? price : -1.0;
    }

    private CroesusChest.Item parseItem(String line) {
        String cleaned = StringUtils.stripFormatting(line).trim();
        if (cleaned.isEmpty() || cleaned.equals("Cost")) return null;

        if (cleaned.contains("Enchanted Book")) {
            Matcher matcher = BOOK_PATTERN.matcher(cleaned);
            if (!matcher.find()) return null;

            String bookName = matcher.group(1).trim();
            String tier = matcher.group(2);

            String id = ("ENCHANTMENT_" + bookName.toUpperCase(Locale.ROOT).replace(" ", "_")
                    + "_" + romanOrNumber(tier)).replace("ULTIMATE_ULTIMATE_", "ULTIMATE_");
            return new CroesusChest.Item(id, 1);
        }

        if (cleaned.contains(" Essence ")) {
            Matcher matcher = ESSENCE_PATTERN.matcher(cleaned);
            if (!matcher.find()) return null;

            try {
                return new CroesusChest.Item("ESSENCE_" + matcher.group(1).trim().replace(" ", "_").toUpperCase(Locale.ROOT),
                        Integer.parseInt(matcher.group(2)));
            } catch (NumberFormatException e) {
                return null;
            }
        }

        String replacement = ITEM_REPLACEMENTS.get(cleaned);
        if (replacement != null) return new CroesusChest.Item(replacement, 1);

        String id = SkyblockItemIndex.findId(cleaned);
        if (id != null) return new CroesusChest.Item(id, 1);

        modMessage("Failed to find id for " + cleaned);
        return null;
    }

    private Reward getBestProfit(List<Reward> rewards) {
        Reward best = null;
        for (Reward reward : rewards) {
            if (best == null || reward.info.profit > best.info.profit) best = reward;
        }
        return best;
    }

    private boolean canUseKismet(AbstractContainerMenu menu) {
        List<Component> modifiers = rawLore(slotItem(menu, 32));
        for (Component line : modifiers) {
            if (!line.getString().contains("Kismet Feather")) continue;
            if (line.getStyle().isStrikethrough()) return false;

            for (Component sibling : line.getSiblings()) {
                if (sibling.getStyle().isStrikethrough()) return false;
            }
            return true;
        }
        return false;
    }

    private String floorName(RunType type, String title) {
        String[] parts = title.split("- Floor");
        if (parts.length < 2) return "";

        int number = romanOrNumber(parts[1].trim());
        if (type == RunType.MASTER_CATACOMBS) return "M" + number;
        return "F" + number;
    }

    private int getPage(List<Slot> slots) {
        ItemStack nextArrow = slots.size() > 53 ? slots.get(53).getItem() : ItemStack.EMPTY;
        ItemStack lastArrow = slots.size() > 45 ? slots.get(45).getItem() : ItemStack.EMPTY;
        if (nextArrow.is(Items.ARROW)) return pageNumber(nextArrow) - 1;
        if (lastArrow.is(Items.ARROW)) return pageNumber(lastArrow) + 1;
        return 1;
    }

    private int pageNumber(ItemStack arrow) {
        List<String> lines = lore(arrow);
        if (lines.isEmpty()) return 1;

        String[] parts = StringUtils.stripFormatting(lines.getFirst()).split(" ");
        if (parts.length < 2) return 1;

        try {
            return Integer.parseInt(parts[1].trim());
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private void click(int slot) {
        if (mc.player == null || mc.gameMode == null) return;
        if (!(mc.gui.screen() instanceof AbstractContainerScreen<?> screen)) return;

        int containerId = screen.getMenu().containerId;
        if (containerId < 0 || containerId > 100) return;

        mc.gameMode.handleContainerInput(containerId, slot, 0, ContainerInput.PICKUP, mc.player);
    }

    private void clickOnDelay(int slot) {
        schedule(clickDelayMs(), () -> click(slot));
    }

    private void close() {
        schedule(50, () -> {
            if (mc.player != null) mc.player.closeContainer();
        });
    }

    private AbstractContainerScreen<?> containerScreen() {
        if (!(mc.gui.screen() instanceof AbstractContainerScreen<?> screen)) return null;
        if (!(screen.getMenu() instanceof ChestMenu)) return null;
        return screen;
    }

    private boolean inDungeonHub() {
        String area = LocationUtils.getCurrentArea();
        if (area != null && !area.equalsIgnoreCase("Unknown")) {
            return area.toLowerCase(Locale.ROOT).contains("dungeon hub");
        }

        for (String line : Utils.getScoreboardSidebarLines()) {
            if (Utils.stripColor(line).toLowerCase(Locale.ROOT).contains("dungeon hub")) return true;
        }
        return false;
    }

    private static ItemStack slotItem(AbstractContainerMenu menu, int index) {
        if (index < 0 || index >= menu.slots.size()) return ItemStack.EMPTY;
        return menu.slots.get(index).getItem();
    }

    private static boolean validSlot(int slot) {
        return slot >= 0 && slot <= 45;
    }

    private static CroesusChest.Type chestType(String name) {
        String cleaned = StringUtils.stripFormatting(name).trim().toLowerCase(Locale.ROOT);
        for (CroesusChest.Type type : CroesusChest.Type.values()) {
            if (type == CroesusChest.Type.NONE) continue;
            if (cleaned.contains(type.name().toLowerCase(Locale.ROOT))) return type;
        }
        return CroesusChest.Type.NONE;
    }

    private static List<String> lore(ItemStack stack) {
        List<String> lines = new ArrayList<>();
        for (Component line : rawLore(stack)) lines.add(line.getString());
        return lines;
    }

    private static List<Component> rawLore(ItemStack stack) {
        ItemLore lore = stack.get(DataComponents.LORE);
        return lore == null ? List.of() : lore.lines();
    }

    private static int romanOrNumber(String value) {
        String trimmed = value.trim();
        if (trimmed.isEmpty()) return 0;
        if (trimmed.matches("\\d+")) return Integer.parseInt(trimmed);

        int total = 0;
        int previous = 0;
        for (int i = trimmed.length() - 1; i >= 0; i--) {
            int current = switch (trimmed.charAt(i)) {
                case 'I' -> 1;
                case 'V' -> 5;
                case 'X' -> 10;
                case 'L' -> 50;
                case 'C' -> 100;
                case 'D' -> 500;
                case 'M' -> 1000;
                default -> 0;
            };
            if (current >= previous) {
                total += current;
                previous = current;
            } else {
                total -= current;
            }
        }
        return total;
    }

    private long clickDelayMs() {
        return Math.round(clickDelay.getInput());
    }

    private void schedule(long delayMs, Runnable task) {
        pending.add(new PendingTask(System.currentTimeMillis() + delayMs, task));
    }

    private void runPending() {
        long now = System.currentTimeMillis();
        while (!pending.isEmpty() && pending.peekFirst().dueAt() <= now) {
            pending.pollFirst().task().run();
        }
    }

    public static void modMessage(String text) {
        Utils.addChatMessage("§eAutoCroesus » §r" + text);
    }

    public ButtonSetting getChestKeys() {
        return chestKeys;
    }

    public SliderSetting getChestKeyMinProfit() {
        return chestKeyMinProfit;
    }

    public MultiSelectSetting getKismetFloors() {
        return kismetFloors;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public boolean isRunning() {
        return running;
    }

    private record PendingTask(long dueAt, Runnable task) {
    }

    private record Reward(CroesusChest.Info info, int slot, String name) {
        boolean alwaysBuy() {
            return info.items.stream().anyMatch(item -> CroesusData.getAlwaysBuy().contains(item.getId()));
        }
    }

    private enum Action {
        CROESUS,
        REWARDS,
        CHEST,
        IDLE
    }

    private enum RunType {
        MASTER_CATACOMBS("Master Mode The Catacombs", "Master Catacombs - Floor "),
        CATACOMBS("The Catacombs", "Catacombs - Floor "),
        KUUDRA("?", "?"),
        NONE("None", "None");

        private final String displayName;
        private final String rewardsTitle;

        RunType(String displayName, String rewardsTitle) {
            this.displayName = displayName;
            this.rewardsTitle = rewardsTitle;
        }

        static RunType findByDisplayName(String name) {
            String cleaned = StringUtils.stripFormatting(name).trim();
            return Arrays.stream(values())
                    .filter(type -> cleaned.equals(type.displayName))
                    .findFirst()
                    .orElse(NONE);
        }

        static RunType findByTitle(String title) {
            String cleaned = StringUtils.stripFormatting(title).trim();
            return Arrays.stream(values())
                    .filter(type -> cleaned.startsWith(type.rewardsTitle))
                    .findFirst()
                    .orElse(NONE);
        }
    }
}
