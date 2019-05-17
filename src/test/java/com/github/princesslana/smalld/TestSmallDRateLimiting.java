package com.github.princesslana.smalld;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class TestSmallDRateLimiting {

  private SmallD subject;

  private MockDiscordServer server;

  @BeforeEach
  public void subject() {
    server = new MockDiscordServer();
    subject = server.newSmallD();
  }

  @AfterEach
  public void closeSmallD() {
    subject.close();
  }

  @AfterEach
  public void closeServer() {
    server.close();
  }

  @Test
  public void get_whenHttp429WithNoRetryAfterHeader_shouldThrowClientException() {
    server.connect(subject);
    server.enqueue(new MockResponse().setResponseCode(429));

    Assertions.assertThatExceptionOfType(HttpException.ClientException.class)
        .isThrownBy(() -> subject.get("/test/url"));
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 1, 100})
  public void get_whenHttp429WithRetryAfterHeader_shouldThrowRateLimitException(int retryAfter) {
    long now = System.currentTimeMillis();

    SmallD smalld = server.newSmallDAtMillis(now);

    server.connect(smalld);

    server.enqueue(new MockResponse().setResponseCode(429).setHeader("Retry-After", retryAfter));

    Assertions.assertThatThrownBy(() -> smalld.get("/test/url"))
        .isInstanceOf(RateLimitException.class)
        .hasFieldOrPropertyWithValue("expiry", Instant.ofEpochMilli(now + retryAfter));
  }

  @Test
  public void get_whenGlobalRateLimited_shouldNotMakeHttpRequest() {
    String date = "Tue, 3 Jun 2008 11:05:30 GMT";
    Instant now = Instant.from(DateTimeFormatter.RFC_1123_DATE_TIME.parse(date));

    SmallD smalld = server.newSmallDAtMillis(now.toEpochMilli());

    server.connect(smalld);

    server.enqueue(
        new MockResponse()
            .setResponseCode(429)
            .setHeader("Date", date)
            .setHeader("Retry-After", 1)
            .setHeader("X-RateLimit-Global", "true"));

    makeThrowawayGetRequest(smalld, "/test/url1");

    assertThrowsRateLimitExceptionBeforeRequest(smalld, "/test/url2");
  }

  @Test
  public void get_whenGlobalRateLimitedButExpired_shouldMakeHttpRequest() {
    MutableClock clock = new MutableClock();

    SmallD smalld = server.newSmallD(clock);

    server.connect(smalld);

    server.enqueue(
        new MockResponse()
            .setResponseCode(429)
            .setHeader("Retry-After", 1)
            .setHeader("X-RateLimit-Global", "true"));

    server.enqueue(new MockResponse().setResponseCode(200).setBody("dummy_body"));

    makeThrowawayGetRequest(smalld, "/test/url1");

    clock.plusMillis(100);

    Assertions.assertThatCode(() -> smalld.get("/test/url2")).doesNotThrowAnyException();

    RecordedRequest req = server.takeRequest();

    assertThatRequestWas(req, "GET", "/test/url2");
  }

  @Test
  public void get_whenGlobalRateLimited_shouldFavorResetOverRetryAfter() {
    MutableClock clock = new MutableClock();

    SmallD smalld = server.newSmallD(clock);

    server.connect(smalld);

    server.enqueue(
        new MockResponse()
            .setResponseCode(429)
            .setHeader("Retry-After", 200)
            .setHeader("X-RateLimit-Reset", clock.toEpochMilli() + 50)
            .setHeader("X-RateLimit-Global", "true"));

    server.enqueue(new MockResponse().setResponseCode(200).setBody("dummy_body"));

    makeThrowawayGetRequest(smalld, "/test/url1");

    clock.plusMillis(100);

    Assertions.assertThatCode(() -> smalld.get("/test/url2")).doesNotThrowAnyException();

    RecordedRequest req = server.takeRequest();

    assertThatRequestWas(req, "GET", "/test/url2");
  }

  @Test
  public void get_whenRateLimitedButNotGlobal_shouldMakeHttpRequestForDifferentUrl() {
    SmallD smalld = server.newSmallDAtNow();

    server.connect(smalld);

    server.enqueue(new MockResponse().setResponseCode(429).setHeader("Retry-After", 1));

    server.enqueue(new MockResponse().setResponseCode(200).setBody("response_body"));

    makeThrowawayGetRequest(smalld, "/test/url1");

    int beforeCount = server.getRequestCount();

    String response = smalld.get("/test/url2");

    Assertions.assertThat(response).isEqualTo("response_body");
    Assertions.assertThat(server.getRequestCount()).isEqualTo(beforeCount + 1);
  }

  @Test
  public void get_whenRateLimitedBy429_shouldNotMakeHttpRequest() {
    String date = "Tue, 3 Jun 2008 11:05:30 GMT";
    Instant now = Instant.from(DateTimeFormatter.RFC_1123_DATE_TIME.parse(date));

    SmallD smalld = server.newSmallDAtMillis(now.toEpochMilli());

    server.connect(smalld);

    server.enqueue(
        new MockResponse()
            .setResponseCode(429)
            .setHeader("Date", date)
            .setHeader("Retry-After", 1));

    makeThrowawayGetRequest(smalld, "/test/url1");

    assertThrowsRateLimitExceptionBeforeRequest(smalld, "/test/url1");
  }

  @Test
  public void get_whenRateLimitedByHeaders_shouldNotMakeHttpRequest() {
    long now = System.currentTimeMillis();

    SmallD smalld = server.newSmallDAtMillis(now);

    server.connect(smalld);

    server.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setHeader("X-RateLimit-Remaining", 0)
            .setHeader("X-RateLimit-Reset", now + 100));

    makeThrowawayGetRequest(smalld, "/test/url1");

    assertThrowsRateLimitExceptionBeforeRequest(smalld, "/test/url1");
  }

  @Test
  public void get_whenRateLimitHasRemainingRequests_shouldMakeHttpRequest() {
    MutableClock clock = new MutableClock();

    SmallD smalld = server.newSmallD(clock);

    server.connect(smalld);

    server.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setHeader("X-RateLimit-Remaining", 1)
            .setHeader("X-RateLimit-Reset", clock.toEpochMilli() + 100));

    server.enqueue(new MockResponse().setResponseCode(200).setBody("dummy_body"));

    makeThrowawayGetRequest(smalld, "/test/url1");

    Assertions.assertThatCode(() -> smalld.get("/test/url1")).doesNotThrowAnyException();

    RecordedRequest req = server.takeRequest();

    assertThatRequestWas(req, "GET", "/test/url1");
  }

  @Test
  public void get_whenRateLimitExceedsRemainingRequests_shouldMakeHttpRequest() {
    MutableClock clock = new MutableClock();

    SmallD smalld = server.newSmallD(clock);

    server.connect(smalld);

    server.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setHeader("X-RateLimit-Remaining", 1)
            .setHeader("X-RateLimit-Reset", clock.toEpochMilli() + 100));

    server.enqueue(new MockResponse().setResponseCode(200).setBody("dummy_body"));

    makeThrowawayGetRequest(smalld, "/test/url1");
    makeThrowawayGetRequest(smalld, "/test/url1");

    assertThrowsRateLimitExceptionBeforeRequest(smalld, "/test/url1");
  }

  @Test
  public void get_whenRateLimitAfterReset_shouldMakeHttpRequest() {
    MutableClock clock = new MutableClock();

    SmallD smalld = server.newSmallD(clock);

    server.connect(smalld);

    server.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setHeader("X-RateLimit-Remaining", 0)
            .setHeader("X-RateLimit-Reset", clock.toEpochMilli() + 1));

    server.enqueue(new MockResponse().setResponseCode(200).setBody("dummy_body"));

    makeThrowawayGetRequest(smalld, "/test/url1");

    clock.plusMillis(100);

    Assertions.assertThatCode(() -> smalld.get("/test/url1")).doesNotThrowAnyException();

    RecordedRequest req = server.takeRequest();

    assertThatRequestWas(req, "GET", "/test/url1");
  }

  private void assertThatRequestWas(RecordedRequest req, String method, String path) {
    SoftAssertions.assertSoftly(
        s -> {
          s.assertThat(req.getMethod()).isEqualTo(method);
          s.assertThat(req.getPath()).isEqualTo("/api/v6" + path);
        });
  }

  private void makeThrowawayGetRequest(SmallD smalld, String path) {
    Assertions.catchThrowable(() -> smalld.get(path));
    server.takeRequest();
  }

  private void assertThrowsRateLimitExceptionBeforeRequest(SmallD smalld, String path) {
    assertThatNoServerRequest(
        () -> {
          Assertions.assertThatThrownBy(() -> smalld.get(path))
              .isInstanceOf(RateLimitException.class);
        });
  }

  private void assertThatNoServerRequest(Runnable r) {
    int beforeCount = server.getRequestCount();

    r.run();

    Assertions.assertThat(server.getRequestCount()).isEqualTo(beforeCount);
  }
}
