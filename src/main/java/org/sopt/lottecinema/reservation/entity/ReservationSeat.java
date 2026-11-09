package org.sopt.lottecinema.reservation.entity;

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
import org.sopt.lottecinema.seat.entity.Seat;
import org.sopt.lottecinema.showtime.entity.Showtime;

@Entity
@Table(
        name = "reservation_seat",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_reservation_seat_showtime", columnNames = {"showtime_id", "seat_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReservationSeat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id", nullable = false, foreignKey = @ForeignKey(name = "fk_reservation_seat_1"))
    private Reservation reservation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "showtime_id", nullable = false, foreignKey = @ForeignKey(name = "fk_reservation_seat_2"))
    private Showtime showtime;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seat_id", nullable = false, foreignKey = @ForeignKey(name = "fk_reservation_seat_3"))
    private Seat seat;

    public ReservationSeat(Reservation reservation, Seat seat) {
        this.reservation = reservation;
        this.showtime = reservation.getShowtime();
        this.seat = seat;
    }
}
