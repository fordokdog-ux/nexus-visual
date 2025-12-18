package dev.simplevisuals.client;

import java.awt.Color;

import dev.simplevisuals.client.util.Wrapper;
import dev.simplevisuals.client.util.ColorUtils;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

public final class ChatUtils implements Wrapper {

        private static final String BRAND = "Nexus Visual";

        private ChatUtils() {}

        public static void sendMessage(String message) {
        if (mc == null || mc.player == null) return;
        MutableText text = Text.literal("");
        for (int i = 0; i < BRAND.length(); i++) {
            text.append(Text.literal(BRAND.charAt(i) + "")
                    .setStyle(Style.EMPTY
                            .withBold(true)
                            .withColor(TextColor.fromRgb(ColorUtils.gradient(ColorUtils.getGlobalColor(), Color.WHITE, (float) i / BRAND.length()).getRGB()))
                    )
            );
        }

        text.append(Text.literal(" ⇨ ")
                .setStyle(Style.EMPTY
                        .withBold(false)
                        .withColor(TextColor.fromRgb(new Color(200, 200, 200).getRGB()))
                )
        );

        text.append(Text.literal(message)
                .setStyle(Style.EMPTY
                        .withBold(false)
                        .withColor(TextColor.fromRgb(new Color(200, 200, 200).getRGB()))
                )
        );

        mc.player.sendMessage(text, false);
    }
}