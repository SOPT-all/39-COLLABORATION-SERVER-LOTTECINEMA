package org.sopt.lottecinema.global.common;

import com.fasterxml.jackson.annotation.JsonInclude;

public record ValidationError(@JsonInclude(JsonInclude.Include.NON_NULL) String field, String message) {
}
