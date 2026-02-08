package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MoviesStore {
    private final Map<Integer, Movie> movies = new HashMap<>();
    private int id = 1;

    public List<Movie> getAll() {
        return new ArrayList<>(movies.values());
    }

    public Movie addMovie(Movie movie) {
        movie.setId(id);
        movies.put(id, movie);
        id++;
        return movie;
    }

    public Movie getByID(int id) {
        return movies.get(id);
    }

    public void deleteMovie(int id) {
        movies.remove(id);
    }

    public void deleteAll() {
        movies.clear();
        id = 1;
    }

}