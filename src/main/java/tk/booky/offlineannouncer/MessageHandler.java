package tk.booky.offlineannouncer;
// Created by booky10 in OfflineAnnouncer (13:48 24.01.22)

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import discord4j.core.event.domain.message.MessageCreateEvent;
import me.lucko.spark.proto.SparkHeapProtos.HeapData;
import me.lucko.spark.proto.SparkProtos.CommandSenderMetadata;
import me.lucko.spark.proto.SparkSamplerProtos.SamplerData;
import tk.booky.offlineannouncer.util.ExpiringMap;
import tk.booky.offlineannouncer.util.FastUuidSansHyphens;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageHandler {

    private static final Pattern SPARK_URL_PATTERN = Pattern.compile("(https?://)?spark\\.lucko\\.me/(\\w{10})");
    private static final String MOJANG_URL = "https://api.mojang.com/users/profiles/minecraft/";
    private static final Map<String, SamplerData> SAMPLERS = new ExpiringMap<>(60 * 2); // Two hours
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{2,16}$");
    private static final Map<String, HeapData> HEAPS = new ExpiringMap<>(60 * 2); // Two hours
    private static final Map<String, String> UUIDS = new ExpiringMap<>(60); // One hour
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final String BYTE_BIN_URL = "https://bytebin.lucko.me/";
    private static final Gson GSON = new Gson();

    public static String handleMessage(MessageCreateEvent event, BotConfiguration config) {
        String stripped = event.getMessage().getContent().replaceAll("[_´~*|\\\\]", "");
        Matcher matcher = SPARK_URL_PATTERN.matcher(stripped);

        while (matcher.find()) {
            String sparkId = matcher.group(2);
            Object data = downloadSparkData(sparkId);

            CommandSenderMetadata creator;
            if (data instanceof SamplerData) {
                creator = ((SamplerData) data).getMetadata().getCreator();
            } else if (data instanceof HeapData) {
                creator = ((HeapData) data).getMetadata().getCreator();
            } else {
                throw new IllegalStateException("Unknown type of data " + data);
            }

            if (creator.getType() != CommandSenderMetadata.Type.PLAYER) {
                // The plugins can only be searched with the sampler, the heap summary
                // doesn't contain the server plugins.
                if (data instanceof SamplerData) {
                    String result = searchPlugins((SamplerData) data, config);
                    if (result != null) return result;
                }
            } else if (!USERNAME_PATTERN.matcher(creator.getName()).matches()) {
                return "Invalid username: " + creator.getName();
            } else {
                UUID offlineUuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + creator.getName())
                    .getBytes(StandardCharsets.UTF_8));
                UUID uniqueId = UUID.fromString(creator.getUniqueId());

                if (offlineUuid.equals(uniqueId)) {
                    return "Offline unique id: " + creator.getUniqueId();
                } else {
                    String undashedId;
                    synchronized (UUIDS) {
                        undashedId = UUIDS.computeIfAbsent(creator.getName(), username -> {
                            try {
                                String fetchResponse = HTTP_CLIENT.send(HttpRequest.newBuilder(URI.create(
                                    MOJANG_URL + username)).build(), HttpResponse.BodyHandlers.ofString()).body();
                                return GSON.fromJson(fetchResponse, JsonObject.class)
                                    .getAsJsonPrimitive("id").getAsString();
                            } catch (IOException | InterruptedException throwable) {
                                throw new RuntimeException(throwable);
                            }
                        });
                    }

                    if (undashedId.equals(FastUuidSansHyphens.toString(uniqueId))) {
                        // Player is confirmed to be verified, now check the server plugins
                        // The plugins can only be searched with the sampler, the heap summary
                        // doesn't contain the server plugins.
                        if (data instanceof SamplerData) {
                            String result = searchPlugins((SamplerData) data, config);
                            if (result != null) return result;
                        }
                    } else {
                        return "Invalid unique id " + creator.getUniqueId() + " for player " + creator.getName();
                    }
                }
            }
        }

        return null;
    }

    private static Object downloadSparkData(String sparkId) {
        try {
            SamplerData sampler = SAMPLERS.get(sparkId);
            if (sampler != null) return sampler;

            HeapData heap = HEAPS.get(sparkId);
            if (heap != null) return heap;

            HttpResponse<byte[]> response = HTTP_CLIENT.send(HttpRequest.newBuilder(URI.create(
                BYTE_BIN_URL + sparkId)).build(), HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200) {
                throw new IllegalStateException("Unsuccessful status code " +
                    response.statusCode() + " for spark " + sparkId);
            }

            String contentType = response.headers().firstValue("Content-Type").orElse("");
            byte[] bytes = new byte[0];

            if (contentType.equals("application/x-spark-sampler")) {
                sampler = SamplerData.parseFrom(response.body());
                SAMPLERS.put(sparkId, sampler);
                if (sampler != null) return sampler;
            } else {
                bytes = response.body();
            }

            heap = HeapData.parseFrom(bytes);
            HEAPS.put(sparkId, heap);
            return heap;
        } catch (IOException | InterruptedException throwable) {
            throw new RuntimeException(throwable);
        }
    }

    private static String searchPlugins(SamplerData data, BotConfiguration config) {
        Set<String> sources = new HashSet<>(data.getClassSourcesMap().values());
        for (String invalidPlugin : config.invalidPlugins()) {
            if (sources.contains(invalidPlugin)) {
                return "Invalid plugin " + invalidPlugin;
            }
        }
        return null;
    }
}
