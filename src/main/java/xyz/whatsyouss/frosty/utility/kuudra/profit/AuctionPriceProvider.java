package xyz.whatsyouss.frosty.utility.kuudra.profit;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import xyz.whatsyouss.frosty.Frosty;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Lowest BIN prices, resolved on demand and cached per item.
 *
 * <p>IQAddons first used {@code moulberry.codes/lowestbin.json} and later a hosted
 * service endpoint; both are unavailable now (the former no longer serves HTTPS, the
 * latter answers 401), so this asks Coflnet for the single item it needs. Only items the
 * bazaar has no price for ever reach it, and each item is requested at most once per
 * session (plus a cooldown after a failure).
 */
public class AuctionPriceProvider implements PriceProvider {

    private static final String LOWEST_BIN_ENDPOINT = "https://sky.coflnet.com/api/item/price/%s/bin";
    private static final long RETRY_COOLDOWN_MS = 60_000L;

    private final HttpClient httpClient;

    private final Map<String, Double> prices = new ConcurrentHashMap<>();
    private final Map<String, Long> failedAt = new ConcurrentHashMap<>();
    private final Set<String> resolving = ConcurrentHashMap.newKeySet();

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "frosty-lowest-bin");
        thread.setDaemon(true);
        return thread;
    });

    public AuctionPriceProvider(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public Optional<Double> getPrice(String itemId) {
        if (itemId == null || itemId.isBlank()) return Optional.empty();

        Double cached = prices.get(itemId);
        if (cached != null) return Optional.of(cached);

        Long lastFailure = failedAt.get(itemId);
        if (lastFailure != null && System.currentTimeMillis() - lastFailure < RETRY_COOLDOWN_MS) {
            return Optional.empty();
        }

        request(itemId);
        return Optional.empty();
    }

    @Override
    public void update() {
    }

    @Override
    public boolean isReady() {
        return true;
    }

    private void request(String itemId) {
        if (!resolving.add(itemId)) return;

        executor.execute(() -> {
            try {
                fetch(itemId);
            } finally {
                resolving.remove(itemId);
            }
        });
    }

    private void fetch(String itemId) {
        try {
            String encodedId = URLEncoder.encode(itemId, StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(String.format(LOWEST_BIN_ENDPOINT, encodedId)))
                    .timeout(Duration.ofSeconds(8))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 400 || response.statusCode() == 404) {
                prices.put(itemId, 0D);
                return;
            }

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                failedAt.put(itemId, System.currentTimeMillis());
                return;
            }

            JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
            JsonElement lowest = root.get("lowest");
            prices.put(itemId, lowest == null ? 0D : Math.max(0D, lowest.getAsDouble()));
        } catch (Exception e) {
            failedAt.put(itemId, System.currentTimeMillis());
            Frosty.LOGGER.debug("Failed resolving lowest BIN for {}", itemId, e);
        }
    }
}
