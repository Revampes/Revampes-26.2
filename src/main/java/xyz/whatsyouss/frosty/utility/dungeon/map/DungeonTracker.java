package xyz.whatsyouss.frosty.utility.dungeon.map;

import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import xyz.whatsyouss.frosty.utility.LocationUtils;
import xyz.whatsyouss.frosty.utility.Utils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static xyz.whatsyouss.frosty.Frosty.mc;

/**
 * Ported from devonian ({@code api/dungeon/Dungeons.kt}), trimmed down to what the map needs:
 * the floor, the "run started" / "in boss room" flags, the puzzle count used for the room type
 * prediction and the player list the markers are drawn from.
 *
 * <p>Devonian reads its tab list / scoreboard from dedicated events; this mod has neither, so the
 * same regexes are applied to {@link Utils#getTablist} and {@link Utils#getScoreboardSidebarLines}
 * once per client tick instead.
 */
public final class DungeonTracker {

    private static final Pattern PLAYER_INFO = Pattern.compile(
            "^\\[\\d+] (?:\\[YOUTUBE] )?(\\w+)(?:.+?)? \\((\\w+) ?([0IVXLCDM]+)?\\)$");
    private static final Pattern FLOOR = Pattern.compile("The Catacombs \\((\\w+)\\)");
    private static final Pattern PUZZLES_COUNT = Pattern.compile("Puzzles: \\((\\d+)\\)");
    private static final Pattern BOSS_MESSAGE = Pattern.compile("^\\[BOSS] (.+?): (.+?)$");
    private static final Pattern ROOM_SECRETS = Pattern.compile("\\b(\\d+)/(\\d+) Secrets");

    /** The bosses that put the run into the boss room (so the map can hide itself). */
    private static final List<String> BOSSES = List.of(
            "Bonzo", "Scarf", "The Professor", "Thorn", "Livid", "Sadan",
            "Maxor", "Storm", "Goldor", "Necron", "Wither King");

    public static FloorType floor = FloorType.None;
    public static boolean inBoss;
    public static boolean bloodCleared;
    public static boolean started;
    public static int totalPuzzles;
    public static DungeonRoom currentRoom;

    public static final Map<String, DungeonPlayer> players = new LinkedHashMap<>();
    public static DungeonPlayer selfPlayer;

    /** Set while the map module is on; the scanner hooks stay inert otherwise. */
    public static boolean active;

    private static boolean wasInDungeon;
    private static boolean scannedSelf;
    private static DungeonRoom lastSecretRoom;
    private static Object lastLevel;
    private static int outOfDungeonTicks;
    private static final int OUT_OF_DUNGEON_GRACE = 20;

    private DungeonTracker() {
    }

    // ------------------------------------------------------------------ lifecycle

    public static void init() {
        reset();
    }

    public static void reset() {
        floor = FloorType.None;
        inBoss = false;
        bloodCleared = false;
        started = false;
        totalPuzzles = 0;
        currentRoom = null;
        lastSecretRoom = null;
        scannedSelf = false;
        DungeonScanner.reset();
        DungeonMapScanner.reset();
        resetPlayers();
    }

    private static void resetPlayers() {
        players.clear();

        selfPlayer = createSelf();
        players.put(selfPlayer.name, selfPlayer);
    }

    private static DungeonPlayer createSelf() {
        String name = mc.getGameProfile() == null ? "" : mc.getGameProfile().name();
        PlayerInfo info = mc.getConnection() == null ? null : mc.getConnection().getPlayerInfo(name);
        return new DungeonPlayer(name, info, DungeonClass.Unknown, 0, false);
    }

    /** Called once per client tick while the map module is enabled. */
    public static void tick() {
        if (mc.level == null || mc.player == null) return;

        // Every catacombs run is its own world, so a world change means the previous run is over:
        // drop the old rooms right away instead of waiting to notice the player is out.
        if (mc.level != lastLevel) {
            lastLevel = mc.level;
            wasInDungeon = false;
            outOfDungeonTicks = 0;
            reset();
        }

        FloorType parsedFloor = parseFloor();
        if (parsedFloor != FloorType.None) floor = parsedFloor;

        // The sidebar line "The Catacombs (F7)" only exists inside an actual run, but it disappears
        // while the sidebar is rebuilt, so the tab list backs it up — and it is the tab list that
        // tells the Dungeon Hub (where the map must not show) apart from the catacombs.
        boolean inDungeon = parsedFloor != FloorType.None || tabShowsCatacombs();
        if (!inDungeon) {
            if (wasInDungeon && ++outOfDungeonTicks > OUT_OF_DUNGEON_GRACE) {
                wasInDungeon = false;
                outOfDungeonTicks = 0;
                reset();
            }
            return;
        }

        outOfDungeonTicks = 0;
        if (!wasInDungeon) {
            wasInDungeon = true;
            reset();
        }

        updateTab();
        updatePlayerEntities();
        if (selfPlayer != null) selfPlayer.tick();

        DungeonScanner.tick();
    }

    private static FloorType parseFloor() {
        for (String line : Utils.getScoreboardSidebarLines()) {
            Matcher matcher = FLOOR.matcher(Utils.stripColor(line));
            if (!matcher.find()) continue;
            return FloorType.byShortName(matcher.group(1));
        }
        return FloorType.None;
    }

    /**
     * Fallback in-dungeon check for the ticks where the sidebar has not been (re)built yet.
     *
     * <p>{@link LocationUtils#isInDungeon()} cannot be used here: it matches any tab entry that
     * contains {@code "Dungeon"}, which is also true in the Dungeon Hub, and that kept a finished
     * run's map on screen.
     */
    private static boolean tabShowsCatacombs() {
        if (mc.getConnection() == null) return false;

        boolean catacombs = false;
        for (PlayerInfo info : new ArrayList<>(mc.getConnection().getOnlinePlayers())) {
            Component display = info.getTabListDisplayName();
            if (display == null) continue;

            String line = Utils.stripColor(display.getString()).toLowerCase(Locale.ROOT);
            if (line.contains("dungeon hub")) return false;
            if (line.contains("catacombs")) catacombs = true;
        }
        return catacombs;
    }

    private static void updateTab() {
        if (mc.getConnection() == null) return;

        for (PlayerInfo info : new ArrayList<>(mc.getConnection().getOnlinePlayers())) {
            Component display = info.getTabListDisplayName();
            String name = display != null ? display.getString()
                    : info.getProfile() == null ? null : info.getProfile().name();
            if (name == null) continue;

            String line = Utils.stripColor(name);

            Matcher puzzles = PUZZLES_COUNT.matcher(line);
            if (puzzles.find()) {
                totalPuzzles = Integer.parseInt(puzzles.group(1));
                continue;
            }

            Matcher matcher = PLAYER_INFO.matcher(line);
            if (!matcher.find()) continue;

            String playerName = matcher.group(1);
            String role = matcher.group(2);
            String level = matcher.group(3);

            DungeonPlayer player = players.get(playerName);
            if (player == null) {
                player = new DungeonPlayer(playerName, info, DungeonClass.Unknown, 0, false);
                players.put(playerName, player);
            }

            // A nick shows up as any name but our own; adopt the first parsed player as "self"
            // so the marker keeps its head and the leap/name logic keeps working.
            if (players.size() > 1 && !scannedSelf) {
                players.remove(selfPlayer.name);
                player.profileInfo = selfPlayer.profileInfo;
                selfPlayer = player;
            }
            scannedSelf = true;

            if ("DEAD".equals(role)) {
                player.isDead = true;
                continue;
            }

            player.isDead = false;
            player.isDisconnected = false;
            player.role = DungeonClass.from(role);

            if (level != null && !level.isEmpty()) player.classLevel = parseRoman(level);
        }
    }

    private static void updatePlayerEntities() {
        for (AbstractClientPlayer entity : mc.level.players()) {
            PlayerInfo info = mc.getConnection() == null ? null : mc.getConnection().getPlayerInfo(entity.getUUID());
            if (info == null || info.getLatency() == -1) continue;

            DungeonPlayer player = players.get(entity.getName().getString());
            if (player == null) continue;

            player.entity = entity;
            if (player.profileInfo == null) player.profileInfo = info;
        }
    }

    private static int parseRoman(String value) {
        int total = 0;
        int previous = 0;
        for (int i = value.length() - 1; i >= 0; i--) {
            int current = switch (value.charAt(i)) {
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

    // ------------------------------------------------------------------ hooks

    /** Chat hook: only the two messages that change the map's behaviour are inspected. */
    public static void onChat(String rawMessage) {
        if (rawMessage == null) return;
        String message = Utils.stripColor(rawMessage).trim();

        if (message.equals("[NPC] Mort: Here, I found this map when I first entered the dungeon.")) {
            started = true;
            return;
        }

        Matcher boss = BOSS_MESSAGE.matcher(message);
        if (!boss.find()) return;

        String name = boss.group(1);
        if (!BOSSES.contains(name)) {
            // The Watcher is the blood room, not the boss room: it gates "inBoss" for the other
            // bosses and marks the blood door as cleared instead.
            if (name.equals("The Watcher") && boss.group(2).equals("You have proven yourself. You may pass.")) {
                bloodCleared = true;
            }
            return;
        }

        inBoss = true;
        bloodCleared = true;
    }

    /**
     * Action bar hook (mixin on {@code ClientPacketListener#setActionBarText}): Hypixel prints
     * {@code x/y Secrets} while inside a room, which is the only per-room secret progress source.
     */
    public static void onActionBar(String rawMessage) {
        if (!active || rawMessage == null) return;

        DungeonRoom room = currentRoom;
        if (room == null || room.totalSecrets <= 0) return;
        if (room != lastSecretRoom) {
            lastSecretRoom = room;
            return;
        }

        Matcher matcher = ROOM_SECRETS.matcher(Utils.stripColor(rawMessage));
        if (!matcher.find()) return;

        int found = Integer.parseInt(matcher.group(1));
        int total = Integer.parseInt(matcher.group(2));
        if (found > room.totalSecrets || total != room.totalSecrets) return;

        room.secretsCompleted = found;
    }

    public static List<DungeonPlayer> playerList() {
        return new ArrayList<>(players.values());
    }

    /** Whether the last client tick found the player inside the catacombs. */
    public static boolean inDungeon() {
        return wasInDungeon;
    }

    public static Player selfEntity() {
        return mc.player;
    }
}
