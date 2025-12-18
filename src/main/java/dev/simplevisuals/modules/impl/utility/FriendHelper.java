package dev.simplevisuals.modules.impl.utility;

import dev.simplevisuals.client.events.impl.EventAttackEntity;
import dev.simplevisuals.client.managers.FriendsManager;
import dev.simplevisuals.client.ChatUtils;
import dev.simplevisuals.client.util.Wrapper;
import dev.simplevisuals.modules.api.Category;
import dev.simplevisuals.modules.api.Module;
import dev.simplevisuals.modules.settings.api.Bind;
import dev.simplevisuals.modules.settings.impl.BindSetting;
import dev.simplevisuals.modules.settings.impl.BooleanSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.client.resource.language.I18n;

public class FriendHelper extends Module {

    private final BindSetting friendKey = new BindSetting("setting.friendKey", new Bind(2, true));
    private final BooleanSetting noFriendDamage = new BooleanSetting("setting.noFriendDamage", true);

    public FriendHelper() {
        super("FriendHelper", Category.Utility, I18n.translate("module.friendhelper.description"));
        getSettings().add(friendKey);
        getSettings().add(noFriendDamage);
    }

    // Добавлено: публичный геттер, чтобы другие части кода (миксины) могли корректно узнать состояние
    public BooleanSetting getNoFriendDamage() {
        return noFriendDamage;
    }

    @EventHandler
    public void onAttack(EventAttackEntity e) {
        if (!isToggled() || !noFriendDamage.getValue()) return;

        Entity target = e.getTarget();
        if (target instanceof PlayerEntity player) {
            String namePrimary = player.getGameProfile().getName();
            String nameAlt = player.getName() != null ? player.getName().getString() : namePrimary;
            if (dev.simplevisuals.client.managers.FriendsManager.checkFriend(namePrimary) || dev.simplevisuals.client.managers.FriendsManager.checkFriend(nameAlt)) {
                e.cancel();
            }
        }
    }

    @EventHandler
    public void onMouse(dev.simplevisuals.client.events.impl.EventMouse event) {
        if (!isToggled()) return;
        if (event.getAction() != 1) return; // press
        if (friendKey.getValue().isMouse() && friendKey.getValue().getKey() == event.getButton()) {
            handleFriendAction();
        }
    }

    private void handleFriendAction() {
        if (mc.crosshairTarget == null || mc.crosshairTarget.getType() != HitResult.Type.ENTITY) return;
        EntityHitResult ehr = (EntityHitResult) mc.crosshairTarget;
        if (!(ehr.getEntity() instanceof PlayerEntity player)) return;
        String playerName = player.getName().getString();
        if (dev.simplevisuals.client.managers.FriendsManager.checkFriend(playerName)) {
            dev.simplevisuals.client.managers.FriendsManager.removeFriend(playerName);
            ChatUtils.sendMessage(String.format(I18n.translate("friend.removed"), playerName));
        } else {
            dev.simplevisuals.client.managers.FriendsManager.addFriend(playerName);
            ChatUtils.sendMessage(String.format(I18n.translate("friend.added"), playerName));
        }
    }
}
