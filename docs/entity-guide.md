# 롯데시네마 JPA 엔티티 가이드

`Seat`를 직접 작성하면서 클래스, 컬럼, 외래 키를 연결해 본다. 이어서 `Showtime`과 `ReservationSeat`에서 열거형, 지연 로딩, 중복 예매 방지를 읽는다. 모든 엔티티는 `org.sopt.lottecinema.<도메인>.entity`에 있다.

## 1. 클래스와 기본 키 만들기

엔티티는 DB에 저장할 데이터를 표현하는 Java 클래스다. `Seat` 객체 하나는 `seat` 테이블의 한 행에 대응한다. `@`로 시작하는 **어노테이션**은 클래스나 필드를 어떻게 처리할지 알려주는 표시다. `jakarta.persistence`는 객체와 테이블을 연결하는 JPA 표준을 제공하고, Hibernate가 매핑에 따라 SQL을 실행한다. [Jakarta Persistence 3.2](https://jakarta.ee/specifications/persistence/3.2/jakarta-persistence-spec-3.2.html)

```java
@Entity
@Table(name = "seat")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
}
```

- `@Entity`는 JPA가 관리할 클래스를 표시하고, `@Table`은 연결할 DB 테이블을 지정한다. `@Id`를 필드에 붙이면 JPA가 필드에 직접 접근하는 방식을 사용한다.
- `@Id`는 각 행을 식별하는 기본 키다. `IDENTITY`를 쓰면 MySQL이 `AUTO_INCREMENT` 컬럼에 값을 부여한다. 새 객체는 `id`가 `null`이고, Hibernate가 INSERT 후 생성된 키를 채운다.
- Lombok의 `@Getter`는 `getId()` 같은 조회 메서드를 만든다. `@NoArgsConstructor(access = AccessLevel.PROTECTED)`는 Hibernate가 DB 행을 객체로 만들 때 쓸 `protected Seat()`를 생성한다. Hibernate는 이 생성자로 객체를 만든 뒤 `@Id`가 붙은 위치를 기준으로 필드에 직접 값을 채운다. [Lombok 생성자 문서](https://projectlombok.org/features/constructor)

## 2. 일반 컬럼과 좌석 시딩

```java
@Column(name = "seat_code", nullable = false, length = 10)
private String seatCode;
```

`@Column`의 `name`은 DB 컬럼명이다. `Movie.title`처럼 생략하면 필드 이름을 기준으로 정한다. `nullable = false`와 `length = 10`은 스키마에서 `VARCHAR(10) NOT NULL`을 표현한다. 좌석 번호 `A1`은 `seatCode`에 담겨 `seat_code`에 저장된다. 빈 문자열을 허용할지 같은 입력 규칙은 저장 전에 별도로 검사한다.

다음 표를 참고해 SQL 타입과 `NULL` 허용 여부를 보고 Java 타입을 고른다.

| SQL과 Java 타입 | 프로젝트 예시 |
| --- | --- |
| `BIGINT` → `Long` / SMALLINT` → `short` 또는 `Short` | ID는 저장 전 `null`일 수 있다. 인기 순위는 값이 없을 수 있어 `Short`, 러닝타임은 필수라 `short`를 쓴다. |
| `TINYINT` → `byte`, `INT` → `int` | 관람 유형별 인원수와 총액에 사용한다. `byte`, `short`, `int`는 `null`을 담을 수 없다. |
| `DECIMAL(3,1)` → `BigDecimal` | 평점은 `new BigDecimal("9.2")`처럼 소수를 정확히 표현한다. `precision = 3`은 전체 자릿수, `scale = 1`은 소수 자릿수다. 0~10점 범위는 `CHECK`로 따로 지정한다. |
| `DATE` → `LocalDate`, `DATETIME` → `LocalDateTime` | 개봉일에는 날짜만 저장한다. 상영·예매 시각에는 날짜와 시각을 담고, 프로젝트에서 KST로 해석한다. `LocalDateTime` 자체에는 시간대가 없다. |
| `CHAR(36)` → `UUID` | `requestId`는 동일한 예매 요청의 재시도를 식별한다. Hibernate가 UUID를 36자 문자열로 읽고 쓴다. |

- **타입 지정** JDBC는 Java가 DB와 통신하는 인터페이스다. `@JdbcTypeCode`는 Hibernate가 값을 읽고 쓸 때 사용할 JDBC 타입을 지정한다. 문자열 enum에는 `VARCHAR`, 예매 요청 UUID에는 `CHAR`, 상영·예매 시각에는 `LOCAL_DATE_TIME`을 쓴다. `@Column(columnDefinition = "DATETIME")`의 `columnDefinition`은 테이블을 만들 때 쓸 SQL 컬럼 타입을 명시한다. [Hibernate JDBC 타입 문서](https://docs.hibernate.org/orm/7.4/javadocs/org/hibernate/annotations/JdbcTypeCode.html)
- **DB 기본값** `@ColumnDefault("0")`은 인원수나 표시 순서 컬럼에 `DEFAULT 0`을 선언한다. DB는 INSERT에서 그 컬럼이 빠진 경우 기본값을 쓴다. 현재 생성자는 넘겨받은 값을 필드에 대입한다. [Hibernate 컬럼 기본값 문서](https://docs.hibernate.org/orm/7.4/userguide/html_single/)

물리 좌석은 초기 데이터를 채우는 과정인 시딩에서 SQL로 등록한다. 다음 예제는 `screen.id = 3`인 상영관에 A1을 추가한다. DB는 `id`를 자동 생성한다.

```sql
INSERT INTO seat (screen_id, seat_code) VALUES (3, 'A1');
```

Java에서는 시딩한 행을 조회해 `Seat`를 얻고, 예매를 확정할 때 이 객체를 `ReservationSeat` 생성자에 전달한다. `private` 필드는 `getSeatCode()`나 `getScreen()` 같은 공개 메서드로 읽는다.

## 3. 외래 키로 좌석과 상영관 연결하기

여러 `Seat`가 같은 `Screen`에 속한다. Java의 `Seat.screen`은 상영관 객체를 가리키고, DB의 `seat.screen_id`는 그 상영관의 기본 키를 저장한다. 외래 키를 가진 쪽인 `Seat`에 매핑을 작성한다.

```java
@ManyToOne(fetch = FetchType.LAZY, optional = false)
@JoinColumn(name = "screen_id", nullable = false, foreignKey = @ForeignKey(name = "fk_seat_1"))
private Screen screen;
```

| 코드 | 무엇을 정하는가 |
| --- | --- |
| `@ManyToOne` | 여러 좌석이 하나의 상영관을 참조하는 객체 관계다. `optional = false`는 JPA 매핑에서 상영관이 필수라는 뜻이다. |
| `@JoinColumn(name = "screen_id")` | 현재 테이블에서 외래 키로 쓸 컬럼명이다. 대상은 필드 타입 `Screen`이며, `referencedColumnName`을 생략하면 대상 테이블의 기본 키인 `screen.id`를 참조한다. `nullable = false`는 외래 키 컬럼에 `NOT NULL`을 선언한다. |
| `foreignKey = @ForeignKey(name = "fk_seat_1")` | 스키마 생성 시 DB에 선언할 외래 키 제약의 이름이다. 기존 SQL에 맞출 때는 그 제약 이름을 사용한다. `name`을 생략하면 Hibernate가 이름을 생성한다. |

아래는 DB 구조를 정의하는 SQL인 DDL의 예다. `screen.id = 3`이 존재하면 `seat.screen_id = 3`으로 좌석을 저장할 수 있다. 제약이 적용된 DB는 존재하지 않는 상영관 ID를 거부한다. [JPA 외래 키 설정](https://jakarta.ee/specifications/persistence/3.2/jakarta-persistence-spec-3.2.html)

```sql
ALTER TABLE seat ADD CONSTRAINT fk_seat_1
    FOREIGN KEY (screen_id) REFERENCES screen (id);
```

프로젝트는 `ddl-auto: update`로 스키마 갱신을 시도한다. 기존 DB에서 외래 키가 실제로 적용됐는지는 `SHOW CREATE TABLE seat;`의 `CONSTRAINT` 정의를 확인한다. [application.yml](../src/main/resources/application.yml) · [Spring Boot JPA 설정](https://docs.spring.io/spring-boot/reference/data/sql.html)

## 4. `FetchType.LAZY`는 언제 SQL을 실행하는가

`@ManyToOne`의 기본값인 `EAGER`는 연관 객체를 즉시 로딩하도록 요구한다. 이 프로젝트에서는 좌석 번호만 필요한 조회도 있으므로 `LAZY`로 상영관 로딩을 미룬다.

**전제** 좌석 `15`의 `screen_id`는 `3`이고, 새 영속성 컨텍스트에서 단건 조회한다. 영속성 컨텍스트는 Hibernate가 조회하거나 저장한 엔티티를 관리하는 범위다. 아래 SQL은 흐름을 보여주기 위해 단순화했다.

```java
Seat seat = entityManager.find(Seat.class, 15L); // seat 행 조회
String code = seat.getSeatCode();               // 이미 읽은 좌석 번호 사용
String name = seat.getScreen().getName();       // 상영관 이름이 필요한 시점
```

1. `find()`는 `SELECT id, screen_id, seat_code FROM seat WHERE id = 15`처럼 좌석 행을 조회한다. `screen_id = 3`으로 Hibernate는 상영관 ID를 아는 대리 객체인 **프록시**를 둘 수 있다.
2. `getSeatCode()`는 조회한 `Seat`의 값으로 응답한다. `getScreen()`은 프록시를 반환할 수 있다.
3. `getName()`에서 상영관의 실제 값이 필요해지면 Hibernate가 `SELECT ... FROM screen WHERE id = 3`을 실행해 프록시를 채운다. 상영관을 이미 로딩했거나 쿼리에서 함께 조회한 경우에는 가져온 값을 재사용한다. [Hibernate 지연 로딩 설명](https://docs.hibernate.org/orm/7.4/introduction/html_single/)

- **트랜잭션 밖에서 접근** 초기화되지 않은 프록시를 영속성 컨텍스트가 닫힌 뒤 사용하면 `LazyInitializationException`이 발생할 수 있다. 현재 설정은 `open-in-view: false`이므로, 화면 응답에 필요한 값은 서비스의 `@Transactional(readOnly = true)` 메서드 안에서 읽고 응답 전용 객체(DTO)에 담는다. [Spring Boot Open EntityManager in View](https://docs.spring.io/spring-boot/reference/data/sql.html)
- **목록에서 반복 접근** 좌석 목록을 한 번 조회한 뒤 각 좌석의 상영관을 따로 읽으면 `N+1` 쿼리가 생길 수 있다. 상영관이 함께 필요한 조회는 `select s from Seat s join fetch s.screen`처럼 JPQL에서 가져올 관계를 명시할 수 있다. JPQL은 테이블명 대신 엔티티명 `Seat`, 컬럼명 대신 필드명 `screen`을 사용한다. [Hibernate 연관관계 조회](https://docs.hibernate.org/orm/7.4/introduction/html_single/)

## 5. enum과 DB 문자열 연결하기

`enum`은 정해진 값만 담는 타입이다. `@Enumerated(EnumType.STRING)`으로 매핑한 `SeatType.STANDARD`는 DB에 `STANDARD`로 저장된다. 상영 방식은 Java에서 `TWO_D`라고 쓰고 DB에는 `2D`를 저장하도록 별도 코드를 지정했다. [FormatType.java](../src/main/java/org/sopt/lottecinema/showtime/entity/FormatType.java)

```java
public enum FormatType {
    TWO_D("2D"),
    THREE_D("3D"),
    FOUR_DX("4DX");

    @EnumeratedValue
    private final String code;

    FormatType(String code) {
        this.code = code;
    }
}
```

`TWO_D("2D")`의 문자열은 enum 생성자에 전달된다. `final` 필드는 초기화 후 다시 대입할 수 없다. JPA는 `@EnumeratedValue`가 붙은 `code`를 사용하므로 `FormatType.TWO_D`와 DB의 `"2D"`를 서로 변환한다. `Movie.curationType`처럼 분류가 선택 사항인 필드는 `null`로 둘 수 있다. [JPA enum 값 매핑](https://jakarta.ee/specifications/platform/11/apidocs/jakarta/persistence/enumeratedvalue)

## 6. 테이블 제약과 예매 흐름 읽기

| 엔티티 | 맡은 역할과 관계 |
| --- | --- |
| [Region](../src/main/java/org/sopt/lottecinema/region/entity/Region.java), [Cinema](../src/main/java/org/sopt/lottecinema/cinema/entity/Cinema.java) | 지역을 정의하고, 각 영화관이 소속 지역을 참조한다. |
| [Screen](../src/main/java/org/sopt/lottecinema/screen/entity/Screen.java), [Seat](../src/main/java/org/sopt/lottecinema/seat/entity/Seat.java) | 상영관은 영화관을, 물리 좌석은 상영관을 참조한다. |
| [Movie](../src/main/java/org/sopt/lottecinema/movie/entity/Movie.java), [Showtime](../src/main/java/org/sopt/lottecinema/showtime/entity/Showtime.java) | 영화 정보를 저장한다. 회차는 영화와 상영관을 참조하며 시작·종료 시각을 담는다. |
| [Reservation](../src/main/java/org/sopt/lottecinema/reservation/entity/Reservation.java), [ReservationSeat](../src/main/java/org/sopt/lottecinema/reservation/entity/ReservationSeat.java) | 예매는 회차·인원·총액을 저장한다. 확정 좌석은 예매·회차·물리 좌석을 연결한다. |

`@Table` 안에 제약을 중첩해서 선언한다. `columnNames = {"showtime_id", "seat_id"}`의 중괄호는 컬럼명 배열이며, DB는 두 컬럼의 조합을 검사한다.

```java
@Table(
        name = "reservation_seat",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_reservation_seat_showtime",
                columnNames = {"showtime_id", "seat_id"}
        )
)
```

- **`@UniqueConstraint`** 같은 회차와 물리 좌석 조합을 한 번만 저장할 수 있다. 1회차 A1에 확정 기록이 있으면 예약된 상태이고, 2회차 A1은 별도로 예약할 수 있다. `Reservation`의 `request_id`도 유니크라 같은 요청 ID로 두 예매 행을 저장할 수 없다.
- **`@CheckConstraint`** SQL 조건을 DB에 선언한다. 예를 들어 `end_at > start_at`은 회차의 시작·종료 순서를, 인원수 조건은 유형별 0~8명과 합계 1~8명을 검사한다.
- **`@Index`** `columnList = "movie_id, start_at"`처럼 DB의 검색에 사용할 인덱스 컬럼과 순서를 정한다. 같은 상영관·시작 시각의 회차 중복은 별도의 유니크 제약이 막는다.

`new ReservationSeat(reservation, seat)`는 기존 좌석을 참조하는 새 확정 좌석 객체를 만든다. 생성자에서 `this.seat = seat`의 왼쪽은 현재 객체의 필드, 오른쪽은 전달받은 값이다. 회차는 `reservation.getShowtime()`에서 가져온다. 좌석이 그 회차의 상영관에 속하는지와 좌석 수가 총인원과 일치하는지는 예매 처리 코드에서 확인할 규칙이다. 예매와 확정 좌석은 같은 트랜잭션에 저장해야 좌석 충돌이 발생했을 때 함께 롤백할 수 있다.

## 7. 직접 작성하는 순서

1. **ERD에서 테이블을 고른다.** 기본 키와 일반 컬럼의 타입, 길이, `NULL` 허용 여부를 표시한다.
2. **Java 클래스로 옮긴다.** 패키지와 클래스, `@Entity`, `@Table`, `@Id`를 먼저 작성한다. 일반 컬럼의 Java 타입과 `@Column` 옵션을 맞춘다.
3. **외래 키를 객체 관계로 옮긴다.** 예를 들어 `screen_id`가 있는 `Seat`에 `Screen screen` 필드와 `@ManyToOne`, `@JoinColumn`을 작성한다. 제약 이름은 ERD의 DDL에서 가져온다.
4. **생성 규칙과 DB 제약을 추가한다.** JPA용 기본 생성자를 두고, `ReservationSeat`처럼 Java에서 새로 만드는 엔티티에는 공개 생성자를 추가한다. `Seat`는 같은 상영관에서 좌석 번호가 겹치지 않도록 `@UniqueConstraint`에 `{"screen_id", "seat_code"}`를 넣는다.
5. **시딩과 조회를 확인한다.** SQL로 A1을 추가하고 Java에서 ID로 조회한다. 같은 상영관에 A1을 다시 넣으면 유니크 제약이 거부한다. 새 트랜잭션에서 `getSeatCode()`와 `getScreen().getName()`을 각각 호출해 SQL 로그를 비교한다.
