package woe.simulator;

import static akka.http.javadsl.server.Directives.complete;
import static akka.http.javadsl.server.Directives.concat;
import static akka.http.javadsl.server.Directives.entity;
import static akka.http.javadsl.server.Directives.path;
import static akka.http.javadsl.server.Directives.post;
import static woe.simulator.WorldMap.devicesWithin;
import static woe.simulator.WorldMap.entityIdOf;
import static woe.simulator.WorldMap.regionForZoom0;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.slf4j.Logger;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.cluster.sharding.typed.javadsl.ClusterSharding;
import akka.http.javadsl.Http;
import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.model.StatusCodes;
import akka.http.javadsl.server.Route;

class HttpServer {
  private final ActorSystem<?> actorSystem;
  private final ClusterSharding clusterSharding;
  private ActorRef<Region.Command> replyTo; // hack for unit testing

  static HttpServer start(String host, int port, ActorSystem<?> actorSystem) {
    return new HttpServer(host, port, actorSystem);
  }

  private HttpServer(String host, int port, ActorSystem<?> actorSystem) {
    this.actorSystem = actorSystem;
    clusterSharding = ClusterSharding.get(actorSystem);

    start(host, port);
  }

  private void start(String host, int port) {
    Http.get(actorSystem).newServerAt(host, port).bind(route());
    log().info("HTTP Server started on {}:{}", host, port);
  }

  private Route route() {
    return concat(
        path("selection", this::handleSelectionActionPost)
    );
  }

  private Route handleSelectionActionPost() {
    return post(
        () -> entity(
            Jackson.unmarshaller(SelectionActionRequest.class),
            selectionActionRequest -> {
              try {
                log().debug("POST {}", selectionActionRequest);
                submit(selectionActionRequest);
                return complete(StatusCodes.OK, SelectionActionResponse.ok(StatusCodes.OK.intValue(), selectionActionRequest), Jackson.marshaller());
              } catch (IllegalArgumentException e) {
                log().warn("POST failed {}", selectionActionRequest);
                return complete(StatusCodes.BAD_REQUEST, SelectionActionResponse.failed(e.getMessage(), StatusCodes.BAD_REQUEST.intValue(), selectionActionRequest), Jackson.marshaller());
              }
            }
        )
    );
  }

  private void submit(SelectionActionRequest selectionActionRequest) {
    final var selectionCommand = selectionActionRequest.asSelectionCommand(replyTo);
    final var entityId = entityIdOf(regionForZoom0());
    final var entityRef = clusterSharding.entityRefFor(Region.entityTypeKey, entityId);
    entityRef.tell(selectionCommand);
    log().info("Selection rate {}, deadline {}, {}", selectionActionRequest.rate, selectionCommand.deadline, selectionCommand.region);
  }

  static Duration selectionProcessingDuration(SelectionActionRequest selectionActionRequest) {
    final var rate = selectionActionRequest.rate;
    final var deviceCount = devicesWithin(selectionActionRequest.zoom);
    return Duration.ofSeconds(deviceCount / rate);
  }

  static Instant selectionProcessingDeadline(SelectionActionRequest selectionActionRequest) {
    return Instant.now().plus(selectionProcessingDuration(selectionActionRequest));
  }

  public static class SelectionActionRequest implements CborSerializable {
    public final String action;
    public final int rate;
    public final int zoom;
    public final double topLeftLat;
    public final double topLeftLng;
    public final double botRightLat;
    public final double botRightLng;

    @JsonCreator
    public SelectionActionRequest(
        @JsonProperty("action") String action,
        @JsonProperty("rate") int rate,
        @JsonProperty("zoom") int zoom,
        @JsonProperty("topLeftLat") double topLeftLat,
        @JsonProperty("topLeftLng") double topLeftLng,
        @JsonProperty("botRightLat") double botRightLat,
        @JsonProperty("botRightLng") double botRightLng) {
      this.action = action;
      this.rate = rate;
      this.zoom = zoom;
      this.topLeftLat = topLeftLat;
      this.topLeftLng = topLeftLng;
      this.botRightLat = botRightLat;
      this.botRightLng = botRightLng;
    }

    SelectionActionRequest(String action, int rate, WorldMap.Region region) {
      this(action, rate, region.zoom, region.topLeft.lat, region.topLeft.lng, region.botRight.lat, region.botRight.lng);
    }

    Region.SelectionCommand asSelectionCommand(ActorRef<Region.Command> replyTo) {
      final var clientRegion = new WorldMap.Region(zoom, WorldMap.topLeft(topLeftLat, topLeftLng), WorldMap.botRight(botRightLat, botRightLng));
      final var region = WorldMap.alignClientRegion(clientRegion);
      final var deadline = selectionProcessingDeadline(this);
      final var delayed = false;

      switch (action) {
        case "create":
          return new Region.SelectionCreate(region, deadline, delayed, replyTo);
        case "delete":
          return new Region.SelectionDelete(region, deadline, delayed, replyTo);
        case "happy":
          return new Region.SelectionHappy(region, deadline, delayed, replyTo);
        case "sad":
          return new Region.SelectionSad(region, deadline, delayed, replyTo);
        default:
          throw new IllegalArgumentException(String.format("Action '%s' illegal, must be one of: 'create', 'delete', 'happy', or 'sad'.", action));
      }
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      SelectionActionRequest that = (SelectionActionRequest) o;
      return rate == that.rate &&
          zoom == that.zoom &&
          Double.compare(that.topLeftLat, topLeftLat) == 0 &&
          Double.compare(that.topLeftLng, topLeftLng) == 0 &&
          Double.compare(that.botRightLat, botRightLat) == 0 &&
          Double.compare(that.botRightLng, botRightLng) == 0 &&
          action.equals(that.action);
    }

    @Override
    public int hashCode() {
      return Objects.hash(action, rate, zoom, topLeftLat, topLeftLng, botRightLat, botRightLng);
    }

    @Override
    public String toString() {
      return String.format("%s[%s, %d, %d, %1.9f, %1.9f, %1.9f, %1.9f]", getClass().getSimpleName(), action, rate, zoom, topLeftLat, topLeftLng, botRightLat, botRightLng);
    }
  }

  public static class SelectionActionResponse implements CborSerializable {
    public final String message;
    public final int httpStatusCode;
    public final SelectionActionRequest selectionActionRequest;

    @JsonCreator
    public SelectionActionResponse(String message, int httpStatusCode, SelectionActionRequest selectionActionRequest) {
      this.message = message;
      this.httpStatusCode = httpStatusCode;
      this.selectionActionRequest = selectionActionRequest;
    }

    static SelectionActionResponse ok(int httpStatusCode, SelectionActionRequest selectionActionRequest) {
      return new SelectionActionResponse("Accepted", httpStatusCode, selectionActionRequest);
    }

    static SelectionActionResponse failed(String message, int httpStatusCode, SelectionActionRequest selectionActionRequest) {
      return new SelectionActionResponse(message, httpStatusCode, selectionActionRequest);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      SelectionActionResponse that = (SelectionActionResponse) o;
      return httpStatusCode == that.httpStatusCode &&
          message.equals(that.message) &&
          selectionActionRequest.equals(that.selectionActionRequest);
    }

    @Override
    public int hashCode() {
      return Objects.hash(message, httpStatusCode, selectionActionRequest);
    }

    @Override
    public String toString() {
      return String.format("%s[%d, %s, %s]", getClass().getSimpleName(), httpStatusCode, message, selectionActionRequest);
    }
  }

  // Hack for unit testing
  void replyTo(ActorRef<Region.Command> replyTo) {
    this.replyTo = replyTo;
  }

  private Logger log() {
    return actorSystem.log();
  }
}
