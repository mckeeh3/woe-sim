package woe.simulator;

import java.util.concurrent.CompletionStage;

import akka.actor.typed.ActorSystem;
import akka.grpc.GrpcClientSettings;
import woe.simulator.Region.SelectionCommand;
import woe.simulator.Telemetry.TelemetryResponse;
import woe.twin.IotDeviceClient;
import woe.twin.IotDeviceApi.Region;
import woe.twin.IotDeviceApi.TelemetryRequest;

public class GrpcClientWoeTwinJs implements Client {
  private final ActorSystem<?> actorSystem;
  private final IotDeviceClient iotDeviceClient;

  public GrpcClientWoeTwinJs(ActorSystem<?> actorSystem, String host, int port) {
    this.actorSystem = actorSystem;
    final var settings = GrpcClientSettings.connectToServiceAt(host, port, actorSystem).withTls(true);
    iotDeviceClient = IotDeviceClient.create(settings, actorSystem);
    actorSystem.log().info("{} {}:{}", getClass().getSimpleName(), host, port);
  }

  @Override
  public CompletionStage<TelemetryResponse> post(SelectionCommand selectionCommand) {
    actorSystem.log().info("{} {}", getClass().getSimpleName(), selectionCommand);
    var telemetryRequest = new Telemetry.TelemetryRequest(selectionCommand.action.name(), selectionCommand.region);
    return iotDeviceClient.telemetry(toTelemetryRequest(selectionCommand))
      .thenApply(res -> {
        var elapsedSec = (System.nanoTime() - res.getTelemetryRequest().getStartTimeNs()) / 1000000000.0;
        var elapsedFmt = String.format("%.3fs", elapsedSec);
        actorSystem.log().info("{} {} {} {}", getClass().getSimpleName(), elapsedFmt, res.getTelemetryRequest().getAction(), res.getTelemetryRequest().getEntityId());
        return new TelemetryResponse(res.getMessage(), res.getHttpStatusCode(), telemetryRequest);
      })
      .exceptionally(e -> {
        actorSystem.log().warn("gRPC request failed", e);
        return new TelemetryResponse(e.getMessage(), 500, telemetryRequest);
      });
  }

  private TelemetryRequest toTelemetryRequest(SelectionCommand selectionCommand) {
    var region = Region.newBuilder()
      .setZoom(selectionCommand.region.zoom)
      .setTopLeftLat(selectionCommand.region.topLeft.lat)
      .setTopLeftLng(selectionCommand.region.topLeft.lng)
      .setBotRightLat(selectionCommand.region.botRight.lat)
      .setBotRightLng(selectionCommand.region.botRight.lng)
      .build();
    return TelemetryRequest.newBuilder()
      .setEntityId(WorldMap.entityIdOf(selectionCommand.region))
      .setRegion(region)
      .setAction(toActionFrom(selectionCommand))
      .setStartTimeNs(System.nanoTime())
      .build();
  }

  private String toActionFrom(SelectionCommand selectionCommand) {
    switch (selectionCommand.action) {
      case create:
        return "create";
      case delete:
        return "delete";
      case happy:
        return "happy";
      case sad:
        return "sad";
      case ping:
        return "ping";
    }
    return null;
  }
}
