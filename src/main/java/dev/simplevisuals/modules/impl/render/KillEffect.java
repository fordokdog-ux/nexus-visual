package dev.simplevisuals.modules.impl.render;

import dev.simplevisuals.client.events.impl.EventAttackEntity;
import dev.simplevisuals.client.events.impl.EventTick;
import dev.simplevisuals.client.events.impl.TotemPopEvent;
import dev.simplevisuals.modules.api.Category;
import dev.simplevisuals.modules.api.Module;
import dev.simplevisuals.modules.settings.api.Nameable;
import dev.simplevisuals.modules.settings.impl.BooleanSetting;
import dev.simplevisuals.modules.settings.impl.EnumSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.block.Blocks;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class KillEffect extends Module {

    private enum EffectMode implements Nameable {
        LIGHTNING("Молния"),
        BLOOD("Кровь"),
        EXPLOSION("Взрыв"),
        HEART("Сердце"),
        SMOKE("Дым");

        private final String name;

        EffectMode(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }

    private final EnumSetting<EffectMode> effect = new EnumSetting<>("Эффект", EffectMode.LIGHTNING);
    private final BooleanSetting onlyPlayers = new BooleanSetting("Только игроки", false);
    private final BooleanSetting onMobs = new BooleanSetting("На мобов", true);
    private final BooleanSetting onTotemPop = new BooleanSetting("На тотем", true);

    private static final long TRACK_MS = 1400L;
    private final Map<Integer, Long> pendingKills = new HashMap<>();

    public KillEffect() {
        super("KillEffect", Category.Render, "Эффекты при убийстве");
        getSettings().add(effect);
        getSettings().add(onlyPlayers);
        getSettings().add(onMobs);
        getSettings().add(onTotemPop);
    }

    @EventHandler
    public void onTotemPop(TotemPopEvent e) {
        if (!onTotemPop.getValue()) return;
        if (fullNullCheck()) return;

        Entity ent = e.getEntity();
        if (!(ent instanceof LivingEntity living)) return;
        if (!isAllowedTarget(living)) return;

        doKillEffect(living);
    }

    @EventHandler
    public void onAttack(EventAttackEntity e) {
        if (fullNullCheck()) return;
        if (!e.isEffectsAllowed()) return;
        if (e.getPlayer() != mc.player) return;

        Entity target = e.getTarget();
        if (!(target instanceof LivingEntity living)) return;
        if (!isAllowedTarget(living)) return;

        pendingKills.put(living.getId(), System.currentTimeMillis() + TRACK_MS);
    }

    @EventHandler
    public void onTick(EventTick e) {
        if (fullNullCheck()) {
            pendingKills.clear();
            return;
        }

        long now = System.currentTimeMillis();
        Iterator<Map.Entry<Integer, Long>> it = pendingKills.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Long> entry = it.next();
            int id = entry.getKey();
            long until = entry.getValue();

            if (now > until) {
                it.remove();
                continue;
            }

            Entity ent = mc.world.getEntityById(id);
            if (!(ent instanceof LivingEntity living)) {
                it.remove();
                continue;
            }

            if (!living.isAlive()) {
                if (isAllowedTarget(living)) {
                    doKillEffect(living);
                }
                it.remove();
            }
        }
    }

    private boolean isAllowedTarget(LivingEntity target) {
        boolean isPlayer = target instanceof PlayerEntity;
        if (onlyPlayers.getValue() && !isPlayer) return false;
        if (!onMobs.getValue() && !isPlayer) return false;
        return true;
    }

    private void doKillEffect(LivingEntity entity) {
        if (entity == null || mc.player == null || mc.world == null) return;

        Vec3d pos = entity.getPos();
        BlockPos blockPos = entity.getBlockPos();

        switch (effect.getValue()) {
            case LIGHTNING -> {
                LightningEntity lightning = new LightningEntity(net.minecraft.entity.EntityType.LIGHTNING_BOLT, mc.world);
                lightning.refreshPositionAfterTeleport(pos.x, pos.y, pos.z);
                lightning.setCosmetic(true);
                mc.world.addEntity(lightning);
                mc.world.playSound(mc.player, blockPos, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.WEATHER, 5f, 1f);
            }
            case BLOOD -> {
                // Красные осколки как "кровь" через block particles
                BlockStateParticleEffect p = new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.REDSTONE_BLOCK.getDefaultState());
                for (int i = 0; i < 32; i++) {
                    mc.world.addParticle(p,
                            pos.x + (ThreadLocalRandom.current().nextDouble() - 0.5) * 1.2,
                            pos.y + ThreadLocalRandom.current().nextDouble() * 1.6,
                            pos.z + (ThreadLocalRandom.current().nextDouble() - 0.5) * 1.2,
                            (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.18,
                            ThreadLocalRandom.current().nextDouble() * 0.15,
                            (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.18);
                }
            }
            case EXPLOSION -> {
                for (int i = 0; i < 30; i++) {
                    mc.world.addParticle(ParticleTypes.EXPLOSION,
                            pos.x + (ThreadLocalRandom.current().nextDouble() - 0.5) * 3,
                            pos.y + ThreadLocalRandom.current().nextDouble() * 2,
                            pos.z + (ThreadLocalRandom.current().nextDouble() - 0.5) * 3,
                            (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.2,
                            ThreadLocalRandom.current().nextDouble() * 0.2,
                            (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.2);
                }
                mc.world.playSound(mc.player, blockPos, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.BLOCKS, 1f, 1f);
            }
            case HEART -> {
                for (int i = 0; i < 20; i++) {
                    mc.world.addParticle(ParticleTypes.HEART,
                            pos.x + (ThreadLocalRandom.current().nextDouble() - 0.5) * 2,
                            pos.y + ThreadLocalRandom.current().nextDouble() * 2,
                            pos.z + (ThreadLocalRandom.current().nextDouble() - 0.5) * 2,
                            0,
                            0.1,
                            0);
                }
                mc.world.playSound(mc.player, blockPos, SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.5f, 1.5f);
            }
            case SMOKE -> {
                for (int i = 0; i < 50; i++) {
                    mc.world.addParticle(ParticleTypes.LARGE_SMOKE,
                            pos.x + (ThreadLocalRandom.current().nextDouble() - 0.5) * 3,
                            pos.y + ThreadLocalRandom.current().nextDouble() * 2,
                            pos.z + (ThreadLocalRandom.current().nextDouble() - 0.5) * 3,
                            (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.05,
                            ThreadLocalRandom.current().nextDouble() * 0.1,
                            (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.05);
                }
                mc.world.playSound(mc.player, blockPos, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 1f, 1f);
            }
        }
    }
}
