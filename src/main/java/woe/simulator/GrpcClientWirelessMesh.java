package woe.simulator;

import java.util.concurrent.CompletionStage;

import akka.actor.typed.ActorSystem;
import akka.grpc.GrpcClientSettings;
import service.AkkaServerlessDemo.AddCustomerLocationCommand;
import service.AkkaServerlessDemo.RemoveCustomerLocationCommand;
import service.WirelessMeshServiceClient;
import woe.simulator.Region.SelectionCommand;
import woe.simulator.Telemetry.TelemetryRequest;
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
        return addCustomerLocation(selectionCommand);
      case delete:
        return removeCustomerLocation(selectionCommand);
      case happy:
        return null;
      case sad:
        return null;
      case ping:
        return null;
    }
    return null;
  }

  private CompletionStage<TelemetryResponse> addCustomerLocation(SelectionCommand selectionCommand) {
    return grpcClient
        .addCustomerLocation(AddCustomerLocationCommand.newBuilder().setCustomerLocationId(getCustomerId(selectionCommand)).build())
        .thenApply(e -> new TelemetryResponse("Customer location created", 200, telemetryRequestFor(selectionCommand)));
  }

  private CompletionStage<TelemetryResponse> removecustomerLocation(SelectionCommand selectionCommand) {
    return grpcClient
        .removeCustomerLocation(RemoveCustomerLocationCommand.newBuilder().setCustomerLocationId(getCustomerId(selectionCommand)).build())
        .thenApply(e -> new TelemetryResponse("Customer location deleted", 200, telemetryRequestFor(selectionCommand)));
  }

  private static String getCustomerId(SelectionCommand selectionCommand) {
    return WorldMap.entityIdOf(selectionCommand.region);
  }

  private static TelemetryRequest telemetryRequestFor(SelectionCommand selectionCommand) {
    return new TelemetryRequest(selectionCommand.action.name(), selectionCommand.region);
  }
}
