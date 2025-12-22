package dev.simplevisuals.modules.impl.utility;

import com.mojang.authlib.GameProfile;
import dev.simplevisuals.modules.api.Category;
import dev.simplevisuals.modules.api.Module;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.Entity;

import java.util.UUID;

public class FakePlayer extends Module {

    private OtherClientPlayerEntity fake;

    public FakePlayer() {
        super("FakePlayer", Category.Utility, "Спавнит клиентского фейкового игрока для тестов");
    }

    @Override
    public void onEnable() {
        super.onEnable();
        spawn();
    }

    @Override
    public void onDisable() {
        despawn();
        super.onDisable();
    }

    private void spawn() {
        if (fullNullCheck()) return;

        despawn();

        GameProfile profile = new GameProfile(UUID.randomUUID(), "FakePlayer");
        fake = new OtherClientPlayerEntity(mc.world, profile);

        fake.refreshPositionAndAngles(mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.getYaw(), mc.player.getPitch());
        fake.setHeadYaw(mc.player.getHeadYaw());
        fake.bodyYaw = mc.player.bodyYaw;

        fake.setVelocity(0, 0, 0);
        fake.setNoGravity(true);
        fake.setInvulnerable(true);
        fake.setSilent(true);

        mc.world.addEntity(fake);
    }

    private void despawn() {
        if (fake == null) return;
        if (mc.world != null) {
            try {
                mc.world.removeEntity(fake.getId(), Entity.RemovalReason.DISCARDED);
            } catch (Throwable t) {
                try {
                    fake.discard();
                } catch (Throwable ignored) {
                }
            }
        }
        fake = null;
    }
}
