package dev.simplevisuals.modules.impl.utility;

import dev.simplevisuals.modules.api.Category;
import dev.simplevisuals.modules.api.Module;
import dev.simplevisuals.modules.settings.impl.BooleanSetting;
import dev.simplevisuals.modules.settings.impl.StringSetting;
import dev.simplevisuals.client.managers.FriendsManager;
import lombok.Getter;
import net.minecraft.client.MinecraftClient;

import java.util.Locale;

public class NameProtect extends Module {

    @Getter
    private static NameProtect instance;

    @Getter
    private final StringSetting customName = new StringSetting("Замена ника", "Protected", false);
    
    @Getter
    private final BooleanSetting protectFriends = new BooleanSetting("Скрывать ники друзей", true);

    public NameProtect() {
        super("NameProtect", Category.Utility, "Заменяет твой ник везде: в чате, tab, scoreboard и над головой");
        instance = this;
    }

    /**
     * Возвращает подменённый ник для любого игрока
     * @param name Оригинальное имя игрока
     * @return Замененное имя или оригинальное если не нужно менять
     */
    public String getProtectedName(String name) {
        if (!isToggled() || name == null) return name;
        
        MinecraftClient mc = MinecraftClient.getInstance();

        // Скрываем свой ник
        if (mc.player != null && name.equals(mc.player.getGameProfile().getName())) {
            return customName.getValue();
        }

        // Скрываем друзей
        if (protectFriends.getValue() && FriendsManager.checkFriend(name)) {
            return "[Friend]";
        }

        return name;
    }

    /**
     * Модифицирует текст, заменяя ники (для чата, scoreboard, и т.д.)
     * @param text Оригинальный текст
     * @return Текст с замененными никами
     */
    public String replaceNames(String text) {
        if (!isToggled() || text == null) return text;
        
        String modified = text;
        MinecraftClient mc = MinecraftClient.getInstance();
        
        // Заменяем свой ник
        if (mc.player != null) {
            String playerName = mc.player.getGameProfile().getName();
            modified = replaceExactName(modified, playerName, customName.getValue());
        }

        // Заменяем ники друзей
        if (protectFriends.getValue()) {
            for (String friend : FriendsManager.getFriends()) {
                modified = replaceExactName(modified, friend, "[Friend]");
            }
        }
        
        return modified;
    }

    private String replaceExactName(String text, String target, String replacement) {
        if (text.isEmpty() || target == null || target.isEmpty()) return text;
        String lowerTarget = target.toLowerCase(Locale.ROOT);
        String lowerSource = text.toLowerCase(Locale.ROOT);
        StringBuilder result = new StringBuilder(text.length());
        int index = 0;
        while (index < text.length()) {
            int found = lowerSource.indexOf(lowerTarget, index);
            if (found == -1) {
                result.append(text.substring(index));
                break;
            }
            if (isBoundary(text, found, found + target.length())) {
                result.append(text, index, found);
                result.append(replacement);
                index = found + target.length();
            } else {
                result.append(text, index, found + 1);
                index = found + 1;
            }
        }
        return result.toString();
    }

    private boolean isBoundary(String text, int start, int end) {
        return (start == 0 || !Character.isLetterOrDigit(text.charAt(start - 1)))
                && (end >= text.length() || !Character.isLetterOrDigit(text.charAt(end)));
    }

}