package woe.simulator;

import java.util.concurrent.CompletionStage;

import akka.actor.typed.ActorSystem;
import akka.grpc.GrpcClientSettings;
import service.WirelessMeshServiceClient;
import woe.simulator.Region.SelectionCommand;
import woe.simulator.Telemetry.TelemetryResponse;

public class GrpcClientWirelessMesh implements Client {
  private final WirelessMeshServiceClient grpcClient;

public GrpcClientWirelessMesh(ActorSystem<?> actorSystem, String host, int port) {
    final GrpcClientSettings grpcClientSettings = GrpcClientSettings.connectToServiceAt(host, port, actorSystem)
        .withTls(false);
    grpcClient = WirelessMeshServiceClient.create(grpcClientSettings, actorSystem);
  }

  @Override
  public CompletionStage<TelemetryResponse> post(SelectionCommand selectionCommand) {
    switch (selectionCommand.action) {
      case create:
        return null;
      case delete:
        return null;
      case happy:
        return null;
      case sad:
        return null;
      case ping:
        return null;
    }
    return null;
  }
}
