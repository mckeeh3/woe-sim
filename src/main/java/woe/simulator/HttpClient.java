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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

class HttpClient implements Client {
  private final ActorSystem<?> actorSystem;
  private final Materializer materializer;
  private final String url;

  public HttpClient(ActorSystem<?> actorSystem, String host, int port) {
    this.actorSystem = actorSystem;
    this.materializer = Materializer.matFromSystem(actorSystem.classicSystem());
    url = String.format("http://%s:%d/telemetry", host, port);
  }

  HttpClient(ActorSystem<?> actorSystem, String url) {
    this.actorSystem = actorSystem;
    this.materializer = Materializer.matFromSystem(actorSystem.classicSystem());
    this.url = url;
  }

  @Override
  public CompletionStage<Telemetry.TelemetryResponse> post(Region.SelectionCommand selectionCommand) {
    return post(new Telemetry.TelemetryRequest(selectionCommand.action.name(), selectionCommand.region));
  }

  private CompletionStage<Telemetry.TelemetryResponse> post(Telemetry.TelemetryRequest telemetryRequest) {
    if (url == null) {
      return CompletableFuture.completedFuture(new Telemetry.TelemetryResponse("no-op", 200, telemetryRequest));
    }
    return Http.get(actorSystem.classicSystem())
        .singleRequest(HttpRequest.POST(url)
            .withHeaders(Collections.singletonList(RawHeader.create("Connection", "close")))
            .withEntity(toHttpEntity(telemetryRequest)))
        .thenCompose(r -> {
          if (r.status().isSuccess()) {
            return Jackson.unmarshaller(Telemetry.TelemetryResponse.class).unmarshal(r.entity(), materializer);
          } else {
            return CompletableFuture.completedFuture(new Telemetry.TelemetryResponse(r.status().reason(), r.status().intValue(), telemetryRequest));
          }
        });
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
