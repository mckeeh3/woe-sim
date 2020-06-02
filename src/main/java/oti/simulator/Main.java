package oti.simulator;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.Terminated;
import akka.actor.typed.javadsl.Behaviors;
import akka.cluster.sharding.typed.javadsl.ClusterSharding;
import akka.cluster.sharding.typed.javadsl.Entity;
import akka.persistence.typed.PersistenceId;

public class Main {
  public static Behavior<Void> create() {
    return Behaviors.setup(
        context -> {

          return Behaviors.receive(Void.class)
              .onSignal(Terminated.class, signal -> Behaviors.stopped())
              .build();
        }
    );
  }

  public static void main(String[] args) {
    ActorSystem<Void> actorSystem = ActorSystem.create(Main.create(), "OTI Simulator");
    ClusterSharding clusterSharding = ClusterSharding.get(actorSystem);

    clusterSharding.init(
        Entity.of(
            Region.entityTypeKey,
            entityContext ->
                Region.create(entityContext.getEntityId(), clusterSharding)
        )
    );
    PersistenceId.ofUniqueId("");
  }
}

// level 0 - 2 regions on either side of lng 0 meridian, 180 lat x 360 / 2 lng, 180 lat x 180 lng
// level 1 - 4 regions 180 / 2, 90 lat x 90 lng
// level 2 - 9 regions 90 / 3 x 90 / 3, 30 lat x 30 lng
// level 3 - 9 regions 30 / 3 x 30 / 3, 10 lat x 10 lng
// level 4 - 4 regions 10 / 2 x 10 / 2, 5 lat x 5 lng

