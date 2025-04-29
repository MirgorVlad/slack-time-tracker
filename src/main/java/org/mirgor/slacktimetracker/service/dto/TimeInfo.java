package org.mirgor.slacktimetracker.service.dto;

public record TimeInfo(
        String project,
        Double fe,
        Double be,
        boolean bugfix) {
}
