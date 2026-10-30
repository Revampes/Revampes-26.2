package xyz.revampes.Revamp.utility.kuudra.profit;

import java.util.Optional;

public interface PriceProvider {

    Optional<Double> getPrice(String itemId);

    void update();

    boolean isReady();
}
