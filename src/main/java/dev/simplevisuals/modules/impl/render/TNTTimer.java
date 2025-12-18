package dev.simplevisuals.modules.impl.render;

import dev.simplevisuals.client.events.impl.EventRender2D;
import dev.simplevisuals.client.util.renderer.Render2D;
import dev.simplevisuals.client.util.world.WorldUtils;
import dev.simplevisuals.modules.api.Category;
import dev.simplevisuals.modules.api.Module;
import dev.simplevisuals.modules.settings.impl.ColorSetting;
import dev.simplevisuals.modules.settings.impl.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.TntEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

import java.awt.Color;
import java.util.Locale;

public class TNTTimer extends Module {
    
    private final ColorSetting textColor = new ColorSetting("Цвет текста", new Color(255, 85, 85).getRGB());
    private final NumberSetting scale = new NumberSetting("Размер", 1.5f, 0.5f, 3.0f, 0.1f);
    
    public TNTTimer() {
        super("TNTTimer", Category.Render, "Показывает время до взрыва TNT");
    }
    
    @EventHandler
    private void onRender2D(EventRender2D e) {
        if (fullNullCheck()) return;

        float tickDelta = e.getTickDelta();

        for (Entity ent : mc.world.getEntities()) {
            if (!(ent instanceof TntEntity tnt)) continue;
            if (mc.player.squaredDistanceTo(tnt) > 64 * 64) continue;

            int fuse = tnt.getFuse();
            if (fuse <= 0) continue;

            float seconds = fuse / 20.0f;
            String label = String.format(Locale.US, "%.1f сек", seconds);

            Vec3d world = tnt.getLerpedPos(tickDelta).add(0.0, tnt.getHeight() + 0.5, 0.0);
            Vec3d screen = WorldUtils.getPosition(world);
            if (!(screen.z > 0) || !(screen.z < 1)) continue;

            float s = scale.getValue().floatValue();
            float pad = 3f;
            float textW = mc.textRenderer.getWidth(label);
            float textH = mc.textRenderer.fontHeight;

            e.getContext().getMatrices().push();
            e.getContext().getMatrices().translate((float) screen.x, (float) screen.y, 0);
            e.getContext().getMatrices().scale(s, s, 1.0f);

            float boxW = textW + pad * 2f;
            float boxH = textH + pad * 2f;
            float x = -boxW / 2f;
            float y = 0f;

            Render2D.drawRoundedRect(
                    e.getContext().getMatrices(),
                    x,
                    y,
                    boxW,
                    boxH,
                    4f,
                    new Color(15, 15, 16, 125)
            );

            int color = (textColor.getValue() | 0xFF000000);
            e.getContext().drawText(
                    mc.textRenderer,
                    Text.of(label),
                    (int) (x + pad),
                    (int) (y + pad),
                    color,
                    true
            );

            e.getContext().getMatrices().pop();
        }
    }
}
