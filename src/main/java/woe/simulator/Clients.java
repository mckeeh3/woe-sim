package woe.simulator;

import akka.actor.typed.ActorSystem;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class Clients {
  private final ActorSystem<?> actorSystem;
  private final List<Client> clients;

  Clients(ActorSystem<?> actorSystem) {
    this.actorSystem = actorSystem;
    clients = configuredClients(actorSystem);
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

  static List<Client> configuredClients(ActorSystem<?> actorSystem) {
    final var clients = new ArrayList<Client>();

    clientConfigurationList(actorSystem).forEach(clientConfiguration -> {
      try {
        clients.add((Client) Class.forName(clientConfiguration.name)
            .getConstructor(ActorSystem.class, String.class, int.class)
            .newInstance(actorSystem, clientConfiguration.host, clientConfiguration.port));
        actorSystem.log().info("Using telemetry client {}", clientConfiguration);
      } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
        throw new ClientsException(String.format("Invalid client class '%s'.", clientConfiguration), e);
      }
    });

    return clients;
  }

  static List<ClientConfiguration> clientConfigurationList(ActorSystem<?> actorSystem) {
    final var clientsStr = actorSystem.settings().config().getString("woe.twin.telemetry.clients");
    final var clients = Arrays.asList(clientsStr.split("\\s*,\\s*"));
    final var clientConfigurationList = new ArrayList<ClientConfiguration>();
    clients.forEach(endpoint -> {
      final var p = endpoint.split(":");
      if (p.length != 3) {
        throw new ClientsException(String.format("Illegal telemetry client endpoint syntax '%s', expected 'client-class-name:host:port.", endpoint));
      }
      clientConfigurationList.add(new ClientConfiguration(p[0], p[1], Integer.parseInt(p[2])));
    });
    return clientConfigurationList;
  }

  static class ClientConfiguration {
    final String name;
    final String host;
    final int port;

    ClientConfiguration(String name, String host, int port) {
      this.name = name;
      this.host = host;
      this.port = port;
    }

    @Override
    public String toString() {
      return String.format("%s[%s, %s, %d]", getClass().getSimpleName(), name, host, port);
    }
  }

  private static class ClientsException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    ClientsException(String message) {
      super(message);
    }

    ClientsException(String message, Throwable t) {
      super(message, t);
    }
  }
}
