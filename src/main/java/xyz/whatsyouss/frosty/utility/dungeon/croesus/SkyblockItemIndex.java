package xyz.whatsyouss.frosty.utility.dungeon.croesus;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import xyz.whatsyouss.frosty.Frosty;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class SkyblockItemIndex {

    private static final String ENDPOINT = "https://api.hypixel.net/v2/resources/skyblock/items";
    private static final long REFRESH_MS = 1_800_000L;

    private static final Map<String, String> ID_BY_NAME = new ConcurrentHashMap<>();

    private static volatile boolean loading;
    private static volatile long fetchedAt;

    private SkyblockItemIndex() {
    }

    public static void ensureLoaded() {
        if (loading) return;
        if (!ID_BY_NAME.isEmpty() && System.currentTimeMillis() - fetchedAt < REFRESH_MS) return;

        loading = true;
        Thread thread = new Thread(SkyblockItemIndex::fetch, "frosty-skyblock-items");
        thread.setDaemon(true);
        thread.start();
    }

    public static boolean isLoaded() {
        return !ID_BY_NAME.isEmpty();
    }

    public static String findId(String displayName) {
        if (displayName == null) return null;
        return ID_BY_NAME.get(displayName);
    }

    public static boolean isKnown(String itemId) {
        return itemId != null && ID_BY_NAME.containsValue(itemId);
    }

    private static void fetch() {
        try {
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(6)).build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ENDPOINT))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("items API returned status " + response.statusCode());
            }

            JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
            JsonArray items = root.getAsJsonArray("items");
            if (items == null) return;

            Map<String, String> resolved = new ConcurrentHashMap<>();
            for (JsonElement element : items) {
                JsonObject item = element.getAsJsonObject();
                if (!item.has("id") || !item.has("name")) continue;

                String id = item.get("id").getAsString();
                if (id.startsWith("STARRED_")) continue;
                resolved.put(item.get("name").getAsString(), id);
            }

            ID_BY_NAME.clear();
            ID_BY_NAME.putAll(resolved);
            fetchedAt = System.currentTimeMillis();
        } catch (Exception e) {
            Frosty.LOGGER.warn("Failed to load skyblock item names", e);
        } finally {
            loading = false;
        }
    }
}
