package dev.simplevisuals.mixin;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.simplevisuals.client.ChatUtils;
import dev.simplevisuals.NexusVisual;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.command.CommandSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.MinecraftClient;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin {

	@Inject(method = "sendMessage", at = @At("HEAD"), cancellable = true)
	private void onSendMessage(String message, boolean addToHistory, CallbackInfo ci) {
		if (message != null) {
			// Always allow .reset with a dot, regardless of current prefix
			if (message.startsWith(".reset")) {
				CommandDispatcher<CommandSource> dispatcher = NexusVisual.getInstance().getCommandManager().getDispatcher();
				StringReader reader = new StringReader("reset");
				try {
					dispatcher.execute(reader, NexusVisual.getInstance().getCommandManager().getSource());
				} catch (CommandSyntaxException e) {
					ChatUtils.sendMessage(String.format(I18n.translate("chat.error"), e.getRawMessage().getString()));
				}
				if (addToHistory) MinecraftClient.getInstance().inGameHud.getChatHud().addToMessageHistory(message);
				ci.cancel();
				return;
			}

			String prefix = NexusVisual.getInstance().getCommandManager().getPrefix();
			if (message.startsWith(prefix)) {
				CommandDispatcher<CommandSource> dispatcher = NexusVisual.getInstance().getCommandManager().getDispatcher();
				StringReader reader = new StringReader(message.substring(prefix.length()));
				try {
					dispatcher.execute(reader, NexusVisual.getInstance().getCommandManager().getSource());
				} catch (CommandSyntaxException e) {
					ChatUtils.sendMessage(String.format(I18n.translate("chat.error"), e.getRawMessage().getString()));
				}
				if (addToHistory) MinecraftClient.getInstance().inGameHud.getChatHud().addToMessageHistory(message);
				ci.cancel();
			}
		}
	}
}