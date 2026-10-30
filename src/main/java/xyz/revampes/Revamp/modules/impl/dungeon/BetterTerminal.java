package xyz.revampes.Revamp.modules.impl.dungeon;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import org.joml.Matrix3x2fStack;
import org.lwjgl.glfw.GLFW;
import xyz.revampes.Revamp.events.impl.ContainerMouseClickEvent;
import xyz.revampes.Revamp.events.impl.ContainerScreenRenderEvent;
import xyz.revampes.Revamp.events.impl.PreUpdateEvent;
import xyz.revampes.Revamp.modules.Module;
import xyz.revampes.Revamp.settings.impl.ButtonSetting;
import xyz.revampes.Revamp.settings.impl.ColorSetting;
import xyz.revampes.Revamp.settings.impl.InputSetting;
import xyz.revampes.Revamp.settings.impl.SelectSetting;
import xyz.revampes.Revamp.settings.impl.SliderSetting;
import xyz.revampes.Revamp.utility.RenderUtils;
import xyz.revampes.Revamp.utility.dungeon.terminal.ExponentialSmooth;
import xyz.revampes.Revamp.utility.dungeon.terminal.SlotRenderInfo;
import xyz.revampes.Revamp.utility.dungeon.terminal.SlotType;
import xyz.revampes.Revamp.utility.dungeon.terminal.TerminalColors;
import xyz.revampes.Revamp.utility.dungeon.terminal.TerminalHandler;
import xyz.revampes.Revamp.utility.dungeon.terminal.TerminalManager;
import xyz.revampes.Revamp.utility.dungeon.terminal.TerminalType;

import java.awt.Color;
import java.util.List;

/**
 * Better Terminal — yqloss' 1.8.9 terminal UI on top of Odin's 26.x solver.
 *
 * <p>The <em>look and feel</em> is a 1:1 port of {@code yqlossclientmixinkt}:
 * {@code BetterTerminal}/{@code BetterTerminalScreen} for the panel, its geometry, colours,
 * cross-fading title, NQ/RS buttons and drag-click, and {@code Button}/{@code Fade}/
 * {@code ExponentialSmooth} for the animations.
 *
 * <p>The <em>solving and clicking</em> is Odin's {@code TerminalSolver}/terminal handlers,
 * because the Floor 7 terminals changed with 26.x: panes are per-colour items instead of
 * metadata, and clicks are sent as {@code ContainerInput.CLONE} with a prediction queue
 * instead of yqloss' window-id-matched queue.
 */
public class BetterTerminal extends Module {

    // ------------------------------------------------------------------ layout
    // Every number below is taken verbatim from yqloss' BetterTerminalScreen so the panel
    // lands where the 1.8.9 one did.

    private static final float PANEL_WIDTH = 212.0f;
    private static final float PANEL_OFFSET_X = -18.0f;
    private static final float PANEL_OFFSET_Y = -10.0f;

    private static final float SLOT_SIZE = 16.0f;
    private static final float SLOT_PITCH = 18.0f;
    private static final float SLOT_ORIGIN_X = 8.0f;
    private static final float SLOT_ORIGIN_Y = 18.0f;

    private static final float TITLE_CENTER_X = 88.0f;
    private static final float TITLE_Y = 2.0f;

    /** Panel centre relative to the screen centre; {@code PANEL_WIDTH / 2}. */
    private static final float PANEL_CENTER_X = 88.0f;

    private static final float BUTTON_NQ_X = -10.0f;
    private static final float BUTTON_NQ_Y = -2.0f;
    private static final float BUTTON_RS_X = 170.0f;
    private static final float BUTTON_RS_Y = -2.0f;

    private static final double SMOOTH_SPEED = 0.5;

    /** yqloss' button accents: red for the non-queue toggle, yellow for the reload button. */
    private static final int COLOR_NON_QUEUE = 0xFFFA5252;
    private static final int COLOR_RELOAD = 0xFFFAB005;
    private static final int COLOR_NONE = 0x00000000;

    // ------------------------------------------------------------------ settings

    // Behaviour (Odin's solver settings).
    public final ButtonSetting clickPrediction = new ButtonSetting("Click Prediction", true);
    public final SliderSetting resolveTimeout =
            new SliderSetting("Resolve Timeout", "ms", 600, 200, 1200, 10);
    public final SliderSetting firstClickProt =
            new SliderSetting("First Click Prot", "ms", 200, 0, 600, 10);
    public final ButtonSetting lagProtection = new ButtonSetting("Server Lag Protection", false);
    public final SliderSetting lagProtectionTicks =
            new SliderSetting("Lag Protection", "ticks", 8, 7, 16, 1);
    public final SelectSetting rubixMode =
            new SelectSetting("Rubix Mode", 1, new String[]{"Fewest Clicks", "Left Clicks Only"});

    // Terminal toggles + per-terminal options.
    public final ButtonSetting orderEnabled = new ButtonSetting("Order Terminal", true);
    public final ButtonSetting orderShowNumber = new ButtonSetting("Order Show Number", true);
    public final ButtonSetting orderShowClickedNumber = new ButtonSetting("Order Show Clicked Number", false);
    public final ButtonSetting orderSmooth = new ButtonSetting("Order Smooth GUI", true);

    public final ButtonSetting panesEnabled = new ButtonSetting("Panes Terminal", true);
    public final ButtonSetting panesSmooth = new ButtonSetting("Panes Smooth GUI", true);

    public final ButtonSetting startEnabled = new ButtonSetting("Starts With Terminal", true);
    public final ButtonSetting startSmooth = new ButtonSetting("Starts With Smooth GUI", true);

    public final ButtonSetting colorEnabled = new ButtonSetting("Select All Terminal", true);
    public final ButtonSetting colorSmooth = new ButtonSetting("Select All Smooth GUI", true);

    public final ButtonSetting rubixEnabled = new ButtonSetting("Rubix Terminal", true);
    public final ButtonSetting rubixShowNumber = new ButtonSetting("Rubix Show Number", true);
    public final ButtonSetting rubixSmooth = new ButtonSetting("Rubix Smooth GUI", true);

    public final ButtonSetting alignEnabled = new ButtonSetting("Align Terminal", true);
    public final ButtonSetting alignSmooth = new ButtonSetting("Align Smooth GUI", true);

    // Presentation.
    public final SliderSetting guiScale = new SliderSetting("GUI Scale", 2.0, 0.5, 3.0, 0.05);
    public final SliderSetting cornerRadius = new SliderSetting("Slot Roundness", 4.0, 0.0, 8.0, 1.0);
    public final ButtonSetting backgroundEnabled = new ButtonSetting("Background", true);
    public final SliderSetting backgroundRadius = new SliderSetting("Background Roundness", 12.0, 0.0, 20.0, 1.0);
    public final ButtonSetting showChest = new ButtonSetting("Show Vanilla Chest GUI", true);
    public final SliderSetting chestScale = new SliderSetting("Vanilla Chest Scale", 0.25, 0.05, 1.0, 0.05);
    public final ButtonSetting dragClick = new ButtonSetting("Drag Click", false);

    // Sound.
    public final ButtonSetting clickSound = new ButtonSetting("Click Sound", true);
    public final InputSetting clickSoundId =
            new InputSetting("Click Sound Id", 64, "entity.blaze.hurt", "entity.blaze.hurt");
    public final SliderSetting clickVolume = new SliderSetting("Click Volume", 1.0, 0.0, 2.0, 0.05);
    public final SliderSetting clickPitch = new SliderSetting("Click Pitch", 1.0, 0.5, 2.0, 0.05);
    public final ButtonSetting completeSound = new ButtonSetting("Complete Sound", false);
    public final InputSetting completeSoundId =
            new InputSetting("Complete Sound Id", 64, "entity.experience_orb.pickup", "entity.experience_orb.pickup");

    // Colours: yqloss' Tremor palette defaults.
    public final ColorSetting order1 = new ColorSetting("Order 1st", hex("40C057"));
    public final ColorSetting order2 = new ColorSetting("Order 2nd", hex("FAB005"));
    public final ColorSetting order3 = new ColorSetting("Order 3rd", hex("FA5252"));
    public final ColorSetting orderClicked = new ColorSetting("Order Clicked", hex("00000000"));
    public final ColorSetting orderOther = new ColorSetting("Order Other", hex("343A40"));

    public final ColorSetting panesOn = new ColorSetting("Panes Correct", hex("40C057"));
    public final ColorSetting panesOff = new ColorSetting("Panes Wrong", hex("FA5252"));

    public final ColorSetting startCorrect = new ColorSetting("Starts With Correct", hex("40C057"));
    public final ColorSetting startClicked = new ColorSetting("Starts With Clicked", hex("343A40"));
    public final ColorSetting startOther = new ColorSetting("Starts With Other", hex("00000000"));

    public final ColorSetting colorCorrect = new ColorSetting("Select Correct", hex("40C057"));
    public final ColorSetting colorClicked = new ColorSetting("Select Clicked", hex("343A40"));
    public final ColorSetting colorOther = new ColorSetting("Select Other", hex("00000000"));

    public final ColorSetting rubixRight2 = new ColorSetting("Rubix Right 2", hex("20C997"));
    public final ColorSetting rubixRight1 = new ColorSetting("Rubix Right 1", hex("099268"));
    public final ColorSetting rubix0 = new ColorSetting("Rubix Correct", hex("343A40"));
    public final ColorSetting rubixLeft1 = new ColorSetting("Rubix Left 1", hex("3B5BDB"));
    public final ColorSetting rubixLeft2 = new ColorSetting("Rubix Left 2", hex("5C7CFA"));

    public final ColorSetting alignTarget = new ColorSetting("Align Target", hex("BE4BDB"));
    public final ColorSetting alignActiveCurrent = new ColorSetting("Align Current", hex("40C057"));
    public final ColorSetting alignActiveOther = new ColorSetting("Align Other", hex("FA5252"));
    public final ColorSetting alignInactive = new ColorSetting("Align Inactive", hex("343A40"));
    public final ColorSetting alignActiveButton = new ColorSetting("Align Active Button", hex("40C057"));
    public final ColorSetting alignInactiveButton = new ColorSetting("Align Inactive Button", hex("343A40"));

    public final ColorSetting backgroundColor = new ColorSetting("Background Color", hex("212529"));
    public final ColorSetting textColor = new ColorSetting("Text Color", hex("DEE2E6"));

    // ------------------------------------------------------------------ runtime

    private final TerminalButton[] slotButtons = new TerminalButton[9 * 9];
    private final TerminalButton buttonNonQueue = new TerminalButton();
    private final TerminalButton buttonReload = new TerminalButton();
    private final ExponentialSmooth titleProgress = new ExponentialSmooth(0.0);

    private String titleLast = "";
    private String titleCurrent = "";
    private int dragLastCell = -1;

    public BetterTerminal() {
        super("Better Terminal", category.Dungeon);

        registerSetting(clickPrediction);
        registerSetting(resolveTimeout);
        registerSetting(firstClickProt);
        registerSetting(lagProtection);
        registerSetting(lagProtectionTicks);
        registerSetting(rubixMode);

        registerSetting(orderEnabled);
        registerSetting(orderShowNumber);
        registerSetting(orderShowClickedNumber);
        registerSetting(orderSmooth);
        registerSetting(panesEnabled);
        registerSetting(panesSmooth);
        registerSetting(startEnabled);
        registerSetting(startSmooth);
        registerSetting(colorEnabled);
        registerSetting(colorSmooth);
        registerSetting(rubixEnabled);
        registerSetting(rubixShowNumber);
        registerSetting(rubixSmooth);
        registerSetting(alignEnabled);
        registerSetting(alignSmooth);

        registerSetting(guiScale);
        registerSetting(cornerRadius);
        registerSetting(backgroundEnabled);
        registerSetting(backgroundRadius);
        registerSetting(showChest);
        registerSetting(chestScale);
        registerSetting(dragClick);

        registerSetting(clickSound);
        registerSetting(clickSoundId);
        registerSetting(clickVolume);
        registerSetting(clickPitch);
        registerSetting(completeSound);
        registerSetting(completeSoundId);

        registerSetting(order1);
        registerSetting(order2);
        registerSetting(order3);
        registerSetting(orderClicked);
        registerSetting(orderOther);
        registerSetting(panesOn);
        registerSetting(panesOff);
        registerSetting(startCorrect);
        registerSetting(startClicked);
        registerSetting(startOther);
        registerSetting(colorCorrect);
        registerSetting(colorClicked);
        registerSetting(colorOther);
        registerSetting(rubixRight2);
        registerSetting(rubixRight1);
        registerSetting(rubix0);
        registerSetting(rubixLeft1);
        registerSetting(rubixLeft2);
        registerSetting(alignTarget);
        registerSetting(alignActiveCurrent);
        registerSetting(alignActiveOther);
        registerSetting(alignInactive);
        registerSetting(alignActiveButton);
        registerSetting(alignInactiveButton);
        registerSetting(backgroundColor);
        registerSetting(textColor);
    }

    /**
     * Parses yqloss' 8-digit {@code RRGGBBAA} palette literals, tolerating 6-digit
     * {@code RRGGBB} ones too (those are treated as fully opaque).
     */
    private static Color hex(String argb) {
        long value = Long.parseLong(argb, 16);
        int alpha = argb.length() >= 8 ? (int) ((value >> 24) & 0xFF) : 0xFF;
        return new Color((int) ((value >> 16) & 0xFF), (int) ((value >> 8) & 0xFF),
                (int) (value & 0xFF), alpha);
    }

    @Override
    public String getDesc() {
        return "yqloss-style terminal UI with Odin's 26.x solver.";
    }

    @Override
    public String getInfo() {
        TerminalHandler handler = TerminalManager.current();
        return handler == null ? "" : handler.type().name();
    }

    @Override
    public void guiUpdate() {
        lagProtectionTicks.setVisibilityCondition(lagProtection::isToggled);
        chestScale.setVisibilityCondition(showChest::isToggled);
        backgroundRadius.setVisibilityCondition(backgroundEnabled::isToggled);

        orderShowNumber.setVisibilityCondition(orderEnabled::isToggled);
        orderShowClickedNumber.setVisibilityCondition(orderEnabled::isToggled);
        orderSmooth.setVisibilityCondition(orderEnabled::isToggled);
        panesSmooth.setVisibilityCondition(panesEnabled::isToggled);
        startSmooth.setVisibilityCondition(startEnabled::isToggled);
        rubixShowNumber.setVisibilityCondition(rubixEnabled::isToggled);
        rubixSmooth.setVisibilityCondition(rubixEnabled::isToggled);
        alignSmooth.setVisibilityCondition(alignEnabled::isToggled);
    }

    @Override
    public void onDisable() {
        TerminalManager.close();
        resetAnimations();
    }

    @Override
    public void onEnable() {
        resetAnimations();
    }

    // ------------------------------------------------------------------ events

    @EventHandler
    public void onPreUpdate(PreUpdateEvent event) {
        refresh(asContainerScreen());
        TerminalManager.tick(this);
    }

    @EventHandler
    public void onContainerRender(ContainerScreenRenderEvent event) {
        refresh(event.screen());
        if (TerminalManager.currentScreen() != event.screen()) return;

        TerminalHandler handler = TerminalManager.current();
        if (handler == null) return;

        drawPanel(event.drawContext(), event.mouseX(), event.mouseY(), handler);
    }

    @EventHandler
    public void onContainerMouseClick(ContainerMouseClickEvent event) {
        if (!isTerminalScreen(event.getScreen())) return;

        double mouseX = event.getMouseX();
        double mouseY = event.getMouseY();

        if (dragClick.isToggled() && event.getButton() == 0) {
            // In drag-click mode the press itself does nothing; the per-frame hover sweep
            // below fires the clicks as the cursor crosses cells, exactly like yqloss.
            dragLastCell = -1;
            event.setCancelled(true);
            return;
        }

        if (handleClick(mouseX, mouseY, event.getButton())) {
            event.setCancelled(true);
        }
    }

    /**
     * Keeps the solver in sync with whatever container screen is open. Called from the render
     * hook (before the vanilla contents are drawn, so the chest can be hidden on the very
     * first frame) and from the tick.
     */
    public void refresh(AbstractContainerScreen<?> screen) {
        TerminalManager.update(isEnabled() ? screen : null, this);
    }

    public boolean isTerminalScreen(AbstractContainerScreen<?> screen) {
        return isEnabled() && screen != null
                && TerminalManager.current() != null
                && TerminalManager.currentScreen() == screen;
    }

    /** Whether the panel replaces the vanilla chest for this screen. */
    public boolean hidesVanillaChest(AbstractContainerScreen<?> screen) {
        return isTerminalScreen(screen) && !showChest.isToggled();
    }

    /** Thumbnail scale for the vanilla chest when it is shown next to the panel (1.0 = none). */
    public float chestDisplayScale(AbstractContainerScreen<?> screen) {
        if (!isTerminalScreen(screen) || !showChest.isToggled()) return 1.0f;
        float scale = (float) chestScale.getInput();
        return scale <= 0.05f || scale >= 0.999f ? 1.0f : scale;
    }

    /** Called by {@link TerminalManager} when a different terminal takes over. */
    public void onTerminalChanged(TerminalHandler handler) {
        resetAnimations();
        dragLastCell = -1;
        titleLast = "";
        titleCurrent = "";
        titleProgress.reset(0.0);
    }

    /** Called by a handler after a click was actually sent, so the module can play the sound. */
    public void onTerminalClick(boolean completed) {
        if (completed && completeSound.isToggled()) {
            playSound(completeSoundId.getValue(), 1.0f, 1.0f);
        } else if (clickSound.isToggled()) {
            playSound(clickSoundId.getValue(),
                    (float) clickVolume.getInput(), (float) clickPitch.getInput());
        }
    }

    private void playSound(String id, float volume, float pitch) {
        if (id == null || id.isBlank()) return;
        Identifier location = Identifier.tryParse(id.trim());
        if (location == null) return;
        SoundEvent sound = SoundEvent.createVariableRangeEvent(location);
        mc.getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
    }

    private AbstractContainerScreen<?> asContainerScreen() {
        return mc.gui.screen() instanceof AbstractContainerScreen<?> screen ? screen : null;
    }

    // ------------------------------------------------------------------ input

    private boolean handleClick(double mouseX, double mouseY, int button) {
        TerminalHandler handler = TerminalManager.current();
        if (handler == null) return false;

        float scale = currentScale();
        double[] base = toBase(mouseX, mouseY, scale);
        double bx = base[0];
        double by = base[1];

        float ttx = panelOriginX();
        float tty = panelOriginY(handler.type());

        if (insidePanel(bx, by, ttx, tty, handler.type())) {
            if (insideBox(bx, by, ttx + BUTTON_NQ_X - SLOT_SIZE / 2.0f, tty + BUTTON_NQ_Y - SLOT_SIZE / 2.0f, SLOT_SIZE)) {
                handler.forceNonQueue();
                return true;
            }
            if (insideBox(bx, by, ttx + BUTTON_RS_X - SLOT_SIZE / 2.0f, tty + BUTTON_RS_Y - SLOT_SIZE / 2.0f, SLOT_SIZE)) {
                reload(handler);
                return true;
            }

            int slot = cellAt(bx, by, ttx, tty, handler);
            if (slot >= 0) {
                handler.click(slot, button);
                return true;
            }
            return true;
        }

        return false;
    }

    private void reload(TerminalHandler handler) {
        handler.clearPredictions();
        handler.recompute();
    }

    /** Container slot index under the cursor, or -1. Slot indices double as grid indices. */
    private int cellAt(double bx, double by, float ttx, float tty, TerminalHandler handler) {
        TerminalType type = handler.type();
        List<SlotRenderInfo> grid = handler.render();
        int begin = type.getBeginLine() * 9;

        for (int i = begin; i < grid.size(); i++) {
            if (grid.get(i).getType() == SlotType.EMPTY) continue;

            int drawn = i - begin;
            float x = ttx + SLOT_ORIGIN_X + SLOT_PITCH * (drawn % 9);
            float y = tty + SLOT_ORIGIN_Y + SLOT_PITCH * (drawn / 9);
            if (insideBox(bx, by, x, y, SLOT_SIZE)) return i;
        }
        return -1;
    }

    // ------------------------------------------------------------------ drawing

    private void drawPanel(GuiGraphicsExtractor ctx, int mouseX, int mouseY, TerminalHandler handler) {
        TerminalType type = handler.type();
        List<SlotRenderInfo> grid = handler.render();

        float scale = currentScale();
        float cx = mc.getWindow().getGuiScaledWidth() / 2.0f;
        float cy = mc.getWindow().getGuiScaledHeight() / 2.0f;

        float ttx = panelOriginX();
        float tty = panelOriginY(type);
        float panelHeight = 40.0f + SLOT_PITCH * (type.getLines() - type.getBeginLine());

        double[] base = toBase(mouseX, mouseY, scale);
        double bx = base[0];
        double by = base[1];

        boolean anyDown = isMouseDown(0) || isMouseDown(1) || isMouseDown(2);
        boolean smooth = isSmooth(type);

        Matrix3x2fStack pose = ctx.pose();
        pose.pushMatrix();
        if (scale != 1.0f) {
            pose.translate(cx, cy);
            pose.scale(scale, scale);
            pose.translate(-cx, -cy);
        }

        if (backgroundEnabled.isToggled()) {
            RenderUtils.drawRoundedRect(ctx,
                    ttx + PANEL_OFFSET_X, tty + PANEL_OFFSET_Y,
                    PANEL_WIDTH, panelHeight,
                    (float) backgroundRadius.getInput(),
                    backgroundColor.getRGB());
        }

        drawTitle(ctx, ttx, tty, handler, bx, by, smooth);
        drawButtons(ctx, ttx, tty, handler, bx, by, anyDown, smooth);
        drawSlots(ctx, ttx, tty, grid, type, bx, by, anyDown, smooth);

        pose.popMatrix();

        if (dragClick.isToggled()) updateDragClick(bx, by, ttx, tty, handler);
    }

    private void drawSlots(GuiGraphicsExtractor ctx, float ttx, float tty,
                           List<SlotRenderInfo> grid, TerminalType type,
                           double bx, double by, boolean anyDown, boolean smooth) {
        int begin = type.getBeginLine() * 9;
        int textArgb = textColor.getRGB();

        for (int i = begin; i < grid.size() && i < slotButtons.length; i++) {
            SlotRenderInfo info = grid.get(i);
            TerminalButton button = slotButton(i);

            Color color = colorFor(info.getType());
            if (color == null) {
                button.reset();
                continue;
            }

            int drawn = i - begin;
            float x = ttx + SLOT_ORIGIN_X + SLOT_PITCH * (drawn % 9);
            float y = tty + SLOT_ORIGIN_Y + SLOT_PITCH * (drawn / 9);

            boolean hovered = insideBox(bx, by, x, y, SLOT_SIZE);
            double size = smooth ? button.size(hovered, anyDown) : (hovered && anyDown ? 0.9 : 1.0);
            int argb = smooth
                    ? button.color(color.getRGB(), hovered, anyDown)
                    : color.getRGB();

            drawCell(ctx, x, y, size, argb);

            // yqloss keeps the label on transparent cells too, which is how the already
            // clicked numbers stay readable (ORDER_CLICKED defaults to a fully clear colour).
            if (info.getText() != null) {
                ctx.centeredText(mc.font, info.getText(),
                        (int) (x + SLOT_SIZE / 2.0f),
                        (int) (y + 4.0f),
                        textArgb);
            }
        }
    }

    private void drawCell(GuiGraphicsExtractor ctx, float x, float y, double size, int argb) {
        if ((argb >>> 24) == 0) return;

        float half = SLOT_SIZE / 2.0f;
        float halfScaled = (float) (half * size);
        float left = x + half - halfScaled;
        float top = y + half - halfScaled;
        float radius = (float) cornerRadius.getInput();

        RenderUtils.drawRoundedRect(ctx, left, top, halfScaled * 2.0f, halfScaled * 2.0f,
                radius, argb);
    }

    private void drawTitle(GuiGraphicsExtractor ctx, float ttx, float tty,
                           TerminalHandler handler, double bx, double by, boolean smooth) {
        String title;
        if (insideBox(bx, by, ttx + BUTTON_NQ_X - SLOT_SIZE / 2.0f, tty + BUTTON_NQ_Y - SLOT_SIZE / 2.0f, SLOT_SIZE)) {
            title = handler.prediction()
                    ? "[Frosty] Switch to non-queue mode."
                    : "[Frosty] Already in non-queue mode.";
        } else if (insideBox(bx, by, ttx + BUTTON_RS_X - SLOT_SIZE / 2.0f, tty + BUTTON_RS_Y - SLOT_SIZE / 2.0f, SLOT_SIZE)) {
            title = "[Frosty] Reload terminal state.";
        } else {
            title = "[Frosty] " + handler.title();
        }

        switchTitle(title);

        double progress = smooth ? titleProgress.approach(1.0, SMOOTH_SPEED) : 1.0;
        if (!smooth) titleProgress.reset(0.0);

        int argb = textColor.getRGB();
        int center = (int) (ttx + TITLE_CENTER_X);
        int y = (int) (tty + TITLE_Y);

        if (progress < 1.0) {
            ctx.centeredText(mc.font, titleLast, center, y, TerminalColors.withAlpha(argb, 1.0 - progress));
        }
        ctx.centeredText(mc.font, titleCurrent, center, y, TerminalColors.withAlpha(argb, progress));
    }

    private void switchTitle(String value) {
        if (value.equals(titleCurrent)) return;
        titleLast = titleCurrent;
        titleCurrent = value;
        titleProgress.set(1.0 - titleProgress.value());
    }

    private void drawButtons(GuiGraphicsExtractor ctx, float ttx, float tty, TerminalHandler handler,
                             double bx, double by, boolean anyDown, boolean smooth) {
        // NQ: drop the click prediction for this terminal, like yqloss' non-queue mode.
        boolean nqHovered = insideBox(bx, by, ttx + BUTTON_NQ_X - SLOT_SIZE / 2.0f, tty + BUTTON_NQ_Y - SLOT_SIZE / 2.0f, SLOT_SIZE);
        int nqBase = (nqHovered || !handler.prediction()) ? COLOR_NON_QUEUE : COLOR_NONE;
        drawButton(ctx, ttx + BUTTON_NQ_X, tty + BUTTON_NQ_Y, "NQ",
                buttonNonQueue, nqBase, nqHovered, anyDown, smooth);

        // RS: reload the terminal state; shows the number of unconfirmed clicks.
        boolean rsHovered = insideBox(bx, by, ttx + BUTTON_RS_X - SLOT_SIZE / 2.0f, tty + BUTTON_RS_Y - SLOT_SIZE / 2.0f, SLOT_SIZE);
        int rsBase = rsHovered ? COLOR_RELOAD : COLOR_NONE;
        int queueSize = handler.queueSize();
        drawButton(ctx, ttx + BUTTON_RS_X, tty + BUTTON_RS_Y, queueSize > 0 ? Integer.toString(queueSize) : "RS",
                buttonReload, rsBase, rsHovered, anyDown, smooth);
    }

    private void drawButton(GuiGraphicsExtractor ctx, float centerX, float centerY, String label,
                            TerminalButton button, int argb, boolean hovered, boolean anyDown, boolean smooth) {
        double size = smooth ? button.size(hovered, anyDown) : (hovered && anyDown ? 0.9 : 1.0);
        int color = smooth ? button.color(argb, hovered, anyDown) : argb;

        float half = (float) (SLOT_SIZE / 2.0 * size);
        RenderUtils.drawRoundedRect(ctx, centerX - half, centerY - half, half * 2.0f, half * 2.0f,
                (float) cornerRadius.getInput(), color);

        ctx.centeredText(mc.font, label, (int) centerX, (int) (centerY - 4), textColor.getRGB());
    }

    private void updateDragClick(double bx, double by, float ttx, float tty, TerminalHandler handler) {
        int dragButton = isMouseDown(1) ? 1 : isMouseDown(0) ? 0 : -1;
        if (dragButton < 0) {
            dragLastCell = -1;
            return;
        }

        int cell = cellAt(bx, by, ttx, tty, handler);
        if (cell >= 0 && cell != dragLastCell) handler.click(cell, dragButton);
        dragLastCell = cell;
    }

    // ------------------------------------------------------------------ helpers

    private TerminalButton slotButton(int index) {
        if (slotButtons[index] == null) slotButtons[index] = new TerminalButton();
        return slotButtons[index];
    }

    private void resetAnimations() {
        for (TerminalButton button : slotButtons) {
            if (button != null) button.reset();
        }
        buttonNonQueue.reset();
        buttonReload.reset();
    }

    private boolean isSmooth(TerminalType type) {
        switch (type) {
            case NUMBERS:
                return orderSmooth.isToggled();
            case PANES:
                return panesSmooth.isToggled();
            case STARTS_WITH:
                return startSmooth.isToggled();
            case SELECT:
                return colorSmooth.isToggled();
            case RUBIX:
                return rubixSmooth.isToggled();
            case MELODY:
                return alignSmooth.isToggled();
            default:
                return true;
        }
    }

    private Color colorFor(SlotType type) {
        switch (type) {
            case ORDER_1:
                return order1.getColor();
            case ORDER_2:
                return order2.getColor();
            case ORDER_3:
                return order3.getColor();
            case ORDER_CLICKED:
                return orderClicked.getColor();
            case ORDER_OTHER:
                return orderOther.getColor();
            case PANES_ON:
                return panesOn.getColor();
            case PANES_OFF:
                return panesOff.getColor();
            case START_CORRECT:
                return startCorrect.getColor();
            case START_CLICKED:
                return startClicked.getColor();
            case START_WRONG:
                return startOther.getColor();
            case COLOR_CORRECT:
                return colorCorrect.getColor();
            case COLOR_CLICKED:
                return colorClicked.getColor();
            case COLOR_WRONG:
                return colorOther.getColor();
            case RUBIX_RIGHT_2:
                return rubixRight2.getColor();
            case RUBIX_RIGHT_1:
                return rubixRight1.getColor();
            case RUBIX_CORRECT:
                return rubix0.getColor();
            case RUBIX_LEFT_1:
                return rubixLeft1.getColor();
            case RUBIX_LEFT_2:
                return rubixLeft2.getColor();
            case ALIGN_TARGET:
                return alignTarget.getColor();
            case ALIGN_ACTIVE_CURRENT:
                return alignActiveCurrent.getColor();
            case ALIGN_ACTIVE_OTHER:
                return alignActiveOther.getColor();
            case ALIGN_INACTIVE:
                return alignInactive.getColor();
            case ALIGN_ACTIVE_BUTTON:
                return alignActiveButton.getColor();
            case ALIGN_INACTIVE_BUTTON:
                return alignInactiveButton.getColor();
            default:
                return null;
        }
    }

    private float currentScale() {
        float scale = (float) guiScale.getInput();
        return scale <= 0.05f ? 1.0f : scale;
    }

    /**
     * yqloss lays the panel out inside a box {@code 113 + 18 * chestLines} tall centred on the
     * screen, with the slot grid offset by {@code beginLine} rows.
     */
    private float panelOriginX() {
        return mc.getWindow().getGuiScaledWidth() / 2.0f - PANEL_CENTER_X;
    }

    /**
     * Vertical origin of the panel.
     *
     * <p>With the vanilla chest hidden the panel itself is centred on screen. While the chest
     * is shown (or shrunk to its thumbnail) yqloss' original placement is kept, because the
     * panel is meant to sit over it: a {@code 113 + 18 * chestLines} box centred on the screen,
     * offset by {@code beginLine} rows.
     */
    private float panelOriginY(TerminalType type) {
        float cy = mc.getWindow().getGuiScaledHeight() / 2.0f;
        float panelHeight = 40.0f + SLOT_PITCH * (type.getLines() - type.getBeginLine());

        if (hidesVanillaChest(TerminalManager.currentScreen())) {
            return cy - panelHeight / 2.0f - PANEL_OFFSET_Y;
        }

        float chestHeight = 113.0f + SLOT_PITCH * type.getChestLines();
        return cy - chestHeight / 2.0f + SLOT_PITCH * type.getBeginLine();
    }

    /** Undoes the GUI-scale transform so hover and click tests use panel coordinates. */
    private double[] toBase(double mouseX, double mouseY, float scale) {
        float cx = mc.getWindow().getGuiScaledWidth() / 2.0f;
        float cy = mc.getWindow().getGuiScaledHeight() / 2.0f;
        return new double[]{
                (mouseX - cx) / scale + cx,
                (mouseY - cy) / scale + cy
        };
    }

    private boolean insidePanel(double bx, double by, float ttx, float tty, TerminalType type) {
        float panelHeight = 40.0f + SLOT_PITCH * (type.getLines() - type.getBeginLine());
        return bx >= ttx + PANEL_OFFSET_X && bx <= ttx + PANEL_OFFSET_X + PANEL_WIDTH
                && by >= tty + PANEL_OFFSET_Y && by <= tty + PANEL_OFFSET_Y + panelHeight;
    }

    private static boolean insideBox(double bx, double by, float x, float y, float size) {
        return bx >= x && bx < x + size && by >= y && by < y + size;
    }

    private static boolean isMouseDown(int button) {
        long handle = mc.getWindow().handle();
        if (handle == 0) return false;
        return GLFW.glfwGetMouseButton(handle, button) == GLFW.GLFW_PRESS;
    }

    /**
     * One animated terminal cell. Port of yqloss' {@code Button}: colour channels ease with
     * {@code approachOrSet}, the hover/press lightness with {@code approach}, and the pressed
     * size dips to 0.9.
     */
    private static final class TerminalButton {

        private final ExponentialSmooth red = new ExponentialSmooth(0.0);
        private final ExponentialSmooth green = new ExponentialSmooth(0.0);
        private final ExponentialSmooth blue = new ExponentialSmooth(0.0);
        private final ExponentialSmooth alpha = new ExponentialSmooth(0.0);
        private final ExponentialSmooth lightness = new ExponentialSmooth(0.0);
        private final ExponentialSmooth scale = new ExponentialSmooth(1.0);

        double size(boolean hovered, boolean anyDown) {
            return scale.approach(hovered && anyDown ? 0.9 : 1.0, SMOOTH_SPEED);
        }

        int color(int target, boolean hovered, boolean anyDown) {
            double[] components = TerminalColors.toComponents(target);
            double r = red.approachOrSet(components[0], SMOOTH_SPEED);
            double g = green.approachOrSet(components[1], SMOOTH_SPEED);
            double b = blue.approachOrSet(components[2], SMOOTH_SPEED);
            double a = alpha.approachOrSet(components[3], SMOOTH_SPEED);

            double[] hsl = TerminalColors.rgbToHsl(r, g, b);
            double modifier = hovered ? (anyDown ? 0.05 : 0.10) : 0.0;
            double eased = lightness.approach(modifier, SMOOTH_SPEED);
            double l = Math.min(1.0, hsl[2] + eased);

            return TerminalColors.toArgb(TerminalColors.hslToRgb(hsl[0], hsl[1], l, a));
        }

        void reset() {
            red.reset(0.0);
            green.reset(0.0);
            blue.reset(0.0);
            alpha.reset(0.0);
            lightness.reset(0.0);
            scale.reset(1.0);
        }
    }
}
