package dev.simplevisuals.client.util.Network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import dev.simplevisuals.client.util.render.Wrapper;
import dev.simplevisuals.NexusVisual;
import lombok.experimental.UtilityClass;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.ReadableScoreboardScore;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.number.StyledNumberFormat;
import net.minecraft.text.MutableText;
import net.minecraft.world.GameMode;

import static dev.simplevisuals.client.util.Wrapper.mc;

@UtilityClass
public class Server implements Wrapper {

    public boolean is(String server) {
        if (mc.getNetworkHandler() == null || mc.getNetworkHandler().getServerInfo() == null) return false;
        return mc.getNetworkHandler().getServerInfo().address.toLowerCase().contains(server);
    }

    public int getPing(PlayerEntity entity) {
        PlayerListEntry list = mc.getNetworkHandler().getPlayerListEntry(entity.getUuid());
        return list != null ? list.getLatency() : 0;
    }

    public float getHealth(LivingEntity entity, boolean gapple) {
        if (entity == null) {
            return 0f; // Early return for null entity
        }

        // Fallback health calculation
        float fallbackHealth = entity.getHealth() + (gapple ? entity.getAbsorptionAmount() : 0f);
        {

            if (entity instanceof PlayerEntity player) {
                // Check if scoreboard objective exists
                ScoreboardObjective objective = player.getScoreboard() != null
                        ? player.getScoreboard().getObjectiveForSlot(ScoreboardDisplaySlot.BELOW_NAME)
                        : null;
                if (objective == null) {
                    return fallbackHealth; // Return fallback if no objective
                }

                // Get score and formatted text
                ReadableScoreboardScore score = player.getScoreboard().getScore(player, objective);
                if (score == null) {
                    return fallbackHealth; // Return fallback if no score
                }

                MutableText text = ReadableScoreboardScore.getFormattedScore(score, objective.getNumberFormatOr(StyledNumberFormat.EMPTY));
                String healthStr = text.getString().replaceAll("\\D", "");

                // Check if the string is empty or invalid
                if (healthStr.isEmpty()) {
                    return fallbackHealth; // Return fallback if string is empty
                }

                try {
                    return Float.parseFloat(healthStr);
                } catch (NumberFormatException e) {
                    // Log error for debugging
                    System.err.println("Failed to parse health string: " + healthStr + " for player: " + player.getName().getString());
                    return fallbackHealth; // Return fallback if parsing fails
                }
            }
        }

        return fallbackHealth; // Default case
    }
}