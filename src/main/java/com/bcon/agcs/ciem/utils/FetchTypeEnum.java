package com.bcon.agcs.ciem.utils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum FetchTypeEnum {
    USER("USER"),
    ROLE("ROLE"),
    APP_ROLE("APP_ROLE"),
    GROUP("GROUP"),
    POLICY("POLICY");
    private final String label;

    FetchTypeEnum(String label) {
        this.label = label;
    }

    public static FetchTypeEnum getFetchTypeEnum(String label) {
        return Arrays.stream(FetchTypeEnum.values())
                .filter(targetEnum -> targetEnum.label.equals(label))
                .findFirst()
                .orElse(null);
    }

    public String getTargetEnumName() {
        return label;
    }

    public static List<String> getTargetEnumNameSet() {
        return Arrays.stream(TargetEnum.values())
                .map(TargetEnum::getTargetEnumName)
                .collect(Collectors.toList());
    }
}
