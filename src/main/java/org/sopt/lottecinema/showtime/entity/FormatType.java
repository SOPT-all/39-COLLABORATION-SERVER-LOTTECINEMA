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

    /** DB와 API에서 사용하는 상영 포맷 표기(2D, 3D, 4DX)를 반환한다. */
    public String getCode() {
        return code;
    }
}
