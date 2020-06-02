package oti.simulator;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.cluster.sharding.typed.javadsl.ClusterSharding;
import akka.cluster.sharding.typed.javadsl.EntityTypeKey;
import akka.persistence.typed.PersistenceId;
import akka.persistence.typed.javadsl.CommandHandler;
import akka.persistence.typed.javadsl.Effect;
import akka.persistence.typed.javadsl.EventHandler;
import akka.persistence.typed.javadsl.EventSourcedBehavior;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

class Region extends EventSourcedBehavior<Region.Command, Region.Event, Region.State> {
  final WorldMap.Region region;
  final ClusterSharding clusterSharding;
  final ActorContext<Command> actorContext;
  static final EntityTypeKey<Command> entityTypeKey = EntityTypeKey.create(Command.class, "Region");

  static Behavior<Command> create(String entityId, ClusterSharding clusterSharding) {
    WorldMap.Region region = WorldMap.regionForEntityId(entityId);
    return Behaviors.setup(actorContext -> new Region(region, clusterSharding, actorContext));
  }

  private Region(WorldMap.Region region, ClusterSharding clusterSharding, ActorContext<Command> actorContext) {
    super(WorldMap.persistenceIdOf(region));
    this.region = region;
    this.clusterSharding = clusterSharding;
    this.actorContext = actorContext;
  }

  interface Command extends CborSerializable {
  }

  static final class AddSelection implements Command {
    final Selection selection;

    AddSelection(Selection selection) {
      this.selection = selection;
    }
  }

  interface Event extends CborSerializable {
  }

  static final class Selection implements Event {
    final WorldMap.Region region;

    Selection(WorldMap.Region region) {
      this.region = region;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Selection selection = (Selection) o;
      return Objects.equals(region, selection.region);
    }

    @Override
    public int hashCode() {
      return Objects.hash(region);
    }

    @Override
    public String toString() {
      return String.format("%s[%s]", getClass().getSimpleName(), region);
    }
  }

  static final class Selections implements CborSerializable {
    final WorldMap.Region region;
    final List<Selection> selections = new ArrayList<>();

    Selections(WorldMap.Region region) {
      this.region = region;
    }

    void add(Selection selection) {
      if (isSuperRegion(selection)) {
        selections.clear();
        selections.add(selection);
      } else if (isSubRegion(selection)) {
        selections.add(selection);
      }
    }

    boolean isSuperRegion(Selection selection) {
      return selection.region.contains(region);
    }

    boolean isSubRegion(Selection selection) {
      return region.contains(selection.region);
    }
  }

  static final class State implements CborSerializable {
    final WorldMap.Region region;
    final Selections selections;

    State(WorldMap.Region region) {
      this.region = region;
      selections = new Selections(region);
    }

    boolean isNewSelection(AddSelection addSelection) {
      return selections.isSuperRegion(addSelection.selection) || selections.isSubRegion(addSelection.selection);
    }

    State addSelection(Selection selection) {
      selections.add(selection);
      return this;
    }
  }

  @Override
  public State emptyState() {
    return new State(region);
  }

  @Override
  public CommandHandler<Command, Event, State> commandHandler() {
    return newCommandHandlerBuilder().forAnyState()
        .onCommand(AddSelection.class, this::onAddSelection)
        .build();
  }

  private Effect<Event, State> onAddSelection(State state, AddSelection addSelection) {
    if (state.isNewSelection(addSelection)) {
      return Effect().persist(addSelection.selection)
          .thenRun(newState -> forwardSelectionToSubRegions(newState, addSelection));
    }
    return Effect().none();
  }

  private void forwardSelectionToSubRegions(State state, AddSelection addSelection) {
    List<WorldMap.Region> subRegions = WorldMap.subRegionFor(state.region);
    subRegions.forEach(region -> {
      PersistenceId persistenceId = WorldMap.persistenceIdOf(region);
      //clusterSharding.
    });
  }

  @Override
  public EventHandler<State, Event> eventHandler() {
    return newEventHandlerBuilder().forAnyState()
        .onEvent(Selection.class, State::addSelection)
        .build();
  }
}

