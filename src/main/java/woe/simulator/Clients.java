package woe.simulator;

import akka.actor.typed.ActorSystem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class Clients {
  private final ActorSystem<?> actorSystem;
  private final List<Client> clients;

  public Clients(ActorSystem<?> actorSystem) {
    this.actorSystem = actorSystem;
    clients = clients(actorSystem);
  }

  void post(Region.SelectionCommand selectionCommand) {
    clients.forEach(client ->
        client.post(selectionCommand)
            .thenAccept(t -> {
              if (t.httpStatusCode != 200) {
                actorSystem.log().warn("Telemetry request failed {}", t);
              }
            }));
  }

  private static List<Client> clients(ActorSystem<?> actorSystem) {
    final List<Client> clients = new ArrayList<>();

    endPoints(actorSystem).forEach(clientInfo -> {
      if ("http".equalsIgnoreCase(clientInfo.protocol)) {
        clients.add(new HttpClient(actorSystem, clientInfo.host, clientInfo.port));
        actorSystem.log().info("Using HTTP client {}", clientInfo);
      } else if ("grpc".equalsIgnoreCase(clientInfo.protocol)) {
        clients.add(new GrpcClient(actorSystem, clientInfo.host, clientInfo.port));
        actorSystem.log().info("Using gRPC client {}", clientInfo);
      } else {
        throw new RuntimeException(String.format("Invalid client protocol '%s', must be either 'http' or 'grpc'.", clientInfo));
      }
    });
    return clients;
  }

  private static List<ClientInfo> endPoints(ActorSystem<?> actorSystem) {
    final String clientsStr = actorSystem.settings().config().getString("woe.twin.servers");
    final List<String> clients = Arrays.asList(clientsStr.split(".,."));
    final List<ClientInfo> clientInfoList = new ArrayList<>();
    clients.forEach(endpoint -> {
      final String[] p = endpoint.split(":");
      if (p.length != 3) {
        throw new RuntimeException(String.format("Illegal client endpoint syntax '%s', expected 'http|grpc:host:port.", endpoint));
      }
      clientInfoList.add(new ClientInfo(p[0], p[1], Integer.parseInt(p[2])));
    });
    return clientInfoList;
  }

  private static class ClientInfo {
    final String protocol;
    final String host;
    final int port;

    private ClientInfo(String protocol, String host, int port) {
      this.protocol = protocol;
      this.host = host;
      this.port = port;
    }

    @Override
    public String toString() {
      return String.format("%s[%s, %s, %d]", getClass().getSimpleName(), protocol, host, port);
    }
  }
}
