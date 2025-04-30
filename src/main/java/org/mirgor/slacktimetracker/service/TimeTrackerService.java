package org.mirgor.slacktimetracker.service;

import com.slack.api.methods.response.conversations.ConversationsHistoryResponse;
import com.slack.api.model.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mirgor.slacktimetracker.service.dto.TimeInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.mirgor.slacktimetracker.service.dto.Constants.DEVELOPER_MAP;
import static org.mirgor.slacktimetracker.service.dto.Constants.FE;

@Slf4j
@Service
@RequiredArgsConstructor
public class TimeTrackerService {

    @Value("${google.sheet.id}")
    private String SPREADSHEET_ID;

    private final SheetsService sheetsService;
    private final SlackService slackService;


    @Scheduled(cron = "0 0 13 * * ?")
    public void sendTrackerMsg() {
        log.info("Sending tracker msg");
        slackService.sendMessage(":wave: Time to track development time");
    }

    @Scheduled(cron = "0 0 17 * * ?")
    public void getTime() {
        log.info("Getting time for development");
        try {
            ConversationsHistoryResponse conversationsHistoryResponse = slackService.readChannel();
            List<TimeInfo> list = conversationsHistoryResponse.getMessages().stream()
                    .filter(message -> validateTs(message.getTs()))
                    .flatMap(msg -> buildTimeInfo(msg).stream())
                    .toList();

            sheetsService.addTime(SPREADSHEET_ID, list);
        } catch (Exception e) {
            log.error("Error while reading messages from Slack. @error={}", e.getMessage());
        }
    }

    private List<TimeInfo> buildTimeInfo(Message message) {
        return Arrays.stream(message.getText().split("\n"))
                .map(text -> {
                    Pattern PATTERN = Pattern.compile("^([^:]+):([0-9]+(?:\\.[0-9]+)?)h(?::bugfix)?$");
                    Matcher matcher = PATTERN.matcher(text);
                    if (!matcher.matches()) {
                        log.info("Invalid message format {}", text);
                        return null;
                    }
                    return buildTimeInfoBasedOnUserRole(message, text, matcher);
                })
                .filter(Objects::nonNull)
                .toList();

    }

    private TimeInfo buildTimeInfoBasedOnUserRole(Message message, String text, Matcher matcher) {
        String name = matcher.group(1);
        Double hours = Double.parseDouble(matcher.group(2));
        boolean bugfix = text.endsWith(":bugfix");
        String userName = slackService.getUserName(message.getUser());
        log.info(userName);
        String role = DEVELOPER_MAP.get(userName);
        if (FE.equals(role)) {
            return new TimeInfo(name.toLowerCase(), hours, 0., bugfix);
        }
        return new TimeInfo(name.toLowerCase(), 0., hours, bugfix);
    }

    private boolean validateTs(String ts) {
        long epochMilli = (long) Double.parseDouble(ts) * 1000;
        ZoneId zone = ZoneId.systemDefault();
        LocalDate givenDate = Instant.ofEpochMilli(epochMilli)
                .atZone(zone)
                .toLocalDate();
        LocalDate today = LocalDate.now(zone);
        return givenDate.equals(today);
    }


}
