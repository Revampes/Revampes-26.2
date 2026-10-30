package xyz.revampes.Revamp.settings;


import java.util.function.BooleanSupplier;

public abstract class Setting {

    public String name;
    public boolean visible = true;
    private BooleanSupplier visibilityCondition;

    public Setting(String name) {
        this.name = name;
    }

    public String group = "";

    public Setting group(String group) {
        this.group = group;
        return this;
    }

    public String getGroup() {
        return group;
    }


    public void setVisibilityCondition(BooleanSupplier condition) {
        this.visibilityCondition = condition;
    }

    public boolean isVisible() {
        if (visibilityCondition != null) {
            return visibilityCondition.getAsBoolean();
        }
        return visible;
    }

    public String getName() {
        return this.name;
    }
}
