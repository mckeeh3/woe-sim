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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
    public final Selection selection;
    public final ActorRef<Command> replyTo;

    @JsonCreator
    public SelectionCommand(@JsonProperty("action") SelectionAction action, @JsonProperty("selection") Selection selection,
                            @JsonProperty("replyTo") ActorRef<Command> replyTo) {
      this.action = action;
      this.selection = selection;
      this.replyTo = replyTo;
    }

    @Override
    public String toString() {
      return String.format("%s[%s, %s]", getClass().getSimpleName(), action, selection);
    }
  }

  public static final class SelectionCreate extends SelectionCommand {
    @JsonCreator
    public SelectionCreate(@JsonProperty("selection") Selection selection, @JsonProperty("replyTo") ActorRef<Command> replyTo) {
      super(SelectionAction.create, selection, replyTo);
    }
  }

  static final class SelectionDelete extends SelectionCommand {
    SelectionDelete(Selection selection, ActorRef<Command> replyTo) {
      super(SelectionAction.delete, selection, replyTo);
    }
  }

  static final class SelectionHappy extends SelectionCommand {
    SelectionHappy(Selection selection, ActorRef<Command> replyTo) {
      super(SelectionAction.happy, selection, replyTo);
    }
  }

  static final class SelectionSad extends SelectionCommand {
    SelectionSad(Selection selection, ActorRef<Command> replyTo) {
      super(SelectionAction.sad, selection, replyTo);
    }
  }

  interface Event extends CborSerializable {
  }

  public static final class SelectionAccepted implements Event {
    public final SelectionAction action;
    public final Selection selection;

    @JsonCreator
    SelectionAccepted(@JsonProperty("action") SelectionAction action, @JsonProperty("selection") Selection selection) {
      this.action = action;
      this.selection = selection;
    }

    @Override
    public String toString() {
      return String.format("%s[%s, %s]", getClass().getSimpleName(), action, selection);
    }
  }

  public static final class Selection implements CborSerializable {
    public final WorldMap.Region region;

    @JsonCreator
    public Selection(@JsonProperty("region") WorldMap.Region region) {
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
    final List<Selection> current = new ArrayList<>();

    Selections(WorldMap.Region region) {
      this.region = region;
    }

    void add(Selection selection) {
      if (isSuperRegion(selection)) {
        current.clear();
        current.add(selection);
      } else if (isSubRegion(selection)) {
        current.removeIf(currentSelection -> selection.region.contains(currentSelection.region));
        current.add(selection);
      }
    }

    boolean isSuperRegion(Selection selection) {
      return selection.region.contains(region);
    }

    boolean isSubRegion(Selection selection) {
      return region.contains(selection.region) && isSelectionVisible(selection);
    }

    private boolean isSelectionVisible(Selection selection) {
      return current.stream().noneMatch(currentSelection -> currentSelection.region.contains(selection.region));
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
      return selections.isSuperRegion(selectionCommand.selection) || selections.isSubRegion(selectionCommand.selection);
    }

    State addSelection(SelectionAccepted selectionAccepted) {
      selections.add(selectionAccepted.selection);
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
      SelectionAccepted selectionAccepted = new SelectionAccepted(selectionCommand.action, selectionCommand.selection);
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
    final Http http = Http.get(actorContext.getSystem().classicSystem());
    actorContext.pipeToSelf(http.singleRequest(HttpRequest.create("")), (res, t) -> {
      return null;
    });
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

