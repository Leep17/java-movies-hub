package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;

public class MoviesHandler extends BaseHttpHandler { // Расширьте базовый класс BaseHttpHandler

    private final MoviesStore store;
    private final Gson gson;

    public MoviesHandler(MoviesStore store) {
        this.store = store;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        String query = ex.getRequestURI().getQuery();
        String[] parts = path.split("/");
        String method = ex.getRequestMethod();
        if (method.equalsIgnoreCase("GET") && (path.equals("/movies") || path.startsWith("/movies/"))) {
            if(path.startsWith("/movies/")) {
                try {

                    int id = Integer.parseInt(parts[2]);
                    if(store.getByID(id)==null) {
                        sendJson(ex, 404, gson.toJson(new ErrorResponse("Фильм не найден")));
                    } else {
                        sendJson(ex, 200, gson.toJson(store.getByID(id)));
                    }
                }catch(NumberFormatException | ArrayIndexOutOfBoundsException e) {
                      sendJson(ex,400, gson.toJson(new ErrorResponse("Некорректный ID")));
                }
            }else {
                if (query == null) {
                    String movieList = gson.toJson(store.getAll());
                    sendJson(ex, 200, movieList);
                } else if(!query.startsWith("year=")) {
                    sendJson(ex, 400, gson.toJson(new ErrorResponse("Некорректный параметр запроса — 'year'")));
                } else {
                    try {
                        int year = Integer.parseInt(query.substring(5));
                        sendJson(ex, 200, gson.toJson(store.getAll().stream().filter(movie -> movie.getYear() == year).toList()));
                    } catch(NumberFormatException e) {
                        sendJson(ex, 400, gson.toJson(new ErrorResponse("Некорректный параметр запроса — year")));
                    }
                }
            }
        } else if(method.equalsIgnoreCase("DELETE") && path.startsWith("/movies/")) {
            try {
                int id = Integer.parseInt(parts[2]);
                if (store.getByID(id) == null) {
                    sendJson(ex, 404, gson.toJson(new ErrorResponse("Фильм не найден")));
                } else {
                    store.deleteMovie(id);
                    sendNoContent(ex);
                }
            } catch(NumberFormatException | ArrayIndexOutOfBoundsException e) {
                sendJson(ex,400, gson.toJson(new ErrorResponse("Некорректный ID")));
            }
        } else if(method.equalsIgnoreCase("POST") && path.equals("/movies")) {

            String contentType = ex.getRequestHeaders().getFirst("Content-Type");

            if (contentType == null || !contentType.startsWith("application/json")) {
                sendJson(ex, 415, gson.toJson(new ErrorResponse("Unsupported Media Type")));
                return;
            }

            String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Movie movie;
            try {
                movie = gson.fromJson(body, Movie.class);
            } catch (Exception e) {sendJson(ex, 422, gson.toJson(new ErrorResponse("Ошибка валидации", List.of("некорректный JSON"))));
                return;
            }
            String title = movie.getTitle();
            int currentYear = Year.now().getValue();
            int year = movie.getYear();
            List<String> details = new ArrayList<>();

            if(title == null || title.isBlank()) {
                details.add("Название не должно быть пустым");
            } else if(title.length()>100) {
                details.add("Название не должно быть более 100 знаков");
            }
            if(year < 1888 || year >currentYear + 1) {
                details.add("год должен быть между 1888 и " + (currentYear + 1));
            }

            if(!details.isEmpty()) {
                sendJson(ex, 422, gson.toJson(new ErrorResponse("Ошибка валидации", details)));
            } else {
                sendJson(ex, 201, gson.toJson(store.addMovie(movie)));
            }
        } else {
            sendJson(ex, 405, gson.toJson(new ErrorResponse("Method Not Allowed")));
        }
    }
}