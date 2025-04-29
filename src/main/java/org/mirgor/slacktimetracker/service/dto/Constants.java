package org.mirgor.slacktimetracker.service.dto;

import java.util.HashMap;
import java.util.Map;

public class Constants {

    public static final String FE = "FE";
    public static final String BE = "BE";
    public static final Map<String, String> DEVELOPER_MAP;

    static {
        DEVELOPER_MAP = new HashMap<>();
        DEVELOPER_MAP.put("Dmytro Pinkevych", FE);
        DEVELOPER_MAP.put("Dmytro Khylko", FE);
        DEVELOPER_MAP.put("Oleksandr Mykytenko", FE);
        DEVELOPER_MAP.put("Tetiana Yankiv", FE);
        DEVELOPER_MAP.put("Valeriia Koriavikova", FE);
        DEVELOPER_MAP.put("Stanislav Sosnov", FE);
        DEVELOPER_MAP.put("Vlad Myrhorodskyi", BE);
        DEVELOPER_MAP.put("Andrey Tkachenko", BE);
        DEVELOPER_MAP.put("Andrii Tykholoz", BE);
        DEVELOPER_MAP.put("Artemii Molyboha", BE);
        DEVELOPER_MAP.put("Nikita Mazurenko", BE);
    }
}
