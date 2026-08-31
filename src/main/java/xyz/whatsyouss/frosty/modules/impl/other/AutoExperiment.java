package xyz.whatsyouss.frosty.modules.impl.other;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.HashedPatchMap;
import net.minecraft.network.HashedStack;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import static net.minecraft.world.item.Items.DYED_TERRACOTTA;
import static net.minecraft.world.item.Items.STAINED_GLASS;
import xyz.whatsyouss.frosty.events.impl.PreUpdateEvent;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.settings.impl.ButtonSetting;
import xyz.whatsyouss.frosty.settings.impl.SliderSetting;
import xyz.whatsyouss.frosty.utility.Utils;

public class AutoExperiment extends Module {

    private SliderSetting startDelayMs;
    private SliderSetting clickDelayMs;
    private ButtonSetting randomDelay;
    private ButtonSetting auto;
    private SliderSetting serum;

    // Slots of the "Experimentation Table" main menu (6 row / 54 slot container). Slot index = (row-1)*9 + (col-1).
    private static final int MAIN_CHRONO_BUTTON = 29;   // 4th row, 3rd column
    private static final int MAIN_ULTRA_BUTTON = 33;    // 4th row, 7th column
    private static final int MAIN_CHRONO_GLINT = 20;    // 3rd row, 3rd column
    private static final int MAIN_ULTRA_GLINT = 24;     // 3rd row, 7th column

    // Slots of the "Chronomatron -> Stakes" / "Ultrasequencer -> Stakes" menus (single chest = 3 row / 27 slot container).
    private static final int CHRONO_STAKE_SLOT = 24;    // 3rd row, 7th column
    private static final int ULTRA_STAKE_SLOT = 23;     // 3rd row, 6th column

    private static final long SCREEN_OPEN_WAIT_MS = 200L;

    private static final Object2ObjectMap<Item, Item> TERRACOTTA_TO_GLASS = Object2ObjectMaps.unmodifiable(
            new Object2ObjectArrayMap<>(
                    new Item[]{
                            DYED_TERRACOTTA.red(), DYED_TERRACOTTA.orange(), DYED_TERRACOTTA.yellow(), DYED_TERRACOTTA.lime(),
                            DYED_TERRACOTTA.green(), DYED_TERRACOTTA.cyan(), DYED_TERRACOTTA.lightBlue(), DYED_TERRACOTTA.blue(),
                            DYED_TERRACOTTA.purple(), DYED_TERRACOTTA.pink()
                    },
                    new Item[]{
                            STAINED_GLASS.red(), STAINED_GLASS.orange(), STAINED_GLASS.yellow(), STAINED_GLASS.lime(),
                            STAINED_GLASS.green(), STAINED_GLASS.cyan(), STAINED_GLASS.lightBlue(), STAINED_GLASS.blue(),
                            STAINED_GLASS.purple(), STAINED_GLASS.pink()
                    }
            )
    );

    private boolean chronoGlintFound = false;
    private int chronoGlintFoundAt = -1;
    private List<Item> chronoClickStack = new ArrayList<>();
    private int chronoLastCycle = 0;
    private int chronoCurrentCycle = 0;
    private Item chronoLastModeItem = null;
    private int chronoStartSeconds = -1;

    private List<Integer> ultraClickStack = new ArrayList<>();
    private int ultraStartSeconds = -1;
    private String currentScreenTitle = "";
    private boolean isChronomatronActive = false;
    private boolean isUltrasequencerActive = false;
    private long lastClickTime = 0;
    private long startDelayTimer = 0;
    private boolean isInitialDelay = true;
    private final Random random = new Random();

    // Auto mode state
    private long screenOpenedAt = 0;
    // 0 = idle / awaiting return to the main menu, 1 = currently in Chronomatron, 2 = currently in Ultrasequencer
    private int autoExperiment = 0;
    private boolean autoChronoDone = false;
    private boolean autoUltraDone = false;
    private long lastNavClickAt = 0;

    public AutoExperiment() {
        super("AutoExperiment", "自动附魔桌", category.Other);

        this.registerSetting(startDelayMs = new SliderSetting("Start Delay (ms)", 200, 150, 1000, 50, "第一次点击的延迟"));
        this.registerSetting(clickDelayMs = new SliderSetting("Click Delay (ms)", 200, 50, 1000, 50, "每次点击的延迟"));
        this.registerSetting(randomDelay = new ButtonSetting("Random Delay", "随机延迟", false));
        this.registerSetting(auto = new ButtonSetting("Auto (Enchat 40+)", "自动", false));
        this.registerSetting(serum = new SliderSetting("Serum drank", 0, 0, 3, 1, "已喝血清数量"));
    }

    @EventHandler
    public void onPreUpdate(PreUpdateEvent event) {
        if (!Utils.nullCheck()) {
            return;
        }

        if (mc.gui.screen() instanceof ContainerScreen genericContainerScreen) {
            String title = genericContainerScreen.getTitle().getString();

            // Detect a new container/screen opening -> start the minimum wait window.
            if (!currentScreenTitle.equals(title)) {
                currentScreenTitle = title;
                screenOpenedAt = System.currentTimeMillis();
                resetAllSolvers();
                onScreenEntered(title);
            }

            // Wait at least 200ms after each screen opens before performing any action.
            if (System.currentTimeMillis() - screenOpenedAt < SCREEN_OPEN_WAIT_MS) {
                return;
            }

            if (isChronomatronStakesScreen(title)) {
                tickChronomatronStakes(genericContainerScreen);
            } else if (isUltrasequencerStakesScreen(title)) {
                tickUltrasequencerStakes(genericContainerScreen);
            } else if (title.startsWith("Chronomatron (")) {
                isChronomatronActive = true;
                tickChronomatron(genericContainerScreen);
            } else if (title.startsWith("Ultrasequencer (")) {
                isUltrasequencerActive = true;
                tickUltrasequencer(genericContainerScreen);
            } else if (title.equals("Experimentation Table")) {
                tickExperimentationTable(genericContainerScreen);
            } else if (title.startsWith("Experiment Over")) {
                tickExperimentOver(genericContainerScreen);
            }
        } else {
            if (!currentScreenTitle.isEmpty()) {
                currentScreenTitle = "";
                screenOpenedAt = 0;
                autoExperiment = 0;
                resetAllSolvers();
            }
        }
    }

    /**
     * Tracks auto-mode progression across screen changes.
     */
    private void onScreenEntered(String title) {
        if (!auto.isToggled()) {
            autoExperiment = 0;
            return;
        }
        if (title.equals("Experimentation Table")) {
            // Back on the main menu: mark the experiment we just finished as done.
            if (autoExperiment == 1) {
                autoChronoDone = true;
            } else if (autoExperiment == 2) {
                autoUltraDone = true;
            } else {
                // Opened a fresh session from outside -> reset prior progress.
                autoChronoDone = false;
                autoUltraDone = false;
            }
            autoExperiment = 0;
        } else if (title.startsWith("Chronomatron")) {
            // Both the "Chronomatron -> Stakes" menu and the game itself belong to the Chronomatron flow.
            autoExperiment = 1;
        } else if (title.startsWith("Ultrasequencer")) {
            autoExperiment = 2;
        }
        // "Experiment Over" keeps autoExperiment unchanged so we know which experiment we came from.
    }

    private void tickExperimentationTable(AbstractContainerScreen<?> screen) {
        if (!auto.isToggled()) {
            return;
        }
        AbstractContainerMenu menu = screen.getMenu();

        // Skip an experiment when its item is glinted (already completed today).
        if (menu.getSlot(MAIN_CHRONO_GLINT).getItem().hasFoil()) {
            autoChronoDone = true;
        }
        if (menu.getSlot(MAIN_ULTRA_GLINT).getItem().hasFoil()) {
            autoUltraDone = true;
        }

        // If both have glint -> do nothing.
        if (autoChronoDone && autoUltraDone) {
            return;
        }

        // Small cooldown to avoid spamming duplicate clicks before the screen changes.
        if (System.currentTimeMillis() - lastNavClickAt < 400) {
            return;
        }

        // Chronomatron first, then Ultrasequencer.
        sendSlotClick(menu, autoChronoDone ? MAIN_ULTRA_BUTTON : MAIN_CHRONO_BUTTON);
        lastNavClickAt = System.currentTimeMillis();
    }

    private void tickChronomatronStakes(AbstractContainerScreen<?> screen) {
        if (!auto.isToggled()) {
            return;
        }
        // Small cooldown to avoid spamming duplicate clicks before the screen changes.
        if (System.currentTimeMillis() - lastNavClickAt < 400) {
            return;
        }
        sendSlotClick(screen.getMenu(), CHRONO_STAKE_SLOT);
        lastNavClickAt = System.currentTimeMillis();
    }

    private void tickUltrasequencerStakes(AbstractContainerScreen<?> screen) {
        if (!auto.isToggled()) {
            return;
        }
        if (System.currentTimeMillis() - lastNavClickAt < 400) {
            return;
        }
        sendSlotClick(screen.getMenu(), ULTRA_STAKE_SLOT);
        lastNavClickAt = System.currentTimeMillis();
    }

    private boolean isChronomatronStakesScreen(String title) {
        // The exact arrow symbol in "Chronomatron -> Stakes" is not known, match on both words.
        return title.startsWith("Chronomatron") && title.contains("Stakes");
    }

    private boolean isUltrasequencerStakesScreen(String title) {
        return title.startsWith("Ultrasequencer") && title.contains("Stakes");
    }

    private void tickExperimentOver(AbstractContainerScreen<?> screen) {
        if (auto.isToggled()) {
            pressEscape(screen);
        }
    }

    /**
     * Simulates pressing ESC: tells the server we closed the container and closes the local screen.
     * Note: {@code setScreen(null)} alone does NOT send a container close packet in this version.
     */
    private void pressEscape(AbstractContainerScreen<?> screen) {
        screen.onClose();
        mc.gui.setScreen(null);
    }

    private long getEffectiveDelay() {
        long baseDelay = (long) clickDelayMs.getInput();
        if (randomDelay.isToggled()) {
            // Add 10-30ms random delay
            baseDelay += 10 + random.nextInt(21); // 10-30 inclusive
        }
        return baseDelay;
    }

    private boolean shouldClick() {
        long currentTime = System.currentTimeMillis();

        // Handle initial delay
        if (isInitialDelay) {
            if (currentTime - startDelayTimer < startDelayMs.getInput()) {
                return false;
            }
            isInitialDelay = false;
            lastClickTime = currentTime;
            return true;
        }

        // Regular click delay
        if (currentTime - lastClickTime >= getEffectiveDelay()) {
            lastClickTime = currentTime;
            return true;
        }
        return false;
    }

    private void resetClickTiming() {
        lastClickTime = 0;
        startDelayTimer = System.currentTimeMillis();
        isInitialDelay = true;
    }

    private void tickChronomatron(AbstractContainerScreen<?> screen) {
        AbstractContainerMenu menu = screen.getMenu();

        chronoCurrentCycle = getChronoCycle(menu);
        Item currentModeItem = menu.getSlot(49).getItem().getItem();

        // Auto mode: quit when the round counter reaches 13 (minus serum levels).
        if (auto.isToggled() && chronoCurrentCycle >= getChronoQuitRound()) {
            pressEscape(screen);
            return;
        }

        if ((chronoCurrentCycle > 0 && currentModeItem == Items.GLOWSTONE) ||
                (chronoCurrentCycle == chronoLastCycle && currentModeItem != chronoLastModeItem)) {
            chronoStartSeconds = -1;
            resetClickTiming();
            if (!chronoGlintFound) {
                for (int i = 10; i < 43; i++) {
                    if (menu.getSlot(i).getItem().hasFoil()) {
                        chronoGlintFound = true;
                        chronoGlintFoundAt = i;
                        chronoClickStack.add(TERRACOTTA_TO_GLASS.get(menu.getSlot(i).getItem().getItem()));
                        break;
                    }
                }
            } else if (!menu.getSlot(chronoGlintFoundAt).getItem().hasFoil()) {
                chronoGlintFound = false;
                chronoGlintFoundAt = -1;
            }
        } else {
            if (chronoStartSeconds == -1) {
                chronoStartSeconds = menu.getSlot(49).getItem().getCount();
                resetClickTiming();
            }

            if (shouldClick() &&
                    menu.getSlot(49).getItem().getCount() < chronoStartSeconds) {
                inputChronomatronSequence(menu, screen);
            }
        }

        chronoLastCycle = chronoCurrentCycle;
        chronoLastModeItem = currentModeItem;
    }

    private int getChronoQuitRound() {
        return 13 - (int) serum.getInput();
    }

    private void inputChronomatronSequence(AbstractContainerMenu menu, AbstractContainerScreen<?> screen) {
        if (mc.player.containerMenu.getCarried().isEmpty()) {
            for (int i = 10; i < 43; i++) {
                if (!chronoClickStack.isEmpty()) {
                    if (menu.getSlot(i).getItem().getItem() == chronoClickStack.get(0)) {
                        sendSlotClick(menu, i);
                        chronoClickStack.remove(0);
                        break;
                    }
                }
            }
        }
    }

    private void tickUltrasequencer(AbstractContainerScreen<?> screen) {
        AbstractContainerMenu menu = screen.getMenu();
        Item currentModeItem = menu.getSlot(49).getItem().getItem();

        // Auto mode: quit when the current sequence length reaches 9 (minus serum levels).
        if (auto.isToggled() && ultraClickStack.size() >= getUltraQuitRound()) {
            pressEscape(screen);
            return;
        }

        if (currentModeItem == Items.GLOWSTONE) {
            ultraStartSeconds = -1;
            resetClickTiming();
            for (int i = 0; i < 45; i++) {
                ItemStack stack = menu.getSlot(i).getItem();
                if (!BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().contains("pane")) {
                    if (stack.getCount() == (ultraClickStack.size() + 1)) {
                        ultraClickStack.add(i);
                    }
                }
            }
        } else if (currentModeItem == Items.CLOCK) {
            if (ultraStartSeconds == -1) {
                ultraStartSeconds = menu.getSlot(49).getItem().getCount();
                resetClickTiming();
            }

            if (shouldClick() &&
                    menu.getSlot(49).getItem().getCount() < ultraStartSeconds) {
                inputUltrasequencerSequence(screen);
            }
        }
    }

    private int getUltraQuitRound() {
        return 10 - (int) serum.getInput();
    }

    private void inputUltrasequencerSequence(AbstractContainerScreen<?> screen) {
        if (mc.player.containerMenu.getCarried().isEmpty()) {
            AbstractContainerMenu menu = screen.getMenu();
            for (int i = 0; i < 45; i++) {
                if (!ultraClickStack.isEmpty()) {
                    if (i == ultraClickStack.get(0)) {
                        sendSlotClick(menu, i);
                        ultraClickStack.remove(0);
                        break;
                    }
                }
            }
        }
    }

    /**
     * Sends a left-click (button 0 / PICKUP) on a slot, matching the existing solver click pattern.
     */
    private void sendSlotClick(AbstractContainerMenu menu, int slot) {
        if (mc.getConnection() == null) {
            return;
        }
        HashedPatchMap.HashGenerator hasher = mc.getConnection().decoratedHashOpsGenenerator();
        HashedStack carriedHashed = HashedStack.create(menu.getCarried(), hasher);
        Int2ObjectMap<HashedStack> changedSlots = new Int2ObjectOpenHashMap<>();
        mc.getConnection().send(new ServerboundContainerClickPacket(
                menu.containerId,
                menu.getStateId(),
                (short) slot,
                (byte) 0,
                ContainerInput.PICKUP,
                changedSlots,
                carriedHashed
        ));
    }

    private int getChronoCycle(AbstractContainerMenu menu) {
        return menu.getSlot(4).getItem().getCount();
    }

    private void resetAllSolvers() {
        chronoClickStack.clear();
        chronoGlintFound = false;
        chronoGlintFoundAt = -1;
        chronoLastCycle = 0;
        chronoCurrentCycle = 0;
        chronoLastModeItem = null;
        chronoStartSeconds = -1;

        ultraClickStack.clear();
        ultraStartSeconds = -1;

        isChronomatronActive = false;
        isUltrasequencerActive = false;

        resetClickTiming();
    }

    @Override
    public void onDisable() {
        currentScreenTitle = "";
        screenOpenedAt = 0;
        autoExperiment = 0;
        autoChronoDone = false;
        autoUltraDone = false;
        lastNavClickAt = 0;
        resetAllSolvers();
    }
}
