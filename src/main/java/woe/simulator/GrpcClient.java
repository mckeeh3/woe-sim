package woe.simulator;

import akka.actor.typed.ActorSystem;
import akka.grpc.GrpcClientSettings;
import com.typesafe.config.ConfigException;
import woe.simulator.grpc.TelemetryRequestGrpc;
import woe.simulator.grpc.TelemetryResponseGrpc;
import woe.simulator.grpc.TelemetryServiceClient;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class GrpcClient {
  private final ActorSystem<?> actorSystem;
  private final String host;
  private final int port;

  public GrpcClient(ActorSystem<?> actorSystem) {
    this.actorSystem = actorSystem;
    final Optional<HostPort> hostPort = hostPort(actorSystem);
    this.host = hostPort.isEmpty() ? null : hostPort.get().host;
    this.port = hostPort.isEmpty() ? -1 : hostPort.get().port;
  }

  CompletionStage<Telemetry.TelemetryResponse> post(Region.SelectionCommand selectionCommand) {
    return post(new Telemetry.TelemetryRequest(selectionCommand.action.name(), selectionCommand.region));
  }

  private CompletionStage<Telemetry.TelemetryResponse> post(Telemetry.TelemetryRequest telemetryRequest) {
    if (host == null) {
      return CompletableFuture.completedFuture(new Telemetry.TelemetryResponse("no-op", 200, telemetryRequest));
    }
    final GrpcClientSettings grpcClientSettings = GrpcClientSettings.connectToServiceAt(host, port, actorSystem)
        .withTls(false);
    final TelemetryServiceClient telemetryServiceClient = TelemetryServiceClient.create(grpcClientSettings, actorSystem);
    final CompletionStage<TelemetryResponseGrpc> telemetryResponseGrpc = telemetryServiceClient.telemetry(toTelemetryRequestGrpc(telemetryRequest));
    return telemetryResponseGrpc.thenApply(this::toTelemetryResponse);
  }

  private TelemetryRequestGrpc toTelemetryRequestGrpc(Telemetry.TelemetryRequest telemetryRequest) {
    return TelemetryRequestGrpc.newBuilder()
        .setAction(telemetryRequest.action)
        .setZoom(telemetryRequest.zoom)
        .setTopLeftLat(telemetryRequest.topLeftLat)
        .setTopLeftLng(telemetryRequest.topLeftLng)
        .setBotRightLat(telemetryRequest.botRightLat)
        .setBotRightLng(telemetryRequest.botRightLng)
        .build();
  }

  private Telemetry.TelemetryResponse toTelemetryResponse(TelemetryResponseGrpc telemetryResponseGrpc) {
    return new Telemetry.TelemetryResponse(
        telemetryResponseGrpc.getMessage(),
        telemetryResponseGrpc.getHttpStatusCode(),
        toTelemetryRequest(telemetryResponseGrpc.getTelemetryRequest())
    );
  }

  private Telemetry.TelemetryRequest toTelemetryRequest(TelemetryRequestGrpc telemetryRequestGrpc) {
    return new Telemetry.TelemetryRequest(
        telemetryRequestGrpc.getAction(),
        telemetryRequestGrpc.getZoom(),
        telemetryRequestGrpc.getTopLeftLat(),
        telemetryRequestGrpc.getTopLeftLng(),
        telemetryRequestGrpc.getBotRightLat(),
        telemetryRequestGrpc.getBotRightLng()
    );
  }

  static Optional<HostPort> hostPort(ActorSystem<?> actorSystem) {
    try {
      final String host = actorSystem.settings().config().getString("woe.twin.grpc.server.host");
      final int port = actorSystem.settings().config().getInt("woe.twin.grpc.server.port");
      return Optional.of(new HostPort(host, port));
    } catch (ConfigException e) {
      return Optional.empty();
    }
  }

  static class HostPort {
    final String host;
    final int port;

    private HostPort(String host, int port) {
      this.host = host;
      this.port = port;
    }
  }
}
