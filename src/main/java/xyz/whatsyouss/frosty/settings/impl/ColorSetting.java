package xyz.whatsyouss.frosty.settings.impl;

import xyz.whatsyouss.frosty.Frosty;
import xyz.whatsyouss.frosty.events.impl.SettingUpdateEvent;
import xyz.whatsyouss.frosty.modules.impl.client.UI;
import xyz.whatsyouss.frosty.settings.Setting;

import java.awt.Color;

public class ColorSetting extends Setting {
    private Color color;
    private boolean expanded = false;
    private String cnName;

    public ColorSetting(String name, Color defaultColor) {
        super(name);
        this.color = defaultColor;
    }

    public ColorSetting(String name, String cnName, Color defaultColor) {
        super(name);
        this.cnName = cnName;
        this.color = defaultColor;
    }

    public ColorSetting(String name, int r, int g, int b, int a) {
        super(name);
        this.color = new Color(r, g, b, a);
    }

    public String getTransName() {
        if (this.cnName != null && !this.cnName.isEmpty() && UI.lang.getValue() == 1) {
            return this.cnName;
        }
        return this.name;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        if (color != null && color.equals(this.color)) return;

        this.color = color;
        Frosty.EVENT_BUS.post(new SettingUpdateEvent());
    }

    public int getRGB() {
        return color.getRGB();
    }

    public int getRed() { return color.getRed(); }
    public int getGreen() { return color.getGreen(); }
    public int getBlue() { return color.getBlue(); }
    public int getAlpha() { return color.getAlpha(); }

    public void setRed(int r) { setColor(new Color(clamp(r), color.getGreen(), color.getBlue(), color.getAlpha())); }
    public void setGreen(int g) { setColor(new Color(color.getRed(), clamp(g), color.getBlue(), color.getAlpha())); }
    public void setBlue(int b) { setColor(new Color(color.getRed(), color.getGreen(), clamp(b), color.getAlpha())); }
    public void setAlpha(int a) { setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), clamp(a))); }

    public String getHex() {
        return String.format("%02X%02X%02X%02X", getRed(), getGreen(), getBlue(), getAlpha());
    }

    public void setHex(String hex) {
        if (hex == null) return;

        String value = (hex.startsWith("#") ? hex.substring(1) : hex).trim();
        if (value.length() == 6) value = value + "FF";
        if (value.length() != 8) return;

        try {
            long rgba = Long.parseLong(value, 16);
            setColor(new Color(
                    (int) ((rgba >> 24) & 0xFF),
                    (int) ((rgba >> 16) & 0xFF),
                    (int) ((rgba >> 8) & 0xFF),
                    (int) (rgba & 0xFF)
            ));
        } catch (NumberFormatException ignored) {
        }
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
