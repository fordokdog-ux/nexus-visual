package dev.simplevisuals.client.events.impl;

import dev.simplevisuals.client.events.Event;
import dev.simplevisuals.client.managers.HitDetectionManager;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

@AllArgsConstructor @Getter
public class EventAttackEntity extends Event {
    private final PlayerEntity player;
    private final Entity target;
    private boolean effectsAllowed = true;

    public EventAttackEntity(PlayerEntity player, Entity target) {
        this.player = player;
        this.target = target;
        this.effectsAllowed = true;
    }
    
    /**
     * Проверяет, можно ли обработать этот удар (не является ли он дубликатом)
     * @return true если удар можно обработать, false если это дубликат
     */
    public boolean canProcess() {
        HitDetectionManager manager = HitDetectionManager.getInstance();
        return manager.canProcessHit(player, target);
    }

    public boolean isEffectsAllowed() {
        return effectsAllowed;
    }

    public void setEffectsAllowed(boolean allowed) {
        this.effectsAllowed = allowed;
    }
    
    /**
     * Принудительно регистрирует этот удар в системе отслеживания
     */
    public void registerHit() {
        HitDetectionManager.getInstance().registerHit(player, target);
    }
}