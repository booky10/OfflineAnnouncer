package tk.booky.offlineannouncer;
// Created by booky10 in OfflineAnnouncer (12:59 24.01.22)

import reactor.util.Logger;
import reactor.util.Loggers;

import java.io.File;

public class OfflineAnnouncerMain {

    private static final Logger LOGGER = Loggers.getLogger(OfflineAnnouncerMain.class);

    public static void main(String[] args) {
        Thread.currentThread().setName("config-loader");
        String configFileName = System.getProperty("of.config");
        if (configFileName == null) configFileName = "config.json";
        File configFile = new File(configFileName);

        LOGGER.info("Loading config from " + configFile.getName() + "...");
        BotConfiguration config = new BotConfiguration(configFile);
        config.loadConfiguration();

        Thread.currentThread().setName("bot-starter");
        LOGGER.info("Starting bot client...");
        new OFBot(config).start();
    }
}
