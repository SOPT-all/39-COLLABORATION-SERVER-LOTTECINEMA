package org.sopt.lottecinema.global.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.sopt.lottecinema.global.exception.GlobalExceptionHandler;
import org.sopt.lottecinema.health.controller.HealthController;
import org.sopt.lottecinema.movie.controller.MovieController;
import org.sopt.lottecinema.movie.controller.MovieShowtimeController;
import org.sopt.lottecinema.movie.service.MovieService;
import org.sopt.lottecinema.movie.service.MovieShowtimeService;
import org.sopt.lottecinema.reservation.controller.ReservationController;
import org.sopt.lottecinema.reservation.service.ReservationService;
import org.sopt.lottecinema.showtime.controller.AdminShowtimeController;
import org.sopt.lottecinema.showtime.controller.ShowtimeController;
import org.sopt.lottecinema.showtime.service.AdminShowtimeService;
import org.sopt.lottecinema.showtime.service.ShowtimeSeatService;
import org.sopt.lottecinema.showtime.service.ShowtimeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(classes = OpenApiContractTest.TestConfig.class,
        properties = "ADMIN_API_TOKEN=0000000000000000000000000000000000000000000000000000000000000000")
@ActiveProfiles("admin-api")
class OpenApiContractTest {

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
    @Import({SwaggerConfig.class, AdminApiConfig.class, GlobalExceptionHandler.class, HealthController.class,
            MovieController.class, MovieShowtimeController.class, ShowtimeController.class,
            ReservationController.class, AdminShowtimeController.class})
    static class TestConfig {
    }

    // 명세 생성에는 DB가 필요 없으므로 실제 컨트롤러와 springdoc만 띄우고 서비스를 대체합니다.
    @MockitoBean MovieService movieService;
    @MockitoBean MovieShowtimeService movieShowtimeService;
    @MockitoBean ShowtimeService showtimeService;
    @MockitoBean ShowtimeSeatService showtimeSeatService;
    @MockitoBean ReservationService reservationService;
    @MockitoBean AdminShowtimeService adminShowtimeService;

    @Autowired WebApplicationContext context;
    @Autowired JsonMapper jsonMapper;

    private JsonNode document;
    private JsonNode schemas;

    @BeforeEach
    void loadDocument() throws Exception {
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).build();
        String json = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        document = jsonMapper.readTree(json);
        schemas = document.path("components").path("schemas");
        // 프론트 생성기로도 같은 명세를 확인할 수 있도록 빌드 산출물에 저장합니다.
        Files.createDirectories(Path.of("build/openapi"));
        Files.writeString(Path.of("build/openapi/openapi.json"), json);
    }

    @ParameterizedTest
    @CsvSource({
            "/health, get, 200, getHealth, string",
            "/api/v1/movies, get, 200, findMovies, MovieListsResponse",
            "/api/v1/movies/{movieId}/cinemas, get, 200, getMovieCinemas, MovieCinemaResponse",
            "/api/v1/showtimes, get, 200, findShowtimes, ShowtimeListResponse",
            "/api/v1/showtimes/{showtimeId}/seats, get, 200, getShowtimeSeats, ShowtimeSeatResponse",
            "/api/v1/showtimes/{showtimeId}/reservations, post, 200, createReservation, ReservationResponse",
            "/api/v1/showtimes/{showtimeId}/reservations, post, 201, createReservation, ReservationResponse",
            "/api/v1/admin/showtimes, post, 201, createShowtime, CreateShowtimeResponse"
    })
    void preservesSuccessDataType(String path, String method, String status, String operationId, String dataType) {
        JsonNode operation = document.path("paths").path(path).path(method);
        assertThat(operation.path("operationId").asString()).isEqualTo(operationId);
        JsonNode response = resolve(operation.path("responses").path(status).path("content")
                .path("application/json").path("schema"));
        assertThat(strings(response.path("required"))).contains("status", "message", "data");
        JsonNode data = response.path("properties").path("data");
        if (dataType.equals("string")) {
            assertThat(data.path("type").asString()).isEqualTo("string");
        } else {
            assertThat(data.path("$ref").asString()).isEqualTo("#/components/schemas/" + dataType);
        }
    }

    @Test
    void documentsErrorBodiesAndBodylessResponses() {
        document.path("paths").forEach(path -> path.forEach(operation ->
                operation.path("responses").properties().forEach(response -> {
                    if (Integer.parseInt(response.getKey()) < 400) {
                        return;
                    }
                    JsonNode content = response.getValue().path("content");
                    if (response.getKey().equals("406")) {
                        assertThat(content.isMissingNode() || content.isEmpty()).isTrue();
                    } else {
                        assertThat(content.path("application/json").path("schema").path("$ref").asString())
                                .isEqualTo("#/components/schemas/ErrorResponse");
                    }
                })));
        assertThat(strings(schemas.path("ErrorResponse").path("required")))
                .containsExactlyInAnyOrder("status", "code", "message");
        assertThat(strings(schemas.path("ValidationError").path("required")))
                .containsExactly("message");
    }

    @Test
    void distinguishesRequiredNullableFieldsFromOmittedFields() {
        assertThat(document.path("openapi").asString()).startsWith("3.0.");
        JsonNode movie = schemas.path("MovieListResponse");
        assertThat(strings(movie.path("required"))).contains("posterUrl", "curationType", "movieRating", "dDay");
        for (String field : Set.of("posterUrl", "curationType", "curationOrder", "popularityRank", "bookingRate", "movieRating", "dDay")) {
            assertNullable(movie.path("properties").path(field));
        }
        JsonNode info = schemas.path("MovieInfo");
        assertThat(strings(info.path("required"))).contains("posterUrl");
        assertNullable(info.path("properties").path("posterUrl"));
    }

    @Test
    void documentsDatesEnumsAndSerializedSeatName() {
        JsonNode seatResponse = schemas.path("ShowtimeSeatResponse").path("properties");
        assertThat(seatResponse.path("startAt").path("format").asString()).isEqualTo("date-time");
        assertThat(seatResponse.path("endAt").path("format").asString()).isEqualTo("date-time");
        assertThat(strings(seatResponse.path("formatType").path("enum"))).containsExactlyInAnyOrder("2D", "3D", "4DX");
        assertThat(schemas.path("ReservationResponse").path("properties").path("createdAt").path("format").asString())
                .isEqualTo("date-time");
        assertThat(strings(schemas.path("SeatInfo").path("required"))).contains("isReserved");
        assertThat(schemas.path("SeatInfo").path("properties").has("reserved")).isFalse();
    }

    private JsonNode resolve(JsonNode schema) {
        return schema.has("$ref") ? schemas.path(schema.path("$ref").asString().substring("#/components/schemas/".length())) : schema;
    }

    private void assertNullable(JsonNode schema) {
        assertThat(schema.path("nullable").asBoolean())
                .as("null 허용 스키마: %s", schema).isTrue();
    }

    private Set<String> strings(JsonNode array) {
        Set<String> values = new HashSet<>();
        array.forEach(value -> values.add(value.asString()));
        return values;
    }
}
