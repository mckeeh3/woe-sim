package woe.simulator;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;
import akka.cluster.sharding.typed.javadsl.ClusterSharding;
import akka.cluster.sharding.typed.javadsl.EntityRef;
import akka.cluster.typed.ClusterSingleton;
import akka.cluster.typed.SingletonActor;

import java.time.Duration;

import static woe.simulator.WorldMap.entityIdOf;
import static woe.simulator.WorldMap.regionForZoom0;

class RegionPinger extends AbstractBehavior<RegionPinger.Command> {
  private final ClusterSharding clusterSharding;

  interface Command {
  }

  enum Tick implements Command {
    ticktock
  }

  static Behavior<?> create(Duration interval) {
    return Behaviors.<Command>setup(actorContext ->
        Behaviors.<Command>withTimers(timer -> new RegionPinger(actorContext, interval, timer)));
  }

  private RegionPinger(ActorContext<Command> actorContext, Duration interval, TimerScheduler<Command> timerScheduler) {
    super(actorContext);
    clusterSharding = ClusterSharding.get(actorContext.getSystem());
    timerScheduler.startTimerWithFixedDelay(Tick.ticktock, interval);
  }

  @Override
  public Receive<Command> createReceive() {
    return newReceiveBuilder()
        .onMessage(Tick.class, t -> tick())
        .build();
  }

  private Behavior<Command> tick() {
    final WorldMap.Region region = regionForZoom0();
    final String entityId = entityIdOf(region);
    final EntityRef<Region.Command> entityRef = clusterSharding.entityRefFor(Region.entityTypeKey, entityId);
    entityRef.tell(new Region.PingPartiallySelected(region, null));
    return this;
  }

  static void start(ActorSystem<?> actorSystem, Duration interval) {
    final ClusterSingleton clusterSingleton = ClusterSingleton.get(actorSystem);
    clusterSingleton.init(SingletonActor.of(RegionPinger.create(interval), RegionPinger.class.getSimpleName()));
  }
}
