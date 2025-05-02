package org.mirgor.slacktimetracker.service.dto;

import lombok.Getter;

@Getter
public enum Headers {
    PROJECT("project"),
    FE("FE"),
    BE("BE"),
    FE_FIX("FE FIX"),
    BE_FIX("BE FIX"),
    TOTAL_DEV("TOTAL DEV"),
    TOTAL_FIX("TOTAL FIX"),
    ;

    Headers(String header) {
        this.header = header;
    }

    private final String header;
}
