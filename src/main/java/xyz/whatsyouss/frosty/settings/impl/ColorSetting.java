package xyz.whatsyouss.frosty.settings.impl;

import xyz.whatsyouss.frosty.settings.Setting;

import java.awt.Color;

public class ColorSetting extends Setting {
    private Color color;
    private boolean expanded = false;

    public ColorSetting(String name, Color defaultColor) {
        super(name);
        this.color = defaultColor;
    }

    public ColorSetting(String name, int r, int g, int b, int a) {
        super(name);
        this.color = new Color(r, g, b, a);
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public int getRGB() {
        return color.getRGB();
    }

    public int getRed() { return color.getRed(); }
    public int getGreen() { return color.getGreen(); }
    public int getBlue() { return color.getBlue(); }
    public int getAlpha() { return color.getAlpha(); }

    public void setRed(int r) { color = new Color(r, color.getGreen(), color.getBlue(), color.getAlpha()); }
    public void setGreen(int g) { color = new Color(color.getRed(), g, color.getBlue(), color.getAlpha()); }
    public void setBlue(int b) { color = new Color(color.getRed(), color.getGreen(), b, color.getAlpha()); }
    public void setAlpha(int a) { color = new Color(color.getRed(), color.getGreen(), color.getBlue(), a); }

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }
}
