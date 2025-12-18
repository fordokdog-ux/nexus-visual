package dev.simplevisuals.mixin;

import dev.simplevisuals.NexusVisual;
import dev.simplevisuals.client.events.impl.EventKey;
import dev.simplevisuals.modules.impl.render.BetterMinecraft;
import dev.simplevisuals.client.ui.hud.impl.PerfHUD;
import dev.simplevisuals.client.ui.hud.HudElement;
import net.minecraft.client.Keyboard;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public abstract class KeyboardMixin {

	@Inject(method = "onKey", at = @At("HEAD"))
	public void onKey(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
		EventKey event = new EventKey(key, action, modifiers);
		NexusVisual.getInstance().getEventHandler().post(event);

		// Ctrl + Shift + Q: toggle Perf HUD overlay (spawn on demand, not listed in elements)
		if (key == GLFW.GLFW_KEY_Q && action == GLFW.GLFW_PRESS
				&& (modifiers & GLFW.GLFW_MOD_CONTROL) != 0
				&& (modifiers & GLFW.GLFW_MOD_SHIFT) != 0) {
			try {
				var hudManager = NexusVisual.getInstance().getHudManager();
				if (hudManager != null) {
					PerfHUD perfHud = null;
					for (HudElement he : hudManager.getHudElements()) {
						if (he instanceof PerfHUD p) { perfHud = p; break; }
					}
					if (perfHud == null) {
						perfHud = new PerfHUD();
						// Set a default position near top-left if needed
						perfHud.setBounds(10, 10, 180, 120);
						hudManager.getHudElements().add(perfHud);
						NexusVisual.getInstance().getEventHandler().subscribe(perfHud);
					}
					perfHud.setToggled(!perfHud.isToggled());
				}
			} catch (Throwable ignored) {}
		}

		BetterMinecraft module = NexusVisual.getInstance().getModuleManager().getModule(BetterMinecraft.class);
		if (module == null || !module.isToggled()) return;

		// F5: сброс анимации отдаления при переключении
		if (key == GLFW.GLFW_KEY_F5 && action == GLFW.GLFW_PRESS && module.smoothThirdPersonZoom.getValue()) {
			module.getThirdPersonAnimation().reset();
		}

		// TAB: направление и сброс анимации открытия/закрытия
		if (key == GLFW.GLFW_KEY_TAB) {
			if (action == GLFW.GLFW_PRESS && module.smoothTab.getValue()) {
				module.setTabPressed(true);
				module.getTabOpenAnimation().reset();
				module.getTabOpenAnimation().update(true);
			} else if (action == GLFW.GLFW_RELEASE && module.smoothTab.getValue()) {
				module.setTabPressed(false);
				module.getTabOpenAnimation().reset();
				module.getTabOpenAnimation().update(false);
			}
		}
	}
}