package woe.simulator;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.DispatcherSelector;
import akka.actor.typed.Terminated;
import akka.actor.typed.javadsl.Behaviors;
import akka.cluster.sharding.typed.javadsl.ClusterSharding;
import akka.cluster.sharding.typed.javadsl.Entity;
import akka.management.cluster.bootstrap.ClusterBootstrap;
import akka.management.javadsl.AkkaManagement;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

public class Main {
  public static Behavior<Void> create() {
    return Behaviors.setup(
        context -> Behaviors.receive(Void.class)
            .onSignal(Terminated.class, signal -> Behaviors.stopped())
            .build()
    );
  }

  public static void main(String[] args) {
    ActorSystem<?> actorSystem = ActorSystem.create(Main.create(), "woe-sim");
    awsCassandraTruststoreHack(actorSystem);
    startClusterBootstrap(actorSystem);
    startHttpServer(actorSystem);
    startRegionClusterSharding(actorSystem);
    startRegionPinger(actorSystem);
  }

  // Copies the truststore file to the local container file system.
  // Cassandra code does not read from classpath resource.
  private static void awsCassandraTruststoreHack(ActorSystem<?> actorSystem) {
    final String filename = "cassandra-truststore.jks";
    final InputStream inputStream = actorSystem.getClass().getClassLoader().getResourceAsStream(filename);
    final Path target = Paths.get(filename);
    if (inputStream != null) {
      try {
        Files.copy(inputStream, target);
      } catch (IOException e) {
        actorSystem.log().error(String.format("Unable to copy '%s'", filename), e);
      }
    }
  }

  private static void startClusterBootstrap(ActorSystem<?> actorSystem) {
    AkkaManagement.get(actorSystem.classicSystem()).start();
    ClusterBootstrap.get(actorSystem.classicSystem()).start();
  }

  static void startHttpServer(ActorSystem<?> actorSystem) {
    try {
      final String host = InetAddress.getLocalHost().getHostName();
      final int port = actorSystem.settings().config().getInt("woe.simulator.http.server.port");
      HttpServer.start(host, port, actorSystem);
    } catch (UnknownHostException e) {
      actorSystem.log().error("Http server start failure.", e);
    }
  }

  private static void startRegionClusterSharding(ActorSystem<?> actorSystem) {
    final ClusterSharding clusterSharding = ClusterSharding.get(actorSystem);
    final Clients clients = new Clients(actorSystem);
    clusterSharding.init(
        Entity.of(
            Region.entityTypeKey,
            entityContext ->
                Region.create(entityContext.getEntityId(), clusterSharding, clients)
        ).withEntityProps(DispatcherSelector.fromConfig("woe.twin.region-entity-dispatcher"))
    );
  }

  private static void startRegionPinger(ActorSystem<?> actorSystem) {
    final Duration interval = Duration.parse(actorSystem.settings().config().getString("woe.twin.region-ping-interval-iso-8601"));
    RegionPinger.start(actorSystem, interval);
  }
}

