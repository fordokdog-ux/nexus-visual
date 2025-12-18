package dev.simplevisuals.client.managers;

import com.google.gson.*;
import lombok.Getter;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FriendsManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final List<String> friends = new ArrayList<>();
    @Getter private static File friendsFile;

    public static void init(File globalsDir) {
        friendsFile = new File(globalsDir, "friends.json");
        load();
    }

    public static synchronized void addFriend(String name) {
        if (name == null || name.isEmpty()) return;
        if (!checkFriend(name)) {
            friends.add(name);
            save();
        }
    }

    public static synchronized void removeFriend(String name) {
        if (name == null) return;
        friends.removeIf(n -> n.equalsIgnoreCase(name));
        save();
    }

    public static synchronized boolean checkFriend(String name) {
        if (name == null) return false;
        for (String n : friends) {
            if (n.equalsIgnoreCase(name)) return true;
        }
        return false;
    }

    public static synchronized void clear() {
        friends.clear();
        save();
    }

    public static synchronized List<String> getFriends() {
        return Collections.unmodifiableList(friends);
    }

    public static synchronized void save() {
        if (friendsFile == null) return;
        JsonObject root = new JsonObject();
        JsonArray arr = new JsonArray();
        for (String n : friends) arr.add(n);
        root.add("friends", arr);
        try (FileWriter writer = new FileWriter(friendsFile)) {
            GSON.toJson(root, writer);
        } catch (IOException ignored) {}
    }

    public static synchronized void load() {
        if (friendsFile == null) return;
        if (!friendsFile.exists()) {
            save();
            return;
        }
        try (FileReader reader = new FileReader(friendsFile)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            friends.clear();
            if (root.has("friends")) {
                JsonArray arr = root.getAsJsonArray("friends");
                for (JsonElement e : arr) friends.add(e.getAsString());
            }
        } catch (Exception ignored) {}
    }
} 