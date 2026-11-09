package org.sopt.lottecinema.health.controller;

import org.sopt.lottecinema.global.common.SuccessResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController implements HealthApi {

    @Override
    @GetMapping("/health")
    public SuccessResponse<String> getHealth() {
        return SuccessResponse.success("UP");
    }
}
