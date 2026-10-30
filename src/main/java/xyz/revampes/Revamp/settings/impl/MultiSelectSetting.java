package xyz.revampes.Revamp.settings.impl;

import xyz.revampes.Revamp.Frosty;
import xyz.revampes.Revamp.events.impl.SettingUpdateEvent;
import xyz.revampes.Revamp.settings.Setting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MultiSelectSetting extends Setting {

    private final String[] options;
    private final boolean[] selected;

    public MultiSelectSetting(String name, String[] options) {
        super(name);
        this.options = options;
        this.selected = new boolean[options.length];
    }

    public MultiSelectSetting(String name, String[] options, Collection<String> defaults) {
        this(name, options);
        setSelectedNames(defaults);
    }

    public String[] getOptions() {
        return options;
    }

    public boolean isSelected(int index) {
        return index >= 0 && index < selected.length && selected[index];
    }

    public boolean isSelected(String option) {
        int index = indexOf(option);
        return index >= 0 && selected[index];
    }

    public void toggle(int index) {
        set(index, !isSelected(index));
    }

    public void set(int index, boolean value) {
        if (index < 0 || index >= selected.length) return;
        if (selected[index] == value) return;

        selected[index] = value;
        Frosty.EVENT_BUS.post(new SettingUpdateEvent());
    }

    public List<String> getSelectedNames() {
        List<String> names = new ArrayList<>();
        for (int i = 0; i < options.length; i++) {
            if (selected[i]) names.add(options[i]);
        }
        return names;
    }

    public void setSelectedNames(Collection<String> names) {
        for (int i = 0; i < options.length; i++) {
            selected[i] = names != null && names.contains(options[i]);
        }
    }

    public String getSummary() {
        List<String> names = getSelectedNames();
        return names.isEmpty() ? "None" : String.join(", ", names);
    }

    private int indexOf(String option) {
        for (int i = 0; i < options.length; i++) {
            if (options[i].equals(option)) return i;
        }
        return -1;
    }
}
