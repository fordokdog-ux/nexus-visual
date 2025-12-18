package dev.simplevisuals.modules.impl.utility;

import dev.simplevisuals.NexusVisual;
import dev.simplevisuals.client.events.impl.EventRender2D;
import dev.simplevisuals.modules.api.Category;
import dev.simplevisuals.modules.api.Module;
import dev.simplevisuals.client.util.renderer.Render2D;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import net.minecraft.text.Text;
import net.minecraft.client.resource.language.I18n;

import java.awt.*;

public class TotemCounter extends Module {

    private static final float ICON_SIZE = 16.0f; // Size of totem icon in pixels
    private static final float TEXT_SCALE = 1.0f; // Scale for the totem count text
    private static final Identifier TOTEM_TEXTURE = Identifier.of("minecraft", "textures/item/totem_of_undying.png");

    public TotemCounter() {
        super("TotemCounter", Category.Utility, I18n.translate("module.totemcounter.description"));
    }

    @EventHandler
    public void onRender2D(EventRender2D e) {
        if (fullNullCheck()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        // Count totems in player's inventory
        int totemCount = countTotems();

        // Get screen coordinates for crosshair
        float centerX = mc.getWindow().getScaledWidth() / 2.0f;
        float centerY = mc.getWindow().getScaledHeight() / 2.0f;

        // Position the icon and text slightly to the right of the crosshair
        float iconX = centerX + 10.0f; // 10 pixels to the right of crosshair
        float iconY = centerY - (ICON_SIZE / 2.0f); // Centered vertically relative to crosshair
        float textX = iconX + ICON_SIZE + 4.0f; // 4 pixels to the right of icon
        float textY = iconY + (ICON_SIZE / 2.0f) - (mc.textRenderer.fontHeight * TEXT_SCALE / 2.0f); // Center text vertically

        // Draw totem icon
        e.getContext().getMatrices().push();
        e.getContext().getMatrices().translate(iconX, iconY, 0);
        Render2D.drawTexture(
                e.getContext().getMatrices(),
                0, 0, ICON_SIZE, ICON_SIZE,
                0f,
                TOTEM_TEXTURE,
                new Color(255, 255, 255, 255) // White color, fully opaque
        );
        e.getContext().getMatrices().pop();

        // Draw totem count text using DrawContext
        String countText = String.valueOf(totemCount);
        e.getContext().getMatrices().push();
        e.getContext().getMatrices().translate(textX, textY, 0);
        e.getContext().getMatrices().scale(TEXT_SCALE, TEXT_SCALE, 1.0f);
        e.getContext().drawText(
                mc.textRenderer,
                Text.of(countText),
                0,
                0,
                new Color(255, 255, 255, 255).getRGB(), // White text, fully opaque
                true // Enable shadow for readability
        );
        e.getContext().getMatrices().pop();
    }

    private int countTotems() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return 0;

        int count = 0;
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.TOTEM_OF_UNDYING) {
                count += mc.player.getInventory().getStack(i).getCount();
            }
        }
        return count;
    }
}