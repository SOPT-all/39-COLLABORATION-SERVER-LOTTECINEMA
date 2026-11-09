package org.sopt.lottecinema.showtime.entity;

import jakarta.persistence.EnumeratedValue;

public enum FormatType {
    TWO_D("2D"),
    THREE_D("3D"),
    FOUR_DX("4DX");

    @EnumeratedValue
    private final String code;

    FormatType(String code) {
        this.code = code;
    }
}
