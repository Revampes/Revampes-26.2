package xyz.revampes.Revamp.utility.dungeon.croesus;

import java.util.ArrayList;
import java.util.List;

public final class CroesusChest {

    public enum Type {
        BEDROCK,
        OBSIDIAN,
        DIAMOND,
        GOLD,
        WOOD,
        NONE
    }

    public static final class Item {

        private final String id;
        private final int quantity;

        public Item(String id, int quantity) {
            this.id = id;
            this.quantity = quantity;
        }

        public String getId() {
            return id;
        }

        public int getQuantity() {
            return quantity;
        }
    }

    public static final class Info {

        public Type type = Type.NONE;
        public double cost;
        public transient double value;
        public transient double profit;
        public final List<Item> items = new ArrayList<>();
    }

    private CroesusChest() {
    }
}
