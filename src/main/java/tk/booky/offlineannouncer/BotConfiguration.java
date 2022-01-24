package tk.booky.offlineannouncer;
// Created by booky10 in OfflineAnnouncer (13:12 24.01.22)

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import reactor.util.Logger;
import reactor.util.Loggers;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class BotConfiguration {

    private static final Gson PRETTY_GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Logger LOGGER = Loggers.getLogger(BotConfiguration.class);
    private final File file;
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
        return json;
    }

    public void fromJson(JsonObject json) {
        token = json.getAsJsonPrimitive("token").getAsString();
    }

    public File file() {
        return file;
    }

    public String token() {
        return token;
    }

    public void token(String token) {
        this.token = token;
    }
}
