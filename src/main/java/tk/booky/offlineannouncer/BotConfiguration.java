package tk.booky.offlineannouncer;
// Created by booky10 in OfflineAnnouncer (13:12 24.01.22)

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import reactor.util.Logger;
import reactor.util.Loggers;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class BotConfiguration {

    private static final Gson PRETTY_GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Logger LOGGER = Loggers.getLogger(BotConfiguration.class);
    private final File file;
    private Set<String> invalidPlugins = Set.of("SkinsRestorer", "AuthMe");
    private String token = "";

    public BotConfiguration(File file) {
        this.file = file;
    }

    public void loadConfiguration() {
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                fromJson(PRETTY_GSON.fromJson(reader, JsonObject.class));
            } catch (IOException exception) {
                throw new RuntimeException(exception);
            }
        } else {
            saveConfiguration();

            LOGGER.error("A default configuration has been created in " + file.getName() + ".");
            LOGGER.error("Please configure the config and restart the bot.");

            System.exit(1);
            throw new IllegalStateException();
        }
    }

    public void saveConfiguration() {
        try (FileWriter writer = new FileWriter(file)) {
            file.getAbsoluteFile().getParentFile().mkdirs();
            PRETTY_GSON.toJson(toJson(), writer);
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("token", token);

        JsonArray invalidPlugins = new JsonArray();
        for (String plugin : this.invalidPlugins) {
            invalidPlugins.add(plugin);
        }

        json.add("invalid_plugins", invalidPlugins);
        return json;
    }

    public void fromJson(JsonObject json) {
        Set<String> invalidPlugins = new HashSet<>();
        for (JsonElement element : json.getAsJsonArray("invalid_plugins")) {
            invalidPlugins.add(element.getAsString());
        }
        this.invalidPlugins = invalidPlugins;

        token = json.getAsJsonPrimitive("token").getAsString();
    }

    public File file() {
        return file;
    }

    public Set<String> invalidPlugins() {
        return invalidPlugins;
    }

    public void invalidPlugins(Set<String> invalidPlugins) {
        this.invalidPlugins = invalidPlugins;
    }

    public String token() {
        return token;
    }

    public void token(String token) {
        this.token = token;
    }
}
