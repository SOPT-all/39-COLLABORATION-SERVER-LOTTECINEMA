package org.sopt.lottecinema.movie.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.sopt.lottecinema.movie.dto.MovieListResponse;
import org.sopt.lottecinema.movie.repository.MovieRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MovieService {

    private final MovieRepository movieRepository;

    public List<MovieListResponse> findMovies() {
        return movieRepository.findAll().stream()
                .map(MovieListResponse::from)
                .toList();
    }
}