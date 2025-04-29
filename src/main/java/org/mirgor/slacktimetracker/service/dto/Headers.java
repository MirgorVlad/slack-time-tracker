package org.mirgor.slacktimetracker.service.dto;

import lombok.Getter;

@Getter
public enum Headers {
    PROJECT("project"),
    FE("FE"),
    BE("BE"),
    FE_FIX("FE FIX"),
    BE_FIX("BE FIX"),
    ;

    Headers(String header) {
        this.header = header;
    }

    private final String header;
}
