package oti.simulator;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

class RegionPinger extends AbstractBehavior<RegionPinger.Command> {
  interface Command {
  }

  static Behavior<Command> create() {
    return Behaviors.setup(RegionPinger::new);
  }

  private RegionPinger(ActorContext<Command> context) {
    super(context);
  }

  @Override
  public Receive<Command> createReceive() {
    return null;
  }

}
