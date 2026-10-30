package xyz.whatsyouss.frosty.modules.impl.client;

import java.awt.Desktop;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import xyz.whatsyouss.frosty.Frosty;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.settings.impl.ButtonSetting;
import xyz.whatsyouss.frosty.utility.LocationUtils;
import xyz.whatsyouss.frosty.utility.Utils;

public class DiscordPresence extends Module {
    private static final String APPLICATION_ID = "1564567419337449552";
    private static final String REPOSITORY_URL = "https://github.com/Revampes/Revampes-26.2";
    private static final String DOWNLOAD_URL = REPOSITORY_URL + "/releases/latest";

    public final ButtonSetting repository;
    public final ButtonSetting download;
    private final DiscordIpcClient discord = new DiscordIpcClient();
    private long sessionStartedAt;
    private long lastSnapshotAt;
    private volatile ActivitySnapshot snapshot = new ActivitySnapshot("Idle", "Minecraft");

    public DiscordPresence() {
        super("Discord Presence", "Discord状态", category.Client);
        repository = new ButtonSetting("Open repository", "打开仓库", () -> openLink(REPOSITORY_URL));
        download = new ButtonSetting("Open downloads", "打开下载页", () -> openLink(DOWNLOAD_URL));
        registerSetting(repository);
        registerSetting(download);
        defaultEnabled = true;
    }

    @Override
    public String getDesc() {
        return "Shows your safe, current SkyBlock activity in Discord Rich Presence.";
    }

    @Override
    public void onEnable() {
        sessionStartedAt = System.currentTimeMillis();
        discord.start(APPLICATION_ID);
    }

    @Override
    public void onDisable() {
        discord.stop();
    }

    @Override
    public void onUpdate() {
        discord.ensureStarted(APPLICATION_ID, sessionStartedAt);
        long now = System.currentTimeMillis();
        if (now - lastSnapshotAt < 2000) return;
        lastSnapshotAt = now;

        String area = LocationUtils.getCurrentArea();
        String activity = findActivity(area, Utils.getScoreboardSidebarLines());
        snapshot = new ActivitySnapshot(activity, area == null || area.isBlank() ? "Minecraft" : area);
        discord.update(snapshot, sessionStartedAt);
    }

    private static String findActivity(String area, List<String> scoreboard) {
        String combined = String.join(" ", scoreboard).replaceAll("§.", "");
        String lower = combined.toLowerCase(Locale.ROOT);
        if (lower.contains("kuudra")) {
            for (String tier : new String[]{"Infernal", "Burning", "Fiery", "Hot", "Basic"}) {
                if (lower.contains(tier.toLowerCase(Locale.ROOT))) return "Kuudra: " + tier;
            }
            return "Kuudra";
        }
        for (String floor : new String[]{"VII", "VI", "V", "IV", "III", "II", "I"}) {
            if (combined.matches(".*(?i)(floor|f\\s*)" + floor + ".*")) return "Dungeon Floor " + floor;
        }
        return area == null || area.isBlank() || "Unknown".equalsIgnoreCase(area) ? "Idle" : "Exploring";
    }

    private static void openLink(String link) {
        try {
            if (Desktop.isDesktopSupported()) Desktop.getDesktop().browse(URI.create(link));
        } catch (Exception exception) {
            Frosty.LOGGER.warn("Unable to open Discord Presence link: {}", link, exception);
        }
    }

    private record ActivitySnapshot(String activity, String area) {}

    private static final class DiscordIpcClient {
        private ScheduledExecutorService executor;
        private String clientId;
        private DiscordIpcConnection connection;
        private ActivitySnapshot latest = new ActivitySnapshot("Idle", "Minecraft");
        private long startedAt;

        synchronized void start(String clientId) {
            stop();
            this.clientId = clientId.trim();
            this.executor = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread thread = new Thread(r, "frosty-discord-presence");
                thread.setDaemon(true);
                return thread;
            });
            this.executor.scheduleWithFixedDelay(this::sendLatest, 0, 2, TimeUnit.SECONDS);
        }

        synchronized void ensureStarted(String clientId, long startedAt) {
            if (executor == null || !clientId.equals(this.clientId)) {
                start(clientId);
            }
            this.startedAt = startedAt;
        }

        synchronized void update(ActivitySnapshot snapshot, long startedAt) {
            latest = snapshot;
            this.startedAt = startedAt;
        }

        private synchronized void sendLatest() {
            if (clientId == null || clientId.isBlank()) return;
            try {
                if (connection == null && !connect()) return;
                connection.setActivity(latest, startedAt);
            } catch (Exception exception) {
                closeConnection();
                Frosty.LOGGER.debug("Discord IPC update failed; will retry.", exception);
            }
        }

        private boolean connect() {
            for (int pipe = 0; pipe < 10; pipe++) {
                try {
                    connection = DiscordIpcConnection.open(pipe);
                    connection.handshake(clientId);
                    return true;
                } catch (Exception ignored) {
                    closeConnection();
                }
            }
            return false;
        }

        synchronized void stop() {
            if (executor != null) {
                executor.shutdownNow();
                executor = null;
            }
            closeConnection();
            clientId = null;
        }

        private void closeConnection() {
            if (connection != null) {
                connection.close();
                connection = null;
            }
        }
    }

    private static final class DiscordIpcConnection {
        private final java.io.RandomAccessFile pipe;

        private DiscordIpcConnection(java.io.RandomAccessFile pipe) {
            this.pipe = pipe;
        }

        static DiscordIpcConnection open(int pipeIndex) throws java.io.IOException {
            String path = "\\\\.\\pipe\\discord-ipc-" + pipeIndex;
            return new DiscordIpcConnection(new java.io.RandomAccessFile(path, "rw"));
        }

        void handshake(String clientId) throws java.io.IOException {
            JsonObject payload = new JsonObject();
            payload.addProperty("v", 1);
            payload.addProperty("client_id", clientId);
            send(0, payload);
            readResponse();
        }

        void setActivity(ActivitySnapshot snapshot, long startedAt) throws java.io.IOException {
            JsonObject activity = new JsonObject();
            activity.addProperty("details", snapshot.activity());
            activity.addProperty("state", snapshot.area());
            JsonObject timestamps = new JsonObject();
            timestamps.addProperty("start", startedAt / 1000);
            activity.add("timestamps", timestamps);
            JsonArray buttons = new JsonArray();
            addButton(buttons, "Download Revampes", DOWNLOAD_URL);
            addButton(buttons, "GitHub repository", REPOSITORY_URL);
            activity.add("buttons", buttons);

            JsonObject args = new JsonObject();
            args.addProperty("pid", ProcessHandle.current().pid());
            args.add("activity", activity);
            JsonObject payload = new JsonObject();
            payload.addProperty("cmd", "SET_ACTIVITY");
            payload.addProperty("nonce", Long.toHexString(System.nanoTime()));
            payload.add("args", args);
            send(1, payload);
            readResponse();
        }

        private static void addButton(JsonArray buttons, String label, String url) {
            JsonObject button = new JsonObject();
            button.addProperty("label", label);
            button.addProperty("url", url);
            buttons.add(button);
        }

        private synchronized void send(int opcode, JsonObject payload) throws java.io.IOException {
            byte[] data = payload.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
            java.nio.ByteBuffer header = java.nio.ByteBuffer.allocate(8).order(java.nio.ByteOrder.LITTLE_ENDIAN);
            header.putInt(opcode).putInt(data.length);
            pipe.write(header.array());
            pipe.write(data);
        }

        private void readResponse() throws java.io.IOException {
            byte[] header = new byte[8];
            try {
                pipe.readFully(header);
            } catch (java.io.EOFException exception) {
                throw new java.io.EOFException("Discord IPC closed");
            }
            java.nio.ByteBuffer buffer = java.nio.ByteBuffer.wrap(header).order(java.nio.ByteOrder.LITTLE_ENDIAN);
            int length = buffer.getInt(4);
            if (length < 0 || length > 1_000_000) throw new java.io.IOException("Invalid Discord IPC response");
            byte[] response = new byte[length];
            pipe.readFully(response);
        }

        void close() {
            try {
                pipe.close();
            } catch (java.io.IOException ignored) {
            }
        }
    }
}
