package org.sopt.lottecinema.reservation.entity;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
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
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.sopt.lottecinema.showtime.entity.Showtime;

@Entity
@Table(
        name = "reservation",
        uniqueConstraints = @UniqueConstraint(name = "uk_reservation_request", columnNames = "request_id"),
        check = {
                @CheckConstraint(
                        name = "ck_reservation_counts",
                        constraint = "adult_count between 0 and 8 and teen_count between 0 and 8"
                                + " and senior_count between 0 and 8 and disabled_count between 0 and 8"
                                + " and adult_count + teen_count + senior_count + disabled_count between 1 and 8"
                ),
                @CheckConstraint(name = "ck_reservation_amount", constraint = "total_amount >= 0")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "request_id", nullable = false, length = 36, columnDefinition = "CHAR(36)")
    private UUID requestId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "showtime_id", nullable = false, foreignKey = @ForeignKey(name = "fk_reservation_1"))
    private Showtime showtime;

    @Column(name = "adult_count", nullable = false)
    @ColumnDefault("0")
    private byte adultCount;

    @Column(name = "teen_count", nullable = false)
    @ColumnDefault("0")
    private byte teenCount;

    @Column(name = "senior_count", nullable = false)
    @ColumnDefault("0")
    private byte seniorCount;

    @Column(name = "disabled_count", nullable = false)
    @ColumnDefault("0")
    private byte disabledCount;

    @Column(name = "total_amount", nullable = false)
    private int totalAmount;

    @JdbcTypeCode(SqlTypes.LOCAL_DATE_TIME)
    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME")
    private LocalDateTime createdAt;

    public Reservation(
            UUID requestId,
            Showtime showtime,
            byte adultCount,
            byte teenCount,
            byte seniorCount,
            byte disabledCount,
            int totalAmount,
            LocalDateTime createdAt
    ) {
        this.requestId = requestId;
        this.showtime = showtime;
        this.adultCount = adultCount;
        this.teenCount = teenCount;
        this.seniorCount = seniorCount;
        this.disabledCount = disabledCount;
        this.totalAmount = totalAmount;
        this.createdAt = createdAt;
    }
}
