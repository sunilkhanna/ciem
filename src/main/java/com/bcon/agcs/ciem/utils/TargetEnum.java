package com.bcon.agcs.ciem.utils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum TargetEnum {
    AWS("AWS"),
    AZURE("AZURE");
    private final String label;

    TargetEnum(String label) {
        this.label = label;
    }

    public static TargetEnum getTargetEnum(String label) {
        return Arrays.stream(TargetEnum.values())
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
