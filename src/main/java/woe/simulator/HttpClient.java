package woe.simulator;

import akka.actor.typed.ActorSystem;
import akka.http.javadsl.Http;
import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.model.ContentTypes;
import akka.http.javadsl.model.HttpEntities;
import akka.http.javadsl.model.HttpEntity;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.headers.RawHeader;
import akka.stream.Materializer;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

class HttpClient {
  private final ActorSystem<?> actorSystem;
  private final Materializer materializer;
  private final String url;

  HttpClient(ActorSystem<?> actorSystem) {
    this(actorSystem, url(actorSystem));
  }

  HttpClient(ActorSystem<?> actorSystem, String url) {
    this.actorSystem = actorSystem;
    this.materializer = Materializer.matFromSystem(actorSystem.classicSystem());
    this.url = url;
  }

  CompletionStage<TelemetryResponse> post(Region.SelectionCommand selectionCommand) {
    return post(new TelemetryRequest(selectionCommand.action.name(), selectionCommand.region));
  }

  private CompletionStage<TelemetryResponse> post(TelemetryRequest telemetryRequest) {
    return Http.get(actorSystem.classicSystem())
        .singleRequest(HttpRequest.POST(url)
            .withHeaders(Collections.singletonList(RawHeader.create("Connection", "close")))
            .withEntity(toHttpEntity(telemetryRequest)))
        .thenCompose(r -> {
          if (r.status().isSuccess()) {
            return Jackson.unmarshaller(TelemetryResponse.class).unmarshal(r.entity(), materializer);
          } else {
            return CompletableFuture.completedFuture(new TelemetryResponse(r.status().reason(), r.status().intValue(), telemetryRequest));
          }
        });
  }

  private static String url(ActorSystem<?> actorSystem) {
    final String host = actorSystem.settings().config().getString("woe.twin.http.server.host");
    final int port = actorSystem.settings().config().getInt("woe.twin.http.server.port");
    return String.format("http://%s:%d/telemetry", host, port);
  }

  public static class TelemetryRequest {
    public final String action;
    public final int zoom;
    public final double topLeftLat;
    public final double topLeftLng;
    public final double botRightLat;
    public final double botRightLng;

    TelemetryRequest(String action, WorldMap.Region region) {
      this(action, region.zoom, region.topLeft.lat, region.topLeft.lng, region.botRight.lat, region.botRight.lng);
    }

    @JsonCreator
    public TelemetryRequest(
        @JsonProperty("action") String action,
        @JsonProperty("zoom") int zoom,
        @JsonProperty("topLeftLat") double topLeftLat,
        @JsonProperty("topLeftLng") double topLeftLng,
        @JsonProperty("botRightLat") double botRightLat,
        @JsonProperty("botRightLng") double botRightLng) {
      this.action = action;
      this.zoom = zoom;
      this.topLeftLat = topLeftLat;
      this.topLeftLng = topLeftLng;
      this.botRightLat = botRightLat;
      this.botRightLng = botRightLng;
    }

    @Override
    public String toString() {
      return String.format("%s[%s, %d, %1.9f, %1.9f, %1.9f, %1.9f]", getClass().getSimpleName(), action, zoom, topLeftLat, topLeftLng, botRightLat, botRightLng);
    }
  }

  public static class TelemetryResponse {
    public final String message;
    public final int httpStatusCode;
    public final TelemetryRequest telemetryRequest;

    @JsonCreator
    public TelemetryResponse(
        @JsonProperty("message") String message,
        @JsonProperty("httpStatusCode") int httpStatusCode,
        @JsonProperty("deviceTelemetryRequest") TelemetryRequest telemetryRequest) {
      this.message = message;
      this.httpStatusCode = httpStatusCode;
      this.telemetryRequest = telemetryRequest;
    }

    @Override
    public String toString() {
      return String.format("%s[%d, %s, %s]", getClass().getSimpleName(), httpStatusCode, message, telemetryRequest);
    }
  }

  private static HttpEntity.Strict toHttpEntity(Object pojo) {
    return HttpEntities.create(ContentTypes.APPLICATION_JSON, toJson(pojo).getBytes());
  }

  private static String toJson(Object pojo) {
    final ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
    try {
      return ow.writeValueAsString(pojo);
    } catch (JsonProcessingException e) {
      return String.format("{ \"error\" : \"%s\" }", e.getMessage());
    }
  }
}
