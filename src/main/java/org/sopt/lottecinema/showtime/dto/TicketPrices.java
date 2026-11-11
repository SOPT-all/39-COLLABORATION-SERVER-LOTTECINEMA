package org.sopt.lottecinema.showtime.dto;

/** 모든 회차가 공유하는 성인·청소년·경로·장애인 1인 단가이며 단위는 원이다. */
public record TicketPrices(int adult, int teen, int senior, int disabled) {
}
