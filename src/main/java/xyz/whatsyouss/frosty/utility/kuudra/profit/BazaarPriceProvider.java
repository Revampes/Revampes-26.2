package xyz.whatsyouss.frosty.utility.kuudra.profit;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import xyz.whatsyouss.frosty.Frosty;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class BazaarPriceProvider implements PriceProvider {

    private static final String BAZAAR_ENDPOINT = "https://api.hypixel.net/v2/skyblock/bazaar";

    private final HttpClient httpClient;

    private final Map<String, Double> instaSellPrices = new ConcurrentHashMap<>();
    private final Map<String, Double> sellOrderPrices = new ConcurrentHashMap<>();

    private final AtomicBoolean ready = new AtomicBoolean(false);

    public BazaarPriceProvider(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public Optional<Double> getPrice(String itemId) {
        if (ProfitTrackerConfig.bazaarPricingMode == ProfitTrackerConfig.BazaarPricingMode.SELL_ORDER) {
            return Optional.ofNullable(sellOrderPrices.get(itemId));
        }

        return Optional.ofNullable(instaSellPrices.get(itemId));
    }

    @Override
    public void update() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BAZAAR_ENDPOINT))
                    .timeout(Duration.ofSeconds(8))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IOException("Bazaar API returned status " + response.statusCode());
            }

            JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
            JsonObject products = root.getAsJsonObject("products");
            if (products == null) return;

            for (Map.Entry<String, JsonElement> entry : products.entrySet()) {
                JsonObject product = entry.getValue().getAsJsonObject();
                readSummary(entry.getKey(), product.getAsJsonArray("sell_summary"), instaSellPrices);
                readSummary(entry.getKey(), product.getAsJsonArray("buy_summary"), sellOrderPrices);
            }

            ready.set(true);
        } catch (Exception e) {
            Frosty.LOGGER.warn("Failed refreshing bazaar prices", e);
        }
    }

    @Override
    public boolean isReady() {
        return ready.get();
    }

    private void readSummary(String itemId, JsonArray summary, Map<String, Double> target) {
        if (summary == null || summary.isEmpty()) return;

        JsonObject first = summary.get(0).getAsJsonObject();
        if (!first.has("pricePerUnit")) return;

        target.put(itemId, Math.max(0D, first.get("pricePerUnit").getAsDouble()));
    }
}
