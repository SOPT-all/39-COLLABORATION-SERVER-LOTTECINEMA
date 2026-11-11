package org.sopt.lottecinema.seat.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sopt.lottecinema.screen.entity.Screen;
import org.sopt.lottecinema.screen.repository.ScreenRepository;
import org.sopt.lottecinema.seat.entity.Seat;
import org.sopt.lottecinema.seat.repository.SeatRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 서버 시작 시 모든 상영관에 공통 좌석 배치를 등록한다. */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeatDataSeeder implements ApplicationRunner {

    private final ScreenRepository screenRepository;
    private final SeatRepository seatRepository;

    /** JPA 스키마 생성 후 실행되며, 배치 검증 또는 저장 실패 시 전체 초기화를 롤백한다. */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void run(ApplicationArguments args) throws IOException {
        List<SeatPosition> layout = readLayout();
        // 좌석 번호를 키로 사용해 기존 좌석의 배치 정보를 찾는다. 예를 들어 B3은 (1, 3)에 해당한다.
        Map<String, SeatPosition> positions = layout.stream()
                .collect(Collectors.toMap(SeatPosition::seatCode, Function.identity()));

        // 모든 상영관을 조회해 각 상영관에 공통 배치를 적용한다.
        List<Screen> screens = screenRepository.findAllByOrderByIdAsc();
        if (screens.isEmpty()) {
            log.info("좌석을 시딩할 상영관이 없습니다.");
        }
        for (Screen screen : screens) {
            seedScreen(screen, layout, positions);
        }
    }

    private void seedScreen(Screen screen, List<SeatPosition> layout, Map<String, SeatPosition> positions) {
        Long screenId = screen.getId();
        List<Seat> existingSeats = seatRepository.findByScreenId(screenId);

        // 모든 기존 좌석을 먼저 확인한다. 다른 배치가 섞여 있으면 변경 전에 시작을 중단한다.
        for (Seat seat : existingSeats) {
            SeatPosition position = positions.get(seat.getSeatCode());
            if (position == null || (seat.getRowIndex() != null && seat.getRowIndex() != position.rowIndex())
                    || (seat.getColumnIndex() != null && seat.getColumnIndex() != position.columnIndex())) {
                throw new IllegalStateException("기존 좌석과 시드 배치가 다릅니다. screenId=" + screenId
                        + ", seatCode=" + seat.getSeatCode());
            }
        }

        // 기존 행은 ID를 유지하고, 빈 좌표만 JPA 변경 감지로 채운다.
        for (Seat seat : existingSeats) {
            SeatPosition position = positions.get(seat.getSeatCode());
            seat.initializePosition(position.rowIndex(), position.columnIndex());
        }
        Set<String> existingCodes = existingSeats.stream().map(Seat::getSeatCode).collect(Collectors.toSet());
        List<Seat> newSeats = layout.stream().filter(position -> !existingCodes.contains(position.seatCode()))
                .map(position -> new Seat(screen, position.seatCode(), position.rowIndex(), position.columnIndex()))
                .toList();
        // flush는 변경 SQL을 DB로 보낸다. 커밋은 run의 트랜잭션이 정상 종료될 때 이루어진다.
        // 뒤쪽 상영관에서 실패하면 여기서 보낸 변경 SQL도 함께 롤백된다.
        seatRepository.flush();
        seatRepository.saveAllAndFlush(newSeats);
        log.info("좌석 시딩 완료. screenId={}, total={}, added={}", screenId, layout.size(), newSeats.size());
    }

    private List<SeatPosition> readLayout() throws IOException {
        ClassPathResource resource = new ClassPathResource("seed/seats-12x15.csv");
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            // CSV에는 실제 좌석만 기록한다. 빈 칸과 마지막 빈 행은 화면의 12×15 그리드가 표현한다.
            return reader.lines().skip(1).map(line -> {
                String[] fields = line.split(",");
                return new SeatPosition(fields[0], Integer.parseInt(fields[1]), Integer.parseInt(fields[2]));
            }).toList();
        }
    }

    private record SeatPosition(String seatCode, int rowIndex, int columnIndex) {
    }
}
