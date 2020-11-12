package woe.simulator;

import akka.actor.typed.ActorSystem;
import akka.grpc.GrpcClientSettings;
import woe.twin.grpc.cloudstate.*;

import java.util.concurrent.CompletionStage;

class GrpcClientCloudstate implements Client {
  private final DigitalTwinClient grpcClient;

  public GrpcClientCloudstate(ActorSystem<?> actorSystem, String host, int port) {
    final GrpcClientSettings grpcClientSettings = GrpcClientSettings.connectToServiceAt(host, port, actorSystem)
        .withTls(false);
    grpcClient = DigitalTwinClient.create(grpcClientSettings, actorSystem);
  }

  @Override
  public CompletionStage<Telemetry.TelemetryResponse> post(Region.SelectionCommand selectionCommand) {
    switch (selectionCommand.action) {
      case create:
        return grpcClient.addDevice(getRegion(getWordWideRegion(selectionCommand.region)))
            .thenApply(this::fromState);
      case delete:
        return grpcClient.deleteDevice(getRegion(getWordWideRegion(selectionCommand.region)))
            .thenApply(this::fromState);
      case happy:
        return grpcClient.setDeviceHappy(getRegion(getWordWideRegion(selectionCommand.region)))
            .thenApply(this::fromState);
      case sad:
        return grpcClient.setDeviceSad(getRegion(getWordWideRegion(selectionCommand.region)))
            .thenApply(this::fromState);
      case ping:
        PingRequest request = PingRequest.newBuilder().setWordwideregion(getWordWideRegion(selectionCommand.region)).build();
        return grpcClient.pingDevice(request).thenApply(this::fromPing);
    }
    return null;
  }

  private static String getWordWideRegion(WorldMap.Region region) {
    final var builder = new StringBuilder();
    builder.append(region.zoom).append("_");
    builder.append(region.botRight.lat).append("_");
    builder.append(region.botRight.lng).append("_");
    builder.append(region.topLeft.lat).append("_");
    builder.append(region.topLeft.lng);
    return builder.toString();
  }

  private static RegionGrpc getRegion(String name) {
    return RegionGrpc.newBuilder().setWordwideregion(name).build();
  }

  private Telemetry.TelemetryResponse fromState(TwinState state) {
    return new Telemetry.TelemetryResponse("Request completed", 200, null);
  }

  private Telemetry.TelemetryResponse fromPing(PingResponse ping) {
    return new Telemetry.TelemetryResponse("Request completed", 200, null);
  }
}
