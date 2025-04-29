package org.mirgor.slacktimetracker.service;

import com.slack.api.Slack;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.methods.request.conversations.ConversationsHistoryRequest;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import com.slack.api.methods.response.conversations.ConversationsHistoryResponse;
import com.slack.api.methods.response.users.UsersInfoResponse;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class SlackService {

    @Value("${slack.bot.channel}")
    public String CHANNEL;

    @Value("${slack.bot.api_key}")
    private String API_KEY;

    private Slack slack;

    @PostConstruct
    void init() {
        slack = Slack.getInstance();
    }

    public void sendMessage(String message) {
        checkChat(CHANNEL);
        log.info("Sending a message to Slack!");

        ChatPostMessageRequest request = ChatPostMessageRequest.builder()
                .channel(CHANNEL)
                .text(message)
                .build();

        try {
            ChatPostMessageResponse chatPostMessageResponse = slack.methods(API_KEY).chatPostMessage(request);
            log.info("Message {} sent to chat {}", message, chatPostMessageResponse.getChannel());
        } catch (Exception e) {
            log.error("Error while sending a message to Slack. @error={}", e.getMessage());
        }
    }

    public ConversationsHistoryResponse readChannel() throws SlackApiException, IOException {
        checkChat(CHANNEL);
        log.info("Read messages from Slack!");

        ConversationsHistoryRequest historyRequest = ConversationsHistoryRequest.builder()
                .channel(CHANNEL)
                .limit(100)
                .build();

        return slack.methods(API_KEY).conversationsHistory(historyRequest);
    }

    public String getUserName(String userId) {
        try {
            UsersInfoResponse response = slack.methods(API_KEY).usersInfo(r -> r.user(userId));
            return response.getUser().getProfile().getDisplayName();
        } catch (Exception e) {
            return "";
        }
    }

    private void checkChat(String chatId) {
        if (!CHANNEL.equalsIgnoreCase(chatId)) {
            throw new IllegalArgumentException("Bot doesn't support this channel");
        }
    }
}
