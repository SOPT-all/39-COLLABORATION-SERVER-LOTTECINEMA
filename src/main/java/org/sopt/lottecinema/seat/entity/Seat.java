package org.sopt.lottecinema.seat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.sopt.lottecinema.screen.entity.Screen;

@Entity
@Table(
        name = "seat",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_seat_screen_code", columnNames = {"screen_id", "seat_code"}),
                @UniqueConstraint(name = "uk_seat_screen_position",
                        columnNames = {"screen_id", "row_index", "column_index"})
        },
        check = @CheckConstraint(name = "ck_seat_position", constraint = "row_index >= 0 and column_index >= 0")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "screen_id", nullable = false, foreignKey = @ForeignKey(name = "fk_seat_1"))
    private Screen screen;

    @Column(name = "seat_code", nullable = false, length = 10)
    private String seatCode;

    // 기존 좌석은 실제 배치 자료로 채울 때까지 null을 허용한다. 미완성 배치는 조회 서비스에서 거부한다.
    @Column(name = "row_index")
    private Integer rowIndex;

    @Column(name = "column_index")
    private Integer columnIndex;

    // null은 구분 자료가 없는 상태이며, 확인된 일반 좌석(false)과 구별한다.
    @Column(name = "is_accessible")
    private Boolean accessible;

    public Seat(Screen screen, String seatCode, int rowIndex, int columnIndex, Boolean accessible) {
        if (seatCode == null || seatCode.isBlank() || seatCode.length() > 10 || rowIndex < 0 || columnIndex < 0) {
            throw new IllegalArgumentException("좌석 번호와 행·열 위치를 확인해 주세요.");
        }
        this.screen = screen;
        this.seatCode = seatCode;
        this.rowIndex = rowIndex;
        this.columnIndex = columnIndex;
        this.accessible = accessible;
    }
}
