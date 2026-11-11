package org.sopt.lottecinema.showtime.service;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sopt.lottecinema.global.exception.BusinessException;
import org.sopt.lottecinema.global.exception.ErrorCode;
import org.sopt.lottecinema.showtime.dto.TicketPrices;
import org.sopt.lottecinema.showtime.entity.ShowtimeTicketPrice;
import org.sopt.lottecinema.showtime.repository.ShowtimeTicketPriceRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShowtimeTicketPriceReaderTest {

    @Mock private ShowtimeTicketPriceRepository repository;
    @InjectMocks private ShowtimeTicketPriceReader reader;

    @Test
    void distinguishesConfiguredZeroPriceFromMissingPrice() {
        when(repository.findByShowtimeId(1L)).thenReturn(Optional.of(new ShowtimeTicketPrice(null, 0, 0, 0, 0)));
        assertThat(reader.getTicketPrices(1L)).isEqualTo(new TicketPrices(0, 0, 0, 0));
    }

    @Test
    void rejectsMissingPriceAsServerError() {
        when(repository.findByShowtimeId(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> reader.getTicketPrices(1L)).isInstanceOfSatisfying(BusinessException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR));
    }
}
