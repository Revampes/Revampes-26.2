package xyz.whatsyouss.frosty.utility.kuudra.profit;

import xyz.whatsyouss.frosty.Frosty;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class ItemPriceManager {

    private static final ItemPriceManager INSTANCE = new ItemPriceManager();

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "frosty-price-cache");
        thread.setDaemon(true);
        return thread;
    });

    private final List<PriceProvider> providers;

    private ItemPriceManager() {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(6)).build();

        this.providers = List.of(
                new BazaarPriceProvider(httpClient),
                new AuctionPriceProvider(httpClient)
        );

        scheduler.scheduleAtFixedRate(() -> {
            try {
                updateAll();
            } catch (Exception e) {
                Frosty.LOGGER.warn("Failed to update prices", e);
            }
        }, 0, 5, TimeUnit.MINUTES);
    }

    public static ItemPriceManager get() {
        return INSTANCE;
    }

    public long getItemPrice(String itemId) {
        return Math.max(0L, Math.round(getPrice(itemId).orElse(0D)));
    }

    public long getKeyPrice(String keyTier) {
        return Math.max(0L, Math.round(calculateKeyPrice(ChestKeyType.parseKeyType(keyTier))));
    }

    public double calculateKeyPrice(ChestKeyType key) {
        if (key == ChestKeyType.FREE || key == ChestKeyType.UNKNOWN) {
            return 0D;
        }

        double netherStar = getPrice("NETHER_STAR").orElse(0D);
        double factionMaterial = getPrice(ProfitTrackerConfig.crimsonIsleFaction.getMaterialId()).orElse(0D);

        return key.getBaseCoinsCost() + (2 * netherStar) + (key.getMaterialAmount() * factionMaterial);
    }

    public long getKismetPrice() {
        return getItemPrice("KISMET_FEATHER");
    }

    public long getWheelOfFatePrice() {
        return getItemPrice("WHEEL_OF_FATE");
    }

    private Optional<Double> getPrice(String itemId) {
        for (PriceProvider provider : providers) {
            Optional<Double> price = provider.getPrice(itemId);
            if (price.isPresent()) return price;
        }

        return Optional.empty();
    }

    private void updateAll() {
        providers.forEach(PriceProvider::update);
    }
}
