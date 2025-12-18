package dev.simplevisuals.mixin;

import dev.simplevisuals.client.events.impl.EventAttackEntity;
import dev.simplevisuals.client.managers.HitDetectionManager;
import net.minecraft.entity.LivingEntity;
import dev.simplevisuals.NexusVisual;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class AttackEventMixin {

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void onAttack(Entity target, CallbackInfo ci) {
        PlayerEntity self = (PlayerEntity) (Object) this;
        // Отсечка фантомных ударов через hurttime цели (мягкий порог)
        if (target instanceof LivingEntity living && living.hurtTime > 2) {
            ci.cancel();
            return;
        }

        EventAttackEntity event = new EventAttackEntity(self, target);
        NexusVisual.getInstance().getEventHandler().post(event);
        if (event.isCancelled()) {
            ci.cancel();
            return;
        }
        // Если два удара почти одновременно (<=0.05ms), блокируем визуальные эффекты для второго
        boolean duplicateByTime = !dev.simplevisuals.client.managers.HitDetectionManager.getInstance().canProcessHit(self, target);
        if (duplicateByTime) {
            event.setEffectsAllowed(false);
        }
        // Регистрируем удар один раз, если не был отменен
        HitDetectionManager.getInstance().registerHit(self, target);
    }
} 