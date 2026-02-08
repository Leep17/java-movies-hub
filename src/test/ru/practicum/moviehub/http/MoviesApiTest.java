package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import org.junit.jupiter.api.*;
import ru.practicum.moviehub.model.Movie;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;


public class MoviesApiTest {
    private static final String BASE = "http://localhost:8080";// !!! добавьте базовую часть URL
    private static MoviesServer server;
    private static HttpClient client;

    @BeforeAll
    static void beforeAll() {
        server = new MoviesServer();
        server.start();
        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
    }

    @BeforeEach
    void beforeEach() {
        server.reset();
    }

    @AfterAll
    static void afterAll() {
        server.stop();
    }

    @Test
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies")) // !!! Добавьте правильный URI
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        Assertions.assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");

        String contentTypeHeaderValue = resp.headers().firstValue("Content-Type").orElse("");

        Assertions.assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue, "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        Assertions.assertTrue(body.startsWith("[") && body.endsWith("]"),
                "Ожидается JSON-массив");
    }

    @Test
    void getMovies_ErrorPostFilm() throws Exception {
        HttpRequest req1 = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("""
                        {
                          "title": "Сопрано",
                          "year": 2004
                        }
                        """))
                .build();

        HttpResponse<String> resp1 =
                client.send(req1, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        Assertions.assertEquals(201, resp1.statusCode(), "POST /movies должен вернуть 201");

        HttpRequest req2 = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("""
                        {
                          "title": "TEST",
                          "year": 2005
                        }
                        """))
                .build();

        HttpResponse<String> resp2 = client.send(req2, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        Assertions.assertEquals(201, resp2.statusCode(), "POST /movies должен вернуть 201");

        HttpRequest req3 = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("""
                        {
                          "title": "WWE",
                          "year": 2005
                        }
                        """))
                .build();

        HttpResponse<String> resp3 = client.send(req3, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        Assertions.assertEquals(201, resp3.statusCode(), "POST /movies должен вернуть 201");

        HttpResponse<String> getResp =
                client.send(
                        HttpRequest.newBuilder()
                                .uri(URI.create(BASE + "/movies"))
                                .GET()
                                .build(),
                        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
                );

        Assertions.assertEquals(200, getResp.statusCode());

        Gson gson = new Gson();
        List<Movie> movies = gson.fromJson(
                getResp.body(),
                new ListOfMoviesTypeToken().getType()
        );
        Assertions.assertEquals(3, movies.size());

        HttpResponse<String> resp5 =
                client.send(
                        HttpRequest.newBuilder()
                                .uri(URI.create(BASE + "/movies/" + 1))
                                .GET()
                                .build(),
                        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
                );
        String body = resp5.body().trim();
        Movie movie = gson.fromJson(body, Movie.class);
        int id = movie.getId();
        Assertions.assertEquals(1,id);

        HttpResponse<String> resp6 =
                client.send(
                        HttpRequest.newBuilder()
                                .uri(URI.create(BASE + "/movies/" + 5))
                                .GET()
                                .build(),
                        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
                );

        Assertions.assertEquals(404,resp6.statusCode());

        HttpResponse<String> resp7 =
                client.send(
                        HttpRequest.newBuilder()
                                .uri(URI.create(BASE + "/movies/" + "fafew"))
                                .GET()
                                .build(),
                        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
                );

        Assertions.assertEquals(400,resp7.statusCode());

        HttpResponse<String> resp8 =
                client.send(
                        HttpRequest.newBuilder()
                                .uri(URI.create(BASE + "/movies?year=" + 2005))
                                .GET()
                                .build(),
                        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
                );

        Assertions.assertEquals(200,resp8.statusCode());

        HttpResponse<String> resp9 =
                client.send(
                        HttpRequest.newBuilder()
                                .uri(URI.create(BASE + "/movies?year=" + "qwerty"))
                                .GET()
                                .build(),
                        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
                );

        Assertions.assertEquals(400,resp9.statusCode());
    }

    @Test
    void getMovies_errorPostFilm() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("""
                        {
                          "title": " ",
                          "year": 2004
                        }
                        """))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        Assertions.assertEquals(422, resp.statusCode(), "POST /movies должен вернуть 422");

        String longTitle = "A".repeat(101);
        HttpRequest req1 = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("""
                        {
                          "title": "%s",
                          "year": 2004
                        }
                        """.formatted(longTitle)))
                .build();

        HttpResponse<String> resp1 = client.send(req1, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        Assertions.assertEquals(422, resp1.statusCode(), "POST /movies должен вернуть 422");

        HttpRequest req2 = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("""
                        {
                          "title": "TEST",
                          "year": 2300
                        }
                        """))
                .build();

        HttpResponse<String> resp2 = client.send(req2, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        Assertions.assertEquals(422, resp2.statusCode(), "POST /movies должен вернуть 422");

        HttpRequest req3 = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/jso")
                .POST(HttpRequest.BodyPublishers.ofString("""
                        {
                          "title": "TEST",
                          "year": 2000
                        }
                        """))
                .build();

        HttpResponse<String> resp3 = client.send(req3, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        Assertions.assertEquals(415, resp3.statusCode(), "POST /movies должен вернуть 415");

        HttpRequest req4 = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("""
                        {
                          "title"
                        }
                        """))
                .build();

        HttpResponse<String> resp4 =
                client.send(req4, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        Assertions.assertEquals(422, resp4.statusCode(), "POST /movies должен вернуть 422");
    }

    @Test
    void Movies_deleteFilm() throws Exception {

        HttpRequest req10 = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("""
                        {
                          "title": "King Kong",
                          "year": 2012
                        }
                        """))
                .build();

        HttpResponse<String> resp10 = client.send(req10, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        Assertions.assertEquals(201, resp10.statusCode(), "POST /movies должен вернуть 201");

        HttpRequest req11 = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/" + 11))
                .header("Content-Type", "application/json")
                .DELETE()
                .build();

        HttpResponse<String> resp11 =
                client.send(req11, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        Assertions.assertEquals(404, resp11.statusCode(), "DELETE /movies должен вернуть 404");

        HttpRequest req12 = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/" + "vaknvla"))
                .header("Content-Type", "application/json")
                .DELETE()
                .build();

        HttpResponse<String> resp12 = client.send(req12, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        Assertions.assertEquals(400, resp12.statusCode(), "DELETE /movies должен вернуть 400");

    }

    @Test
    void Movies_notAllowedMethod() throws Exception {
        HttpRequest req13 = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString("asdsaf"))
                .build();

        HttpResponse<String> resp13 = client.send(req13, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        Assertions.assertEquals(405, resp13.statusCode(), "movies должен вернуть 405");
    }
}