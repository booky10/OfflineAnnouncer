package tk.booky.offlineannouncer;
// Created by booky10 in OfflineAnnouncer (13:16 24.01.22)

import discord4j.core.DiscordClient;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.presence.ClientActivity;
import discord4j.core.object.presence.ClientPresence;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.rest.request.GlobalRateLimiter;
import discord4j.rest.util.AllowedMentions;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

public class OFBot {

    private static final Logger LOGGER = Loggers.getLogger(OFBot.class);
    private final BotConfiguration config;

    public OFBot(BotConfiguration config) {
        this.config = config;
    }

    public void start() {
        if (config.token() == null || config.token().isBlank()) {
            throw new IllegalArgumentException("Token \"" + config.token() + "\" is not a valid token.");
        }

        DiscordClient client = DiscordClient.builder(config.token())
            .setDefaultAllowedMentions(AllowedMentions.suppressAll())
            .setGlobalRateLimiter(GlobalRateLimiter.create()).build();

        Mono<Void> login = client.withGateway(gateway -> {
            Mono<Void> ready = gateway.on(ReadyEvent.class, event -> Mono.fromRunnable(() ->
                LOGGER.info("Logged in as " + event.getSelf().getTag()))).then();

            Mono<Void> message = gateway.on(MessageCreateEvent.class, event -> {
                if (event.getGuildId().isPresent()) {
                    String result = MessageHandler.handleMessage(event, config);
                    if (result != null) {
                        return event.getMessage().addReaction(ReactionEmoji.unicode("🤔"))
                            .and(event.getMessage().getChannel().flatMap(channel ->
                                channel.createMessage(MessageCreateSpec.builder()
                                    .messageReference(event.getMessage().getId())
                                    .content("Are you using offline mode? (" + result + ")").build())));
                    }
                }

                return Mono.empty();
            }).then();

            Mono<Void> presence = gateway.updatePresence(ClientPresence.online(
                ClientActivity.watching("spark reports"))).then();

            return ready.and(message).and(presence);
        });

        LOGGER.info("Logging in...");
        login.block();
    }
}
