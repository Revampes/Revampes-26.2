package xyz.whatsyouss.frosty.utility.kuudra.profit;

public enum CrimsonFaction {

    BARBARIAN("ENCHANTED_RED_SAND"),
    MAGE("ENCHANTED_MYCELIUM");

    private final String materialId;

    CrimsonFaction(String materialId) {
        this.materialId = materialId;
    }

    public String getMaterialId() {
        return materialId;
    }
}
