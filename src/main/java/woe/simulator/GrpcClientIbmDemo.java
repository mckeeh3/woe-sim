package woe.simulator;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import com.google.protobuf.Empty;

import akka.actor.typed.ActorSystem;
import akka.grpc.GrpcClientSettings;
import devices.DeviceServiceClient;
import devices.AkkaServerlessIbmDemo.TelemetryRequest;
import devices.AkkaServerlessIbmDemo.TelemetryRequest.Action;
import woe.simulator.Region.SelectionCommand;
import woe.simulator.Telemetry.TelemetryResponse;

class GrpcClientIbmDemo implements Client {
  private final ActorSystem actorSystem;
  private final DeviceServiceClient deviceServiceClient;

  public GrpcClientIbmDemo(ActorSystem<?> actorSystem, String host, int port) {
    this.actorSystem = actorSystem;
    final GrpcClientSettings settings = GrpcClientSettings.connectToServiceAt(host, port, actorSystem)
        .withTls(false);
    deviceServiceClient = DeviceServiceClient.create(settings, actorSystem);
  }

  @Override
  public CompletionStage<TelemetryResponse> post(SelectionCommand selectionCommand) {
    // CompletionStage<Empty> telemetry = deviceServiceClient.telemetry(toTelemetryRequest(selectionCommand));
    // telemetry.thenApply(empty -> toTelemetryResponse(selectionCommand));
    var telemetryRequest = new Telemetry.TelemetryRequest(selectionCommand.action.name(), selectionCommand.region);
    return deviceServiceClient.telemetry(toTelemetryRequest(selectionCommand))
      .thenApply(empty -> {
        return new TelemetryResponse("accepted", 200, telemetryRequest);
      })
      .exceptionally(e -> {
        actorSystem.log().warn("gRPC request failed", e);
        return new TelemetryResponse(e.getMessage(), 500, telemetryRequest);
      });
    //return toTelemetryResponse(selectionCommand);
  }

  private TelemetryRequest toTelemetryRequest(SelectionCommand selectionCommand) {
    return TelemetryRequest.newBuilder()
      .setDeviceId(WorldMap.entityIdOf(selectionCommand.region))
      .setAction(toActionFrom(selectionCommand))
      .build();
  }

  private Action toActionFrom(SelectionCommand selectionCommand) {
    switch (selectionCommand.action) {
      case create:
        return Action.HAPPY;
      case delete:
        return Action.DELETE;
      case happy:
        return Action.HAPPY;
      case sad:
        return Action.SAD;
      case ping:
        return Action.PING;
    }
    return null;
  }

  private CompletionStage<TelemetryResponse> toTelemetryResponse(SelectionCommand selectionCommand) {
    var telemetryRequest = new Telemetry.TelemetryRequest(selectionCommand.action.name(), selectionCommand.region);
    return CompletableFuture.completedFuture(new TelemetryResponse("", 200, telemetryRequest));
  }
}
