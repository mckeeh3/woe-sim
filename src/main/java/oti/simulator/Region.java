package oti.simulator;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.cluster.sharding.typed.javadsl.ClusterSharding;
import akka.cluster.sharding.typed.javadsl.EntityRef;
import akka.cluster.sharding.typed.javadsl.EntityTypeKey;
import akka.http.javadsl.Http;
import akka.http.javadsl.model.HttpRequest;
import akka.persistence.typed.PersistenceId;
import akka.persistence.typed.SnapshotSelectionCriteria;
import akka.persistence.typed.javadsl.*;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

class Region extends EventSourcedBehavior<Region.Command, Region.Event, Region.State> {
  final WorldMap.Region region;
  final ClusterSharding clusterSharding;
  final ActorContext<Command> actorContext;
  static final EntityTypeKey<Command> entityTypeKey = EntityTypeKey.create(Command.class, Region.class.getSimpleName());

  static Behavior<Command> create(String entityId, ClusterSharding clusterSharding) {
    WorldMap.Region region = WorldMap.regionForEntityId(entityId);
    return Behaviors.setup(actorContext -> new Region(region, clusterSharding, actorContext));
  }

  private Region(WorldMap.Region region, ClusterSharding clusterSharding, ActorContext<Command> actorContext) {
    super(PersistenceId.of(entityTypeKey.name(), WorldMap.entityIdOf(region)));
    this.region = region;
    this.clusterSharding = clusterSharding;
    this.actorContext = actorContext;
  }

  interface Command extends CborSerializable {
  }

  enum SelectionAction {
    create, delete, happy, sad
  }

  public abstract static class SelectionCommand implements Command {
    public final SelectionAction action;
    public final WorldMap.Region region;
    public final ActorRef<Command> replyTo;

    @JsonCreator
    public SelectionCommand(@JsonProperty("action") SelectionAction action, @JsonProperty("region") WorldMap.Region region,
                            @JsonProperty("replyTo") ActorRef<Command> replyTo) {
      this.action = action;
      this.region = region;
      this.replyTo = replyTo;
    }

    @Override
    public String toString() {
      return String.format("%s[%s, %s]", getClass().getSimpleName(), action, region);
    }
  }

  public static final class SelectionCreate extends SelectionCommand {
    @JsonCreator
    public SelectionCreate(@JsonProperty("region") WorldMap.Region region, @JsonProperty("replyTo") ActorRef<Command> replyTo) {
      super(SelectionAction.create, region, replyTo);
    }
  }

  static final class SelectionDelete extends SelectionCommand {
    SelectionDelete(WorldMap.Region region, ActorRef<Command> replyTo) {
      super(SelectionAction.delete, region, replyTo);
    }
  }

  static final class SelectionHappy extends SelectionCommand {
    SelectionHappy(WorldMap.Region region, ActorRef<Command> replyTo) {
      super(SelectionAction.happy, region, replyTo);
    }
  }

  static final class SelectionSad extends SelectionCommand {
    SelectionSad(WorldMap.Region region, ActorRef<Command> replyTo) {
      super(SelectionAction.sad, region, replyTo);
    }
  }

  interface Event extends CborSerializable {
  }

  public static final class SelectionAccepted implements Event {
    public final SelectionAction action;
    public final WorldMap.Region region;

    @JsonCreator
    SelectionAccepted(@JsonProperty("action") SelectionAction action, @JsonProperty("region") WorldMap.Region region) {
      this.action = action;
      this.region = region;
    }

    @Override
    public String toString() {
      return String.format("%s[%s, %s]", getClass().getSimpleName(), action, region);
    }
  }

  static final class Selections implements CborSerializable {
    final WorldMap.Region region;
    final List<WorldMap.Region> currentSelections = new ArrayList<>();

    Selections(WorldMap.Region region) {
      this.region = region;
    }

    void add(WorldMap.Region region) {
      if (isSuperRegion(region)) {
        currentSelections.clear();
        currentSelections.add(region);
      } else if (isSubRegion(region)) {
        currentSelections.removeIf(region::contains);
        currentSelections.add(region);
      }
    }

    boolean isSuperRegion(WorldMap.Region region) {
      return region.contains(this.region);
    }

    boolean isSubRegion(WorldMap.Region region) {
      return this.region.contains(region) && isSelectionVisible(region);
    }

    private boolean isSelectionVisible(WorldMap.Region region) {
      return currentSelections.stream().noneMatch(currentRegion -> currentRegion.contains(region));
    }
  }

  static final class State implements CborSerializable {
    final WorldMap.Region region;
    final Selections selections;

    State(WorldMap.Region region) {
      this.region = region;
      selections = new Selections(region);
    }

    boolean isNewSelection(SelectionCommand selectionCommand) {
      return selections.isSuperRegion(selectionCommand.region) || selections.isSubRegion(selectionCommand.region);
    }

    State addSelection(SelectionAccepted selectionAccepted) {
      selections.add(selectionAccepted.region);
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
        .onCommand(SelectionCommand.class, this::onAddSelection)
        .build();
  }

  private Effect<Event, State> onAddSelection(State state, SelectionCommand selectionCommand) {
    if (state.isNewSelection(selectionCommand)) {
      log().debug("{} accepted {}", region, selectionCommand);
      SelectionAccepted selectionAccepted = new SelectionAccepted(selectionCommand.action, selectionCommand.region);
      return Effect().persist(selectionAccepted)
          .thenRun(newState -> eventPersisted(newState, selectionCommand));
    } else {
      log().debug("{} rejected {}", region, selectionCommand);
    }
    return Effect().none();
  }

  private void eventPersisted(State state, SelectionCommand selectionCommand) {
    if (region.zoom == WorldMap.zoomMax && selectionCommand.replyTo != null) {
      selectionCommand.replyTo.tell(selectionCommand);
    }
    notifyTwin(selectionCommand);
    forwardSelectionToSubRegions(state, selectionCommand);
  }

  private void notifyTwin(SelectionCommand selectionCommand) {
    // TODO
    //final Http http = Http.get(actorContext.getSystem().classicSystem());
    //actorContext.pipeToSelf(http.singleRequest(HttpRequest.create("")), (res, t) -> {
    //});
  }

  private void forwardSelectionToSubRegions(State state, SelectionCommand selectionCommand) {
    List<WorldMap.Region> subRegions = WorldMap.subRegionsFor(state.region);
    subRegions.forEach(region -> {
      EntityRef<Command> entityRef = clusterSharding.entityRefFor(entityTypeKey, WorldMap.entityIdOf(region));
      entityRef.tell(selectionCommand);
    });
  }

  @Override
  public EventHandler<State, Event> eventHandler() {
    return newEventHandlerBuilder().forAnyState()
        .onEvent(SelectionAccepted.class, State::addSelection)
        .build();
  }

  @Override
  public Recovery recovery() {
    return Recovery.withSnapshotSelectionCriteria(SnapshotSelectionCriteria.none());
  }

  Logger log() {
    return actorContext.getSystem().log();
  }
}

