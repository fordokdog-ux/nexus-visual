package dev.simplevisuals.mixin;

import dev.simplevisuals.client.util.Wrapper;
import dev.simplevisuals.NexusVisual;
import dev.simplevisuals.client.events.impl.TotemPopEvent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity implements Wrapper {

    public LivingEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    // 35 is the vanilla entity status byte used for the Totem of Undying pop effect.
    @Inject(method = "handleStatus", at = @At("HEAD"))
    private void simplevisuals$onHandleStatus(byte status, CallbackInfo ci) {
        if (status != 35) return;
        try {
            NexusVisual.getInstance().getEventHandler().post(new TotemPopEvent((Entity) (Object) this));
        } catch (Throwable ignored) {}
    }

}