package dev.simplevisuals.client.util.renderer.fonts;

import dev.simplevisuals.client.render.msdf.MsdfFont;
import lombok.experimental.UtilityClass;

@UtilityClass
public class Fonts {
    public static Font BOLD;
    public static Font MEDIUM;
    public static Font REGULAR;
    public static Font SEMIBOLD;
    public static Font ICONS;
    public static Font GEISTMONO;

    static {
        BOLD = new Font(MsdfFont.builder().atlas("sf_bold").data("sf_bold").build());
        MEDIUM = new Font(MsdfFont.builder().atlas("sf_medium").data("sf_medium").build());
        REGULAR = new Font(MsdfFont.builder().atlas("sf_regular").data("sf_regular").build());
        SEMIBOLD = new Font(MsdfFont.builder().atlas("sf_semibold").data("sf_semibold").build());
        ICONS = new Font(MsdfFont.builder().atlas("icons").data("icons").build());
        GEISTMONO = new Font(MsdfFont.builder().atlas("geistmono-black").data("geistmono-black").build());
    }
}