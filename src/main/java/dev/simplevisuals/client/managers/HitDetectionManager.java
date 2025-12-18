package dev.simplevisuals.client.managers;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Менеджер для предотвращения множественных срабатываний ударов
 * с погрешностью 5ms (5 миллисекунд)
 */
public class HitDetectionManager {
    
    private static HitDetectionManager instance;
    
    // Погрешность в наносекундах (5ms = 5 миллисекунд)
    private static final long HIT_TOLERANCE_NANOS = 5_000_000L; // 5 миллисекунд в наносекундах
    
    // Хранит время последнего удара для каждой пары игрок-цель
    private final Map<String, Long> lastHitTimes = new ConcurrentHashMap<>();
    
    private HitDetectionManager() {}
    
    public static HitDetectionManager getInstance() {
        if (instance == null) {
            instance = new HitDetectionManager();
        }
        return instance;
    }
    
    /**
     * Проверяет, можно ли обработать удар (не слишком рано после предыдущего)
     * @param attacker атакующий игрок
     * @param target цель атаки
     * @return true если удар можно обработать, false если это дубликат
     */
    public boolean canProcessHit(PlayerEntity attacker, Entity target) {
        if (attacker == null || target == null) {
            return true; // Если данные некорректны, разрешаем обработку
        }
        
        String hitKey = generateHitKey(attacker, target);
        long currentTime = System.nanoTime();
        
        Long lastHitTime = lastHitTimes.get(hitKey);
        if (lastHitTime == null) {
            // Первый удар по этой цели
            return true;
        }
        
        long timeDifference = currentTime - lastHitTime;
        
        if (timeDifference >= HIT_TOLERANCE_NANOS) {
            // Прошло достаточно времени, разрешаем обработку
            return true;
        }
        
        // Удар произошел слишком рано, игнорируем
        return false;
    }
    
    /**
     * Принудительно регистрирует удар (для случаев, когда нужно обновить время)
     * @param attacker атакующий игрок
     * @param target цель атаки
     */
    public void registerHit(PlayerEntity attacker, Entity target) {
        if (attacker == null || target == null) {
            return;
        }
        
        String hitKey = generateHitKey(attacker, target);
        lastHitTimes.put(hitKey, System.nanoTime());
    }
    
    /**
     * Очищает старые записи для предотвращения утечек памяти
     * Рекомендуется вызывать периодически
     */
    public void cleanup() {
        long currentTime = System.nanoTime();
        long maxAge = 1_000_000_000L; // 1 секунда в наносекундах
        
        lastHitTimes.entrySet().removeIf(entry -> 
            (currentTime - entry.getValue()) > maxAge
        );
    }
    
    /**
     * Генерирует уникальный ключ для пары атакующий-цель
     */
    private String generateHitKey(PlayerEntity attacker, Entity target) {
        return attacker.getUuid().toString() + "->" + target.getUuid().toString();
    }
    
    /**
     * Получает время последнего удара для отладки
     */
    public long getLastHitTime(PlayerEntity attacker, Entity target) {
        String hitKey = generateHitKey(attacker, target);
        Long lastHitTime = lastHitTimes.get(hitKey);
        return lastHitTime != null ? lastHitTime : 0L;
    }
    
    /**
     * Получает время между последним ударом и текущим временем в миллисекундах
     */
    public double getTimeSinceLastHit(PlayerEntity attacker, Entity target) {
        String hitKey = generateHitKey(attacker, target);
        Long lastHitTime = lastHitTimes.get(hitKey);
        if (lastHitTime == null) {
            return Double.MAX_VALUE; // Никогда не было удара
        }
        return (System.nanoTime() - lastHitTime) / 1_000_000.0; // Конвертируем в миллисекунды
    }
    
    /**
     * Получает количество активных записей для отладки
     */
    public int getActiveHitRecords() {
        return lastHitTimes.size();
    }
}
