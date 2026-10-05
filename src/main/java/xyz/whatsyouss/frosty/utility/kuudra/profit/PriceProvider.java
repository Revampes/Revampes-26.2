package xyz.whatsyouss.frosty.utility.kuudra.profit;

import java.util.Optional;

public interface PriceProvider {

    Optional<Double> getPrice(String itemId);

    void update();

    boolean isReady();
}
