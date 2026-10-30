package xyz.revampes.Revamp.utility.dungeon.croesus;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.Minecraft;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class CroesusData {

    private static final Path DIR = Minecraft.getInstance().gameDirectory.toPath().resolve("config/Frosty/croesus");
    private static final Path WORTHLESS_FILE = DIR.resolve("worthless.json");
    private static final Path ALWAYS_BUY_FILE = DIR.resolve("always_buy.json");
    private static final Path RUN_LOG_FILE = DIR.resolve("run_log.json");

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type STRING_LIST = new TypeToken<List<String>>() {
    }.getType();
    private static final Type RUN_LIST = new TypeToken<List<CroesusChest.Info>>() {
    }.getType();

    private static final List<String> DEFAULT_WORTHLESS = List.of(
            "DUNGEON_DISC_5",
            "DUNGEON_DISC_4",
            "DUNGEON_DISC_3",
            "DUNGEON_DISC_2",
            "DUNGEON_DISC_1",
            "MAXOR_THE_FISH",
            "STORM_THE_FISH",
            "GOLDOR_THE_FISH",
            "ENCHANTMENT_ULTIMATE_NO_PAIN_NO_GAIN_1",
            "ENCHANTMENT_ULTIMATE_NO_PAIN_NO_GAIN_2",
            "ENCHANTMENT_ULTIMATE_NO_PAIN_NO_GAIN_3",
            "ENCHANTMENT_ULTIMATE_NO_PAIN_NO_GAIN_4",
            "ENCHANTMENT_ULTIMATE_NO_PAIN_NO_GAIN_5",
            "ENCHANTMENT_ULTIMATE_COMBO_1",
            "ENCHANTMENT_ULTIMATE_COMBO_2",
            "ENCHANTMENT_ULTIMATE_COMBO_3",
            "ENCHANTMENT_ULTIMATE_COMBO_4",
            "ENCHANTMENT_ULTIMATE_COMBO_5",
            "ENCHANTMENT_ULTIMATE_BANK_1",
            "ENCHANTMENT_ULTIMATE_BANK_2",
            "ENCHANTMENT_ULTIMATE_BANK_3",
            "ENCHANTMENT_ULTIMATE_BANK_4",
            "ENCHANTMENT_ULTIMATE_BANK_5",
            "ENCHANTMENT_ULTIMATE_JERRY_1",
            "ENCHANTMENT_ULTIMATE_JERRY_2",
            "ENCHANTMENT_ULTIMATE_JERRY_3",
            "ENCHANTMENT_ULTIMATE_JERRY_4",
            "ENCHANTMENT_ULTIMATE_JERRY_5",
            "ENCHANTMENT_FEATHER_FALLING_6",
            "ENCHANTMENT_FEATHER_FALLING_7",
            "ENCHANTMENT_FEATHER_FALLING_8",
            "ENCHANTMENT_FEATHER_FALLING_9",
            "ENCHANTMENT_FEATHER_FALLING_10",
            "ENCHANTMENT_INFINITE_QUIVER_6",
            "ENCHANTMENT_INFINITE_QUIVER_7",
            "ENCHANTMENT_INFINITE_QUIVER_8",
            "ENCHANTMENT_INFINITE_QUIVER_9",
            "ENCHANTMENT_INFINITE_QUIVER_10",
            "SPIRIT_SHORTBOW",
            "SPIRIT_BOW",
            "ITEM_SPIRIT_BOW",
            "WITHER_BOOTS",
            "WITHER_CHESTPLATE",
            "WITHER_LEGGINGS",
            "WITHER_HELMET",
            "WITHER_CLOAK",
            "AUTO_RECOMBOBULATOR",
            "MASTER_SKULL_TIER_5",
            "MASTER_SKULL_TIER_4",
            "SHADOW_ASSASSIN_BOOTS",
            "SHADOW_ASSASSIN_LEGGINGS",
            "SHADOW_ASSASSIN_CHESTPLATE",
            "SHADOW_ASSASSIN_HELMET",
            "WARPED_STONE"
    );

    private static final List<String> DEFAULT_ALWAYS_BUY = List.of(
            "NECRON_HANDLE",
            "DARK_CLAYMORE",
            "FIRST_MASTER_STAR",
            "SECOND_MASTER_STAR",
            "THIRD_MASTER_STAR",
            "FOURTH_MASTER_STAR",
            "FIFTH_MASTER_STAR",
            "SHADOW_FURY",
            "SHADOW_WARP_SCROLL",
            "IMPLOSION_SCROLL",
            "WITHER_SHIELD_SCROLL",
            "DYE_LIVID"
    );

    private static List<String> worthless = new ArrayList<>(DEFAULT_WORTHLESS);
    private static List<String> alwaysBuy = new ArrayList<>(DEFAULT_ALWAYS_BUY);
    private static final List<CroesusChest.Info> runLog = new ArrayList<>();
    private static boolean loaded;

    private CroesusData() {
    }

    public static void load() {
        if (loaded) return;
        loaded = true;

        List<String> loadedWorthless = readList(WORTHLESS_FILE);
        if (loadedWorthless != null) worthless = loadedWorthless;

        List<String> loadedAlwaysBuy = readList(ALWAYS_BUY_FILE);
        if (loadedAlwaysBuy != null) alwaysBuy = loadedAlwaysBuy;

        List<CroesusChest.Info> loadedRuns = readRuns();
        if (loadedRuns != null) {
            runLog.clear();
            runLog.addAll(loadedRuns);
        }
    }

    public static List<String> getWorthless() {
        return worthless;
    }

    public static List<String> getAlwaysBuy() {
        return alwaysBuy;
    }

    public static List<CroesusChest.Info> getRunLog() {
        return runLog;
    }

    public static void saveWorthless() {
        write(WORTHLESS_FILE, worthless);
    }

    public static void saveAlwaysBuy() {
        write(ALWAYS_BUY_FILE, alwaysBuy);
    }

    public static boolean addRunLog(CroesusChest.Info info) {
        if (info.items.isEmpty() || info.value == 0.0 || runLog.contains(info)) return false;

        runLog.add(info);
        write(RUN_LOG_FILE, runLog);
        return true;
    }

    private static List<String> readList(Path file) {
        if (!Files.exists(file)) {
            write(file, file.equals(WORTHLESS_FILE) ? worthless : alwaysBuy);
            return null;
        }

        try (Reader reader = Files.newBufferedReader(file)) {
            List<String> data = GSON.fromJson(reader, STRING_LIST);
            return data == null ? null : new ArrayList<>(data);
        } catch (Exception e) {
            System.err.println("Frosty: failed to read " + file.getFileName() + ": " + e.getMessage());
            return null;
        }
    }

    private static List<CroesusChest.Info> readRuns() {
        if (!Files.exists(RUN_LOG_FILE)) return null;

        try (Reader reader = Files.newBufferedReader(RUN_LOG_FILE)) {
            return GSON.fromJson(reader, RUN_LIST);
        } catch (Exception e) {
            System.err.println("Frosty: failed to read run_log.json: " + e.getMessage());
            return null;
        }
    }

    private static void write(Path file, Object data) {
        try {
            Files.createDirectories(DIR);
            try (Writer writer = Files.newBufferedWriter(file)) {
                GSON.toJson(data, writer);
            }
        } catch (Exception e) {
            System.err.println("Frosty: failed to write " + file.getFileName() + ": " + e.getMessage());
        }
    }
}
