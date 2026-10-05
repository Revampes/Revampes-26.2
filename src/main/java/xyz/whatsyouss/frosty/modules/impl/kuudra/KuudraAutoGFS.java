package xyz.whatsyouss.frosty.modules.impl.kuudra;

import jdk.jshell.execution.Util;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import xyz.whatsyouss.frosty.events.impl.EntityJoinEvent;
import xyz.whatsyouss.frosty.events.impl.PreUpdateEvent;
import xyz.whatsyouss.frosty.events.impl.ServerConnectBeginEvent;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.settings.impl.ButtonSetting;
import xyz.whatsyouss.frosty.settings.impl.SliderSetting;
import xyz.whatsyouss.frosty.utility.HotbarSwapUtils;
import xyz.whatsyouss.frosty.utility.LocationUtils;
import xyz.whatsyouss.frosty.utility.Utils;

import java.awt.*;

public class KuudraAutoGFS extends Module {

    private final ButtonSetting toxic_arrow_poison = new ButtonSetting("Toxic Arrow Poison", false);
    private final SliderSetting tap_num = new SliderSetting("Max TAP", 32, 1, 64, 1);
    private final ButtonSetting ender_pearl = new ButtonSetting("Ender Pearl", false);
    private final SliderSetting worldLoadTicks = new SliderSetting("Level Load Delay", 40.0, 20.0, 80.0, 1.0);
    private final SliderSetting getItemDelay = new SliderSetting("Get Item Delay", 40.0, 20.0, 80.0, 1.0);

    private int loadDelay = 0;
    private boolean worldLoaded = false;
    private boolean countdownStarted = false;
    private int globalDelay = 0;
    private boolean unknownAreaLastTick = true;

    public KuudraAutoGFS() {
        super("KuudraAutoGFS", category.Kuudra);
        this.registerSetting(toxic_arrow_poison);
        this.registerSetting(tap_num);
        this.registerSetting(worldLoadTicks);
        this.registerSetting(getItemDelay);
    }

    @Override
    public void onDisable() {
        this.loadDelay = 0;
        this.worldLoaded = false;
        this.countdownStarted = false;
        this.globalDelay = 0;
        this.unknownAreaLastTick = true;
    }

    @EventHandler
    private void onServerConnectBegin(ServerConnectBeginEvent event) {
        startLevelLoadCountdown();
        this.unknownAreaLastTick = true;
    }

    @EventHandler
    private void onEntityJoin(EntityJoinEvent event) {
        if (mc.player == null || event == null || event.entity == null) return;

        if (event.entity == mc.player || event.entity.getUUID().equals(mc.player.getUUID())) startLevelLoadCountdown();
    }

    @EventHandler
    public void onTick(PreUpdateEvent event) {
        if (mc.player == null || mc.level == null) {
            this.worldLoaded = false;
            return;
        }

        boolean unknowArea = "Unknown".equalsIgnoreCase(LocationUtils.getCurrentArea());
        if (unknowArea) {
            this.worldLoaded = false;
            this.unknownAreaLastTick = true;
            return;
        }

        if (this.unknownAreaLastTick && !this.countdownStarted && this.worldLoaded) startLevelLoadCountdown();

        this.unknownAreaLastTick = unknowArea;

        if (this.countdownStarted) {
            this.worldLoaded = false;
            if (this.loadDelay > 0) {
                this.loadDelay--;
                return;
            }
            this.countdownStarted = false;
            this.worldLoaded = true;
        }

        if (!this.worldLoaded || mc.getConnection() == null) return;

        if (this.globalDelay > 0) {
            this.globalDelay--;
            return;
        }

        int itemDelayTicks = Math.max(1, (int) this.getItemDelay.getInput());

        boolean sentCommand = false;
        if (this.ender_pearl.isToggled() && tryGetItem(16, "ENDER_PEARL", true)) {
            this.globalDelay = itemDelayTicks;
            sentCommand = true;
        }

        if (!sentCommand && this.toxic_arrow_poison.isToggled() && tryGetItem(16, "TOXIC_ARROW_POISON", true)) {
            this.globalDelay = itemDelayTicks;
            sentCommand = true;
        }
    }

    public static boolean tryGetItem(int maxStack, String sbId) {
        return tryGetItem(maxStack, sbId, false);
    }

    public static boolean tryGetItem(int maxStack, String sbId, boolean notExisiting) {
        if (mc.player == null || mc.getConnection() == null) {
            return false;
        }

        int slot = getItemSlot(sbId);
        if (slot == -1) {
            if (notExisiting) {
                mc.getConnection().sendCommand("gfs " + sbId + " " + maxStack);
                return true;
            }
            return false;
        }

        ItemStack stack = mc.player.getInventory().getItem(slot);
        int count = stack.getCount();
        if (count > 0 && count < maxStack) {
            int missing = maxStack - count;
            mc.getConnection().sendCommand("gfs " + sbId + " " + missing);
            return true;
        }
        return false;
    }

    private static int getItemSlot(String sbId) {
        if (mc.player == null || sbId == null || sbId.isBlank()) {
            return -1;
        }

        String targetId = normalizeSkyblockId(sbId);

        for (int slot = 0; slot < mc.player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = mc.player.getInventory().getItem(slot);
            if (stack == null || stack.isEmpty()) {
                continue;
            }

            if (matchesTargetItem(stack, targetId)) {
                return slot;
            }
        }
        return -1;
    }

    private static boolean matchesTargetItem(ItemStack stack, String targetId) {
        String skyblockId = HotbarSwapUtils.getSkyblockID(stack);
        if (skyblockId != null && normalizeSkyblockId(skyblockId).equals(targetId)) {
            return true;
        }

        String parseId = Utils.getCustomDataIId(stack.toString());
        if (parseId != null && !parseId.isBlank() && normalizeSkyblockId(parseId).equals(targetId)) {
            return true;
        }

        return "ENDER_PEARL".equals(targetId) && stack.getItem() == Items.ENDER_PEARL;
    }

    private static String normalizeSkyblockId(String id) {
        if (id == null) {
            return "";
        }

        return id.trim().toUpperCase().replace(' ', '_');
    }

    private void startLevelLoadCountdown() {
        this.countdownStarted = true;
        this.worldLoaded = false;
        this.loadDelay = Math.max(0, (int) this.worldLoadTicks.getInput());
    }

    public ButtonSetting getEnderPearl() { return this.ender_pearl; }

    public ButtonSetting getTAP() { return this.toxic_arrow_poison; }

    public SliderSetting getLevelLoadTicks() {
        return this.worldLoadTicks;
    }

    public SliderSetting getGetItemDelay() {
        return this.getItemDelay;
    }

    public int getLoadDelay() {
        return this.loadDelay;
    }

    public boolean isLevelLoaded() {
        return this.worldLoaded;
    }

    public boolean isCountdownStarted() {
        return this.countdownStarted;
    }

    public int getGlobalDelay() {
        return this.globalDelay;
    }

}
