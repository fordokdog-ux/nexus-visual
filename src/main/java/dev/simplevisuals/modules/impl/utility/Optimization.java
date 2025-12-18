package dev.simplevisuals.modules.impl.utility;

import dev.simplevisuals.client.events.impl.EventTick;
import dev.simplevisuals.modules.api.Category;
import dev.simplevisuals.modules.api.Module;
import dev.simplevisuals.modules.settings.impl.BooleanSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.option.CloudRenderMode;

public class Optimization extends Module {
    
    private final BooleanSetting disableParticles = new BooleanSetting("Партиклы", false);
    private final BooleanSetting disableClouds = new BooleanSetting("Облака", false);
    private final BooleanSetting reduceEntityRendering = new BooleanSetting("Энтити", false);
    
    // Сохраняем оригинальные настройки
    private CloudRenderMode originalCloudMode;
    private double originalEntityDistance;
    
    public Optimization() {
        super("Optimization", Category.Utility, "Оптимизация игры для слабых ПК");
    }
    
    @Override
    public void onEnable() {
        super.onEnable();
        if (mc.options != null) {
            originalCloudMode = mc.options.getCloudRenderMode().getValue();
            originalEntityDistance = mc.options.getEntityDistanceScaling().getValue();
        }
        applyOptimizations();
    }
    
    @Override
    public void onDisable() {
        super.onDisable();
        restoreSettings();
    }
    
    @EventHandler
    private void onTick(EventTick event) {
        applyOptimizations();
    }
    
    private void applyOptimizations() {
        if (mc.options == null) return;
        
        // Облака
        if (disableClouds.getValue()) {
            mc.options.getCloudRenderMode().setValue(CloudRenderMode.OFF);
        }
        
        // Уменьшаем дистанцию рендера энтити
        if (reduceEntityRendering.getValue()) {
            mc.options.getEntityDistanceScaling().setValue(0.5);
        }
    }
    
    private void restoreSettings() {
        if (mc.options == null) return;
        
        if (originalCloudMode != null) {
            mc.options.getCloudRenderMode().setValue(originalCloudMode);
        }
        if (originalEntityDistance > 0) {
            mc.options.getEntityDistanceScaling().setValue(originalEntityDistance);
        }
    }
    
    public boolean shouldDisableParticles() {
        return isToggled() && disableParticles.getValue();
    }
}
