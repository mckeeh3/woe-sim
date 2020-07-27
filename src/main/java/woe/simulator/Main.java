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

import java.net.InetAddress;
import java.net.UnknownHostException;
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
    startClusterBootstrap(actorSystem);
    startHttpServer(actorSystem);
    startRegionClusterSharding(actorSystem);
    startRegionPinger(actorSystem);
  }

  private static void startClusterBootstrap(ActorSystem<?> actorSystem) {
    AkkaManagement.get(actorSystem.classicSystem()).start();
    ClusterBootstrap.get(actorSystem.classicSystem()).start();
  }

  static void startHttpServer(ActorSystem<?> actorSystem) {
    try {
      String host = InetAddress.getLocalHost().getHostName();
      int port = actorSystem.settings().config().getInt("woe.simulator.http.server.port");
      HttpServer.start(host, port, actorSystem);
    } catch (UnknownHostException e) {
      actorSystem.log().error("Http server start failure.", e);
    }
  }

  private static void startRegionClusterSharding(ActorSystem<?> actorSystem) {
    ClusterSharding clusterSharding = ClusterSharding.get(actorSystem);
    clusterSharding.init(
        Entity.of(
            Region.entityTypeKey,
            entityContext ->
                Region.create(entityContext.getEntityId(), clusterSharding)
        ).withEntityProps(DispatcherSelector.fromConfig("woe.twin.region-entity-dispatcher"))
    );
  }

  private static void startRegionPinger(ActorSystem<?> actorSystem) {
    final Duration interval = Duration.parse(actorSystem.settings().config().getString("woe.twin.region-ping-interval-iso-8601"));
    RegionPinger.start(actorSystem, interval);
  }
}

