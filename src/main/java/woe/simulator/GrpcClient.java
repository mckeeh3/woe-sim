package woe.simulator;

import akka.actor.typed.ActorSystem;
import akka.grpc.GrpcClientSettings;
import woe.twin.grpc.TelemetryRequestGrpc;
import woe.twin.grpc.TelemetryResponseGrpc;
import woe.twin.grpc.TelemetryServiceClient;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

class GrpcClient implements Client {
  private final ActorSystem<?> actorSystem;
  private final String host;
  private final int port;

  public GrpcClient(ActorSystem<?> actorSystem, String host, int port) {
    this.actorSystem = actorSystem;
    this.host = host;
    this.port = port;
  }

  @Override
  public CompletionStage<Telemetry.TelemetryResponse> post(Region.SelectionCommand selectionCommand) {
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
}
