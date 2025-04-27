package org.mirgor.slacktimetracker.service;

public record TimeInfo(
        String project,
        Double fe,
        Double be,
        boolean bugfix) {
}
