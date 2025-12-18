package dev.simplevisuals.mixin.accessors;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.Session;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.Mutable;

@Mixin(MinecraftClient.class)
public interface IMinecraftClientSessionAccessor {
    @Accessor("session")
    @Mutable
    void simplevisuals$setSession(Session session);
}



