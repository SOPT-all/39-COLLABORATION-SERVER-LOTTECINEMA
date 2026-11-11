package org.sopt.lottecinema.showtime.entity;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 회차별 네 관람 유형의 1인 단가. 좌석 조회와 예매 금액 계산이 같은 가격을 사용한다. */
@Entity
@Table(
        name = "showtime_ticket_price",
        uniqueConstraints = @UniqueConstraint(name = "uk_ticket_price_showtime", columnNames = "showtime_id"),
        check = @CheckConstraint(name = "ck_ticket_price_amounts", constraint =
                "adult_price between 0 and 268435455 and teen_price between 0 and 268435455"
                        + " and senior_price between 0 and 268435455 and disabled_price between 0 and 268435455")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ShowtimeTicketPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "showtime_id", nullable = false, foreignKey = @ForeignKey(name = "fk_ticket_price_showtime"))
    private Showtime showtime;

    @Column(name = "adult_price", nullable = false)
    private int adultPrice;

    @Column(name = "teen_price", nullable = false)
    private int teenPrice;

    @Column(name = "senior_price", nullable = false)
    private int seniorPrice;

    @Column(name = "disabled_price", nullable = false)
    private int disabledPrice;

    public ShowtimeTicketPrice(Showtime showtime, int adultPrice, int teenPrice, int seniorPrice, int disabledPrice) {
        // 최대 8인 합계가 기존 예매 총액의 INT 범위 안에 들도록 유형별 단가를 제한한다.
        for (int price : new int[]{adultPrice, teenPrice, seniorPrice, disabledPrice}) {
            if (price < 0 || price > Integer.MAX_VALUE / 8) {
                throw new IllegalArgumentException("관람 유형별 단가 범위를 확인해 주세요.");
            }
        }
        this.showtime = showtime;
        this.adultPrice = adultPrice;
        this.teenPrice = teenPrice;
        this.seniorPrice = seniorPrice;
        this.disabledPrice = disabledPrice;
    }
}
