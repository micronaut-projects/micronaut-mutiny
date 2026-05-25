/*
 * Copyright 2017-2026 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.mutiny.http.client;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Patch;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.mutiny.http.client.proxy.MutinyProxyHttpClient;
import io.micronaut.mutiny.http.client.websocket.MutinyWebSocketClient;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@MicronautTest
class MutinyHttpClientTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    @Inject
    @Client("/")
    MutinyHttpClient httpClient;

    @Inject
    @Client("/")
    MutinyStreamingHttpClient streamingHttpClient;

    @Inject
    @Client("/")
    MutinySseClient sseClient;

    @Inject
    @Client("/")
    MutinyProxyHttpClient proxyHttpClient;

    @Inject
    @Client("/")
    MutinyWebSocketClient webSocketClient;

    @Inject
    BookClient client;

    @Test
    void clientsAreInjected() {
        assertNotNull(httpClient);
        assertNotNull(streamingHttpClient);
        assertNotNull(sseClient);
        assertNotNull(proxyHttpClient);
        assertNotNull(webSocketClient);
    }

    @Test
    void declarativeAndInjectedClientsSupportMutinyTypes() {
        assertNull(await(client.get(99)));
        assertEquals(List.of(), await(client.list()));
        assertEquals(List.of(), client.stream().collect().asList().await().atMost(TIMEOUT));
        assertEquals(List.of(), client.missingStream().collect().asList().await().atMost(TIMEOUT));

        Book book = await(client.save("The Stand"));

        assertNotNull(book);
        assertEquals("The Stand", book.getTitle());
        assertEquals(1, book.getId());

        Book retrieved = await(client.get(book.getId()));

        assertNotNull(retrieved);
        assertEquals("The Stand", retrieved.getTitle());
        assertEquals(1, retrieved.getId());
        assertEquals(List.of(retrieved), client.stream().collect().asList().await().atMost(TIMEOUT));
        assertEquals("The Stand", httpClient.retrieve(HttpRequest.GET("/mutiny/books/by-id/" + book.getId()), Book.class)
            .await()
            .atMost(TIMEOUT)
            .getTitle());

        HttpResponse<Book> bookResponse = await(client.getResponse(book.getId()));

        assertEquals(HttpStatus.OK, bookResponse.status());
        assertNotNull(bookResponse.body());
        assertEquals("The Stand", bookResponse.body().getTitle());

        Book updated = await(client.update(book.getId(), "The Shining"));

        assertNotNull(updated);
        assertEquals("The Shining", updated.getTitle());
        assertEquals(1, updated.getId());

        Book deleted = await(client.delete(book.getId()));

        assertNotNull(deleted);
        assertEquals("The Shining", deleted.getTitle());
        assertNull(await(client.get(book.getId())));
    }

    private static <T> T await(Uni<T> uni) {
        try {
            return uni.await().atMost(TIMEOUT);
        } catch (HttpClientResponseException e) {
            throw new AssertionError(e.getResponse().getBody(String.class).orElse(e.getMessage()), e);
        }
    }

    @Client("/mutiny/books")
    interface BookClient {

        @Get("/by-id/{id}")
        Uni<Book> get(@PathVariable long id);

        @Get("/res/{id}")
        Uni<HttpResponse<Book>> getResponse(@PathVariable long id);

        @Get
        Uni<List<Book>> list();

        @Get(value = "/stream", processes = MediaType.APPLICATION_JSON_STREAM)
        @Produces(MediaType.APPLICATION_JSON_STREAM)
        Multi<Book> stream();

        @Get("/missing-stream")
        Multi<Book> missingStream();

        @Post
        Uni<Book> save(@Body String title);

        @Patch("/by-id/{id}")
        Uni<Book> update(@PathVariable long id, @Body String title);

        @Delete("/by-id/{id}")
        Uni<Book> delete(@PathVariable long id);
    }

    @Controller("/mutiny/books")
    static class BookController {

        private final Map<Long, Book> books = new LinkedHashMap<>();
        private final AtomicLong currentId = new AtomicLong();

        @Get("/by-id/{id}")
        Uni<HttpResponse<Book>> get(@PathVariable long id) {
            Book book = books.get(id);
            return Uni.createFrom().item(book == null ? HttpResponse.notFound() : HttpResponse.ok(book));
        }

        @Get("/res/{id}")
        Uni<HttpResponse<Book>> getResponse(@PathVariable long id) {
            return get(id);
        }

        @Get
        Uni<List<Book>> list() {
            return Uni.createFrom().item(new ArrayList<>(books.values()));
        }

        @Get(value = "/stream", processes = MediaType.APPLICATION_JSON_STREAM)
        @Produces(MediaType.APPLICATION_JSON_STREAM)
        Multi<Book> stream() {
            return Multi.createFrom().iterable(books.values());
        }

        @Get("/missing-stream")
        HttpResponse<?> missingStream() {
            return HttpResponse.notFound();
        }

        @Post
        Uni<Book> save(@Body String title) {
            Book book = new Book(currentId.incrementAndGet(), title);
            books.put(book.getId(), book);
            return Uni.createFrom().item(book);
        }

        @Patch("/by-id/{id}")
        Uni<HttpResponse<Book>> update(@PathVariable long id, @Body String title) {
            Book book = books.get(id);
            if (book == null) {
                return Uni.createFrom().item(HttpResponse.notFound());
            }
            book.setTitle(title);
            return Uni.createFrom().item(HttpResponse.ok(book));
        }

        @Delete("/by-id/{id}")
        Uni<HttpResponse<Book>> delete(@PathVariable long id) {
            Book book = books.remove(id);
            return Uni.createFrom().item(book == null ? HttpResponse.notFound() : HttpResponse.ok(book));
        }
    }

    public static class Book {

        private long id;
        private String title;

        public Book() {
        }

        public Book(long id, String title) {
            this.id = id;
            this.title = title;
        }

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Book book)) {
                return false;
            }
            return id == book.id && title.equals(book.title);
        }

        @Override
        public int hashCode() {
            return Long.hashCode(id) * 31 + title.hashCode();
        }
    }
}
