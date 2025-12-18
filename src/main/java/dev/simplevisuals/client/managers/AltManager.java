package dev.simplevisuals.client.managers;

import com.google.gson.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.Session;
import dev.simplevisuals.client.ChatUtils;
import net.minecraft.client.resource.language.I18n;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Enhanced Alt Manager with improved error handling, validation, and additional features.
 * Stores simple offline (pirate) accounts as nicknames and allows switching.
 */
public class AltManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static File altsFile;
    private static final List<String> nicknames = new CopyOnWriteArrayList<>();
    private static String lastUsed = null;
    private static final int MAX_ALTS = 100; // Maximum number of alts to prevent abuse
    private static final int MAX_NICKNAME_LENGTH = 16;
    private static final int MIN_NICKNAME_LENGTH = 3;

    public static void init(File globalsDir) {
        altsFile = new File(globalsDir, "alts.json");
        load();
    }

    public static List<String> getNicknames() {
        return new ArrayList<>(nicknames);
    }

    public static boolean addNickname(String nickname) {
        if (nickname == null || nickname.trim().isEmpty()) {
            ChatUtils.sendMessage(I18n.translate("simplevisuals.alt.emptyNickname"));
            return false;
        }
        
        nickname = nickname.trim();
        
        if (!isValidNickname(nickname)) {
            ChatUtils.sendMessage(I18n.translate("simplevisuals.alt.invalidNickname"));
            return false;
        }
        
        if (nicknames.size() >= MAX_ALTS) {
            ChatUtils.sendMessage(I18n.translate("simplevisuals.alt.tooManyAlts", MAX_ALTS));
            return false;
        }
        
        if (nicknames.contains(nickname)) {
            ChatUtils.sendMessage(I18n.translate("simplevisuals.alt.alreadyExists"));
            return false;
        }
        
        try {
            nicknames.add(nickname);
            save();
            ChatUtils.sendMessage(I18n.translate("simplevisuals.alt.added", nickname));
            return true;
        } catch (Exception e) {
            ChatUtils.sendMessage(I18n.translate("simplevisuals.alt.addError"));
            return false;
        }
    }

    public static boolean removeNickname(String nickname) {
        if (nickname == null || nickname.trim().isEmpty()) {
            ChatUtils.sendMessage(I18n.translate("simplevisuals.alt.emptyNickname"));
            return false;
        }
        
        final String trimmedNickname = nickname.trim();
        boolean removed = nicknames.removeIf(n -> n.equalsIgnoreCase(trimmedNickname));
        
        if (!removed) {
            ChatUtils.sendMessage(I18n.translate("simplevisuals.alt.notFound"));
            return false;
        }
        
        if (lastUsed != null && lastUsed.equalsIgnoreCase(trimmedNickname)) {
            lastUsed = null;
        }
        
        try {
            save();
            ChatUtils.sendMessage(I18n.translate("simplevisuals.alt.removed", trimmedNickname));
            return true;
        } catch (Exception e) {
            ChatUtils.sendMessage(I18n.translate("simplevisuals.alt.removeError"));
            return false;
        }
    }

    public static boolean applyNickname(String nickname) {
        if (nickname == null || nickname.trim().isEmpty()) {
            ChatUtils.sendMessage(I18n.translate("simplevisuals.alt.emptyNickname"));
            return false;
        }
        
        nickname = nickname.trim();
        if (!isValidNickname(nickname)) {
            ChatUtils.sendMessage(I18n.translate("simplevisuals.alt.invalidNickname"));
            return false;
        }
        
        try {
            lastUsed = nickname;
            save();

            UUID uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + nickname).getBytes());
            Session session = new Session(nickname, uuid, "0", Optional.empty(), Optional.empty(), Session.AccountType.MSA);

            dev.simplevisuals.mixin.accessors.IMinecraftClientSessionAccessor accessor = (dev.simplevisuals.mixin.accessors.IMinecraftClientSessionAccessor) MinecraftClient.getInstance();
            accessor.simplevisuals$setSession(session);
            
            ChatUtils.sendMessage(I18n.translate("simplevisuals.alt.applied", nickname));
            return true;
        } catch (Exception e) {
            ChatUtils.sendMessage(I18n.translate("simplevisuals.alt.applyError"));
            return false;
        }
    }

    public static String getLastUsedNickname() {
        return lastUsed;
    }

    private static boolean isValidNickname(String nickname) {
        if (nickname == null || nickname.isEmpty()) {
            return false;
        }
        
        // Check length constraints
        if (nickname.length() < MIN_NICKNAME_LENGTH || nickname.length() > MAX_NICKNAME_LENGTH) {
            return false;
        }
        
        // Allow only Latin letters, digits, underscore; typical for MC
        return nickname.matches("[A-Za-z0-9_]+");
    }
    
    public static int getAltCount() {
        return nicknames.size();
    }
    
    public static int getMaxAlts() {
        return MAX_ALTS;
    }
    
    public static boolean hasNickname(String nickname) {
        return nicknames.contains(nickname);
    }
    
    public static void clearAllAlts() {
        nicknames.clear();
        lastUsed = null;
        save();
        ChatUtils.sendMessage(I18n.translate("simplevisuals.alt.cleared"));
    }

    private static void save() {
        if (altsFile == null) return;
        
        try {
            JsonObject root = new JsonObject();
            JsonArray arr = new JsonArray();
            for (String n : nicknames) {
                if (n != null && !n.trim().isEmpty()) {
                    arr.add(n);
                }
            }
            root.add("alts", arr);
            if (lastUsed != null) root.addProperty("lastUsed", lastUsed);
            root.addProperty("version", "1.0");
            root.addProperty("maxAlts", MAX_ALTS);
            
            try (FileWriter writer = new FileWriter(altsFile)) {
                GSON.toJson(root, writer);
            }
        } catch (IOException e) {
            ChatUtils.sendMessage(I18n.translate("simplevisuals.alt.saveError"));
        }
    }

    private static void load() {
        if (altsFile == null) return;
        if (!altsFile.exists()) {
            save();
            return;
        }
        
        try (FileReader reader = new FileReader(altsFile)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            nicknames.clear();
            
            if (root.has("alts")) {
                JsonArray arr = root.getAsJsonArray("alts");
                for (JsonElement e : arr) {
                    String nickname = e.getAsString();
                    if (nickname != null && !nickname.trim().isEmpty() && isValidNickname(nickname.trim())) {
                        nicknames.add(nickname.trim());
                    }
                }
            }
            
            if (root.has("lastUsed")) {
                String lastUsedNick = root.get("lastUsed").getAsString();
                if (lastUsedNick != null && !lastUsedNick.trim().isEmpty() && isValidNickname(lastUsedNick.trim())) {
                    lastUsed = lastUsedNick.trim();
                }
            }
            
            // Validate that lastUsed is still in the list
            if (lastUsed != null && !nicknames.contains(lastUsed)) {
                lastUsed = null;
            }
            
        } catch (Exception e) {
            ChatUtils.sendMessage(I18n.translate("simplevisuals.alt.loadError"));
            // Create a backup of corrupted file
            if (altsFile.exists()) {
                File backup = new File(altsFile.getParent(), "alts_backup_" + System.currentTimeMillis() + ".json");
                altsFile.renameTo(backup);
            }
            save(); // Create a new clean file
        }
    }
}



