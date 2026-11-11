package org.sopt.lottecinema.seat.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.sopt.lottecinema.screen.repository.ScreenRepository;
import org.sopt.lottecinema.seat.entity.Seat;
import org.sopt.lottecinema.seat.repository.SeatRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** seed 프로필로 시작할 때 지정한 상영관에 사용자 제공 배치 77석을 등록한다. */
@Slf4j
@Component
@Profile("seed")
public class SeatDataSeeder implements ApplicationRunner {

    private final ScreenRepository screenRepository;
    private final SeatRepository seatRepository;
    private final Long screenId;

    public SeatDataSeeder(ScreenRepository screenRepository, SeatRepository seatRepository,
                          @Value("${seat.seed.screen-id}") Long screenId) {
        this.screenRepository = screenRepository;
        this.seatRepository = seatRepository;
        this.screenId = screenId;
    }

    /** JPA 스키마 생성 후 실행되며, 배치 검증 또는 저장 실패 시 전체 초기화를 롤백한다. */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void run(ApplicationArguments args) throws IOException {
        if (screenId == null || screenId < 1) {
            throw new IllegalArgumentException("SEAT_SEED_SCREEN_ID에 실제 상영관 ID를 지정해 주세요.");
        }
        var layout = readLayout();
        var screen = screenRepository.findByIdForUpdate(screenId)
                .orElseThrow(() -> new IllegalStateException("시딩할 상영관이 없습니다. screenId=" + screenId));
        var existingSeats = seatRepository.findByScreenId(screenId);
        Map<String, SeatPosition> positions = layout.stream()
                .collect(Collectors.toMap(SeatPosition::seatCode, Function.identity()));

        // 모든 기존 좌석을 먼저 확인한다. 다른 배치가 섞여 있으면 변경 전에 시작을 중단한다.
        for (Seat seat : existingSeats) {
            var position = positions.get(seat.getSeatCode());
            if (position == null || (seat.getRowIndex() != null && seat.getRowIndex() != position.rowIndex())
                    || (seat.getColumnIndex() != null && seat.getColumnIndex() != position.columnIndex())) {
                throw new IllegalStateException("기존 좌석과 시드 배치가 다릅니다. screenId=" + screenId
                        + ", seatCode=" + seat.getSeatCode());
            }
        }

        // 기존 행은 ID와 장애인석 표시를 유지하고, 빈 좌표만 JPA 변경 감지로 채운다.
        for (Seat seat : existingSeats) {
            var position = positions.get(seat.getSeatCode());
            seat.initializePosition(position.rowIndex(), position.columnIndex());
        }
        var existingCodes = existingSeats.stream().map(Seat::getSeatCode).collect(Collectors.toSet());
        var newSeats = layout.stream().filter(position -> !existingCodes.contains(position.seatCode()))
                .map(position -> new Seat(screen, position.seatCode(), position.rowIndex(), position.columnIndex(), null))
                .toList();
        // 기존 행의 좌표 갱신을 먼저 반영하고 누락 좌석을 추가한다. 두 작업은 같은 트랜잭션이다.
        seatRepository.flush();
        seatRepository.saveAllAndFlush(newSeats);
        log.info("좌석 시딩 완료. screenId={}, total=77, added={}", screenId, newSeats.size());
    }

    private List<SeatPosition> readLayout() throws IOException {
        var resource = new ClassPathResource("seed/seats-12x15.csv");
        try (var reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            // CSV에는 실제 좌석만 기록한다. 빈 칸과 마지막 빈 행은 화면의 12×15 그리드가 표현한다.
            return reader.lines().skip(1).map(line -> {
                var fields = line.split(",");
                return new SeatPosition(fields[0], Integer.parseInt(fields[1]), Integer.parseInt(fields[2]));
            }).toList();
        }
    }

    private record SeatPosition(String seatCode, int rowIndex, int columnIndex) {
    }
}
