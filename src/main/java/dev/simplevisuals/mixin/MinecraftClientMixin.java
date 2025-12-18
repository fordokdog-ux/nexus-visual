package dev.simplevisuals.mixin;

import dev.simplevisuals.NexusVisual;
import dev.simplevisuals.client.events.impl.EventTick;
import dev.simplevisuals.client.events.impl.EventGameShutdown;
import dev.simplevisuals.client.util.math.Counter;
import dev.simplevisuals.client.managers.HitDetectionManager;
import dev.simplevisuals.client.util.Wrapper;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin implements Wrapper {
    
    private static int tickCounter = 0;

    @Inject(method = "tick", at = @At("HEAD"))
    public void tick(CallbackInfo ci) {
        EventTick event = new EventTick();
        NexusVisual.getInstance().getEventHandler().post(event);
        Counter.updateFPS();
        
        // Периодическая очистка HitDetectionManager (каждые 20 тиков = 1 секунда)
        tickCounter++;
        if (tickCounter >= 20) {
            HitDetectionManager.getInstance().cleanup();
            tickCounter = 0;
        }
    }

    @Inject(method = "getWindowTitle", at = @At("HEAD"), cancellable = true)
    public void updateWindowTitle(CallbackInfoReturnable<String> cir) {
        cir.setReturnValue("Nexus Visual 0.1 ALPHA");
    }

    @Inject(method = "stop", at = @At("HEAD"))
    private void onStop(CallbackInfo ci) {
        // Публикуем событие выключения игры для автосохранения конфигурации
        NexusVisual.getInstance().getEventHandler().post(new EventGameShutdown());
    }
}