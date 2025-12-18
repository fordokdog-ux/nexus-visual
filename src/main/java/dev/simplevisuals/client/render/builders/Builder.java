package dev.simplevisuals.client.render.builders;

import dev.simplevisuals.client.render.builders.impl.*;
;

public final class Builder {

    private static final RectangleBuilder RECTANGLE_BUILDER = new RectangleBuilder();
    private static final BorderBuilder BORDER_BUILDER = new BorderBuilder();
    private static final TextureBuilder TEXTURE_BUILDER = new TextureBuilder();
    private static final TextBuilder TEXT_BUILDER = new TextBuilder();
    private static final BlurBuilder BLUR_BUILDER = new BlurBuilder();
    private static final RockstarRectangleBuilder ROCKSTAR_RECTANGLE_BUILDER = new RockstarRectangleBuilder();
    private static final RockstarBlurBuilder ROCKSTAR_BLUR_BUILDER = new RockstarBlurBuilder();

    public static RectangleBuilder rectangle() {
        return RECTANGLE_BUILDER;
    }

    public static RockstarRectangleBuilder rockstarRectangle() {
        return ROCKSTAR_RECTANGLE_BUILDER;
    }

    public static RockstarBlurBuilder rockstarBlur() {
        return ROCKSTAR_BLUR_BUILDER;
    }

    public static BorderBuilder border() {
        return BORDER_BUILDER;
    }

    public static TextureBuilder texture() {
        return TEXTURE_BUILDER;
    }

    public static TextBuilder text() {
        return TEXT_BUILDER;
    }

    public static BlurBuilder blur() {
        return BLUR_BUILDER;
    }
}