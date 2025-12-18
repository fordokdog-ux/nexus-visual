package dev.simplevisuals.modules.api;

public enum Category {
    Theme("I"),
    Render("H"),
    Utility("I"),
    Hud("LOL");

    private final String icon;

    Category(String icon) {
        this.icon = icon;
    }

    public String getIcon() {
        return icon;
    }
}