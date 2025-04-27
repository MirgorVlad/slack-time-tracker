package org.mirgor.slacktimetracker.service;

import com.slack.api.Slack;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.methods.request.conversations.ConversationsHistoryRequest;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import com.slack.api.methods.response.conversations.ConversationsHistoryResponse;
import com.slack.api.methods.response.users.UsersInfoResponse;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class TimeTrackerService {

    @Value("${slack.bot.channel}")
    public String CHANNEL;

    @Value("${slack.bot.api_key}")
    private String API_KEY;

    @Value("${google.sheet.id}")
    private String SPREADSHEET_ID;

    private Slack slack;
    private final SheetsService sheetsService;

    @PostConstruct
    void init() {
        slack = Slack.getInstance();
    }

    @Scheduled(cron = "0 0 10 * * ?")
    public void sendTrackerMsg() {
        checkChat(CHANNEL);
        log.info("Sending a message to Slack!");

        ChatPostMessageRequest request = ChatPostMessageRequest.builder()
                .channel(CHANNEL)
                .text(":wave: Time to track development time")
                .build();

        try {
            ChatPostMessageResponse chatPostMessageResponse = slack.methods(API_KEY).chatPostMessage(request);
            log.info(chatPostMessageResponse.getChannel());
        } catch (Exception e) {
            log.error("Error while sending a message to Slack. @error={}", e.getMessage());
        }
    }

    @Scheduled(cron = "0 0 12 * * ?")
    public void getTime() {
        checkChat(CHANNEL);
        log.info("Read messages from Slack!");

        ConversationsHistoryRequest historyRequest = ConversationsHistoryRequest.builder()
                .channel(CHANNEL)
                .limit(100)
                .build();

        try {
            ConversationsHistoryResponse conversationsHistoryResponse = slack.methods(API_KEY)
                    .conversationsHistory(historyRequest);
            List<TimeInfo> list = conversationsHistoryResponse.getMessages().stream()
                    .filter(message -> validateTs(message.getTs()))
                    .peek(message -> log.info(getUserName(message.getUser())))
                    .flatMap(message -> Arrays.stream(message.getText().split("\n")))
                    .map(TimeTrackerService::getTimeInfo)
                    .toList();

            sheetsService.addTime(SPREADSHEET_ID, list);
        } catch (
                Exception e) {
            log.error("Error while reading messages from Slack. @error={}", e.getMessage());
        }
    }

    private String getUserName(String userId) {
        try {
            UsersInfoResponse response = slack.methods(API_KEY).usersInfo(r -> r.user(userId));
            return response.getUser().getProfile().toString();
        } catch (Exception e){
            return "";
        }
    }

    private static TimeInfo getTimeInfo(String text) {
        Pattern PATTERN = Pattern.compile("^([^:]+):(\\d+)h(?::bugfix)?$");
        Matcher matcher = PATTERN.matcher(text);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid message format " + text);
        }

        String name = matcher.group(1);
        Double hours = Double.parseDouble(matcher.group(2));
        boolean bugfix = text.endsWith(":bugfix");
        return new TimeInfo(name, 0., hours, bugfix);
    }

    private static boolean validateTs(String ts) {
        long epochMilli = (long) Double.parseDouble(ts) * 1000;
        ZoneId zone = ZoneId.of("Europe/Kyiv");
        LocalDate givenDate = Instant.ofEpochMilli(epochMilli)
                .atZone(zone)
                .toLocalDate();
        LocalDate today = LocalDate.now(zone);
        return givenDate.equals(today);
    }

    private void checkChat(String chatId) {
        if (!CHANNEL.equalsIgnoreCase(chatId)) {
            throw new IllegalArgumentException("Bot doesn't support this channel");
        }
    }
}
