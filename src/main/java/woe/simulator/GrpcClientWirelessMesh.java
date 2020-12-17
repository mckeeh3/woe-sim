package woe.simulator;

import java.util.concurrent.CompletionStage;
import java.util.stream.IntStream;

import com.google.protobuf.Empty;

import org.slf4j.Logger;

import akka.actor.typed.ActorSystem;
import akka.grpc.GrpcClientSettings;
import service.AkkaServerlessDemo.ActivateDeviceCommand;
import service.AkkaServerlessDemo.AddCustomerLocationCommand;
import service.AkkaServerlessDemo.GetDevicesCommand;
import service.AkkaServerlessDemo.RemoveCustomerLocationCommand;
import service.WirelessMeshServiceClient;
import woe.simulator.Region.SelectionCommand;
import woe.simulator.Telemetry.TelemetryRequest;
import woe.simulator.Telemetry.TelemetryResponse;

public class GrpcClientWirelessMesh implements Client {
  private final ActorSystem<?> actorSystem;
  private final WirelessMeshServiceClient grpcClient;

  public GrpcClientWirelessMesh(ActorSystem<?> actorSystem, String host, int port) {
    this.actorSystem = actorSystem;
    final GrpcClientSettings grpcClientSettings = GrpcClientSettings.connectToServiceAt(host, port, actorSystem)
        .withTls(true);
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
        return getCustomerLocationDevices(selectionCommand);
      case sad:
        return getCustomerLocationDevices(selectionCommand);
      case ping:
        return getCustomerLocationDevices(selectionCommand);
    }
    return null;
  }

  private CompletionStage<TelemetryResponse> addCustomerLocation(SelectionCommand selectionCommand) {
    log().info("{}", selectionCommand);
    return grpcClient
        .addCustomerLocation(
            AddCustomerLocationCommand.newBuilder().setCustomerLocationId(getCustomerId(selectionCommand)).build())
        .thenApply(e -> {
          log().info("{} {}", selectionCommand.action, getCustomerId(selectionCommand));
          addDevices(selectionCommand);
          return new TelemetryResponse("Customer location created", 200, telemetryRequestFor(selectionCommand));
        });
  }

  private CompletionStage<TelemetryResponse> removeCustomerLocation(SelectionCommand selectionCommand) {
    return grpcClient
        .removeCustomerLocation(
            RemoveCustomerLocationCommand.newBuilder().setCustomerLocationId(getCustomerId(selectionCommand)).build())
        .thenApply(e -> {
          log().info("{} {}", selectionCommand.action, getCustomerId(selectionCommand));
          return new TelemetryResponse("Customer location deleted", 200, telemetryRequestFor(selectionCommand));
        });
  }

  private CompletionStage<TelemetryResponse> getCustomerLocationDevices(SelectionCommand selectionCommand) {
    return grpcClient
        .getDevices(GetDevicesCommand.newBuilder().setCustomerLocationId(getCustomerId(selectionCommand)).build())
        .thenApply(devices -> {
          final var msg = String.format("Get location devices (%d)", devices.getDeviceCount());
          return new TelemetryResponse(msg, 200, telemetryRequestFor(selectionCommand));
        });
  }

  private static String getCustomerId(SelectionCommand selectionCommand) {
    return WorldMap.entityIdOf(selectionCommand.region);
  }

  private static TelemetryRequest telemetryRequestFor(SelectionCommand selectionCommand) {
    return new TelemetryRequest(selectionCommand.action.name(), selectionCommand.region);
  }

  private void addDevices(SelectionCommand selectionCommand) {
    var deviceCount = Math.round((float) Math.random() * 3 + 1);
    IntStream.range(1, deviceCount).forEach(n -> addDevice(n, selectionCommand));
  }

  private void addDevice(int n, SelectionCommand selectionCommand) {
    ActivateDeviceCommand activateDeviceCommand = ActivateDeviceCommand.newBuilder()
      .setCustomerLocationId(getCustomerId(selectionCommand))
      .setDeviceId(String.format("Room-%d", n))
      .build();
      grpcClient.activateDevice(activateDeviceCommand)
        .thenApply(r -> {
          log().info("{} {}", activateDeviceCommand.getCustomerLocationId(), activateDeviceCommand.getDeviceId());
          return Empty.newBuilder().build();
        });
  }

  private Logger log() {
    return actorSystem.log();
  }
}
