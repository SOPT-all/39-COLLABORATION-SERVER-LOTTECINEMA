package org.sopt.lottecinema.movie.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class MovieListDateProviderTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-11-13T15:00:00Z"), ZoneOffset.UTC);
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withBean(Clock.class, () -> clock)
            .withUserConfiguration(MovieListDateProvider.class);

    @Test
    void returnsKoreanDateWhenReferenceDateIsMissing() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(MovieListDateProvider.class).getReferenceDate())
                    .isEqualTo(LocalDate.of(2026, 11, 14));
        });
    }

    @Test
    void usesCurrentDateWithDefaultApplicationConfiguration() {
        runner.withInitializer(new ConfigDataApplicationContextInitializer())
                .withPropertyValues("MOVIE_LIST_REFERENCE_DATE=")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(MovieListDateProvider.class).getReferenceDate())
                            .isEqualTo(LocalDate.of(2026, 11, 14));
                });
    }

    @Test
    void usesDemoDateWithoutChangingSharedClock() {
        runner.withInitializer(new ConfigDataApplicationContextInitializer())
                .withPropertyValues("spring.profiles.active=demo")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(MovieListDateProvider.class).getReferenceDate())
                            .isEqualTo(LocalDate.of(2026, 10, 27));
                    assertThat(context.getBeansOfType(Clock.class)).hasSize(1);
                    assertThat(context.getBean(Clock.class)).isSameAs(clock);
                    assertThat(context.getBean(Clock.class).instant())
                            .isEqualTo(Instant.parse("2026-11-13T15:00:00Z"));
                });
    }

    @Test
    void allowsOverridingDemoDate() {
        runner.withInitializer(new ConfigDataApplicationContextInitializer())
                .withPropertyValues("spring.profiles.active=demo", "MOVIE_LIST_REFERENCE_DATE=2026-10-30")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(MovieListDateProvider.class).getReferenceDate())
                            .isEqualTo(LocalDate.of(2026, 10, 30));
                });
    }

    @Test
    void rejectsInvalidDateAtStartup() {
        runner.withPropertyValues("movie.list.reference-date=2026-02-30").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(DateTimeException.class);
        });
    }

    @Test
    void updatesCurrentDateAcrossKoreanMidnight() {
        Clock movingClock = mock(Clock.class);
        ZoneId zone = ZoneId.of("Asia/Seoul");
        when(movingClock.withZone(zone)).thenReturn(movingClock);
        when(movingClock.getZone()).thenReturn(zone);
        when(movingClock.instant()).thenReturn(
                Instant.parse("2026-11-13T14:59:59Z"),
                Instant.parse("2026-11-13T15:00:00Z")
        );
        MovieListDateProvider provider = new MovieListDateProvider(movingClock, "");

        assertThat(provider.getReferenceDate()).isEqualTo(LocalDate.of(2026, 11, 13));
        assertThat(provider.getReferenceDate()).isEqualTo(LocalDate.of(2026, 11, 14));
    }
}
