package woe.simulator;

import static woe.simulator.WorldMap.entityIdOf;
import static woe.simulator.WorldMap.regionForZoom0;

import java.time.Duration;
import java.time.Instant;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.actor.typed.javadsl.TimerScheduler;
import akka.cluster.sharding.typed.javadsl.ClusterSharding;
import akka.cluster.typed.ClusterSingleton;
import akka.cluster.typed.SingletonActor;

class RegionPinger extends AbstractBehavior<RegionPinger.Command> {
  private final ClusterSharding clusterSharding;
  private final Duration interval;

  interface Command {}

  enum Tick implements Command {
    ticktock
  }

  static Behavior<?> create(Duration interval) {
    return Behaviors.<Command>setup(actorContext ->
        Behaviors.withTimers(timer -> new RegionPinger(actorContext, interval, timer)));
  }

  private RegionPinger(ActorContext<Command> actorContext, Duration interval, TimerScheduler<Command> timerScheduler) {
    super(actorContext);
    clusterSharding = ClusterSharding.get(actorContext.getSystem());
    this.interval = interval;
    timerScheduler.startTimerWithFixedDelay(Tick.ticktock, interval);
  }

  @Override
  public Receive<Command> createReceive() {
    return newReceiveBuilder()
        .onMessage(Tick.class, t -> tick())
        .build();
  }

  private Behavior<Command> tick() {
    final var region = regionForZoom0();
    final var entityId = entityIdOf(region);
    final var entityRef = clusterSharding.entityRefFor(Region.entityTypeKey, entityId);
    final var deadline = Instant.now().plus(interval);
    entityRef.tell(new Region.PingPartiallySelected(region, deadline, false, null));
    return this;
  }

  static void start(ActorSystem<?> actorSystem, Duration interval) {
    final var clusterSingleton = ClusterSingleton.get(actorSystem);
    clusterSingleton.init(SingletonActor.of(RegionPinger.create(interval), RegionPinger.class.getSimpleName()));
  }
}
