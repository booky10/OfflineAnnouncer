package tk.booky.offlineannouncer;
// Created by booky10 in OfflineAnnouncer (13:16 24.01.22)

import discord4j.core.DiscordClient;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.presence.ClientActivity;
import discord4j.core.object.presence.ClientPresence;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.shard.ShardingStrategy;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.gateway.intent.Intent;
import discord4j.gateway.intent.IntentSet;
import discord4j.rest.util.AllowedMentions;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

@SuppressWarnings("ClassCanBeRecord")
public class OfflineAnnouncerBot {

    private static final Logger LOGGER = Loggers.getLogger(OfflineAnnouncerBot.class);
    private final BotConfiguration config;

    public OfflineAnnouncerBot(BotConfiguration config) {
        this.config = config;
    }

    public void start() {
        if (config.token() == null || config.token().isBlank()) {
            throw new IllegalArgumentException("Token \"" + config.token() + "\" is not a valid token.");
        }

        Mono<Void> login = DiscordClient.builder(config.token())
            .setDefaultAllowedMentions(AllowedMentions.suppressAll())
            .build().gateway()
            .setSharding(ShardingStrategy.recommended())
            .setEnabledIntents(IntentSet.of(Intent.GUILD_MESSAGES))
            .setInitialPresence(shard -> ClientPresence.online(ClientActivity.watching("spark reports")))
            .withGateway(gateway -> {
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

                return ready.then(message);
            });

        LOGGER.info("Logging in...");
        login.block();
    }
}
