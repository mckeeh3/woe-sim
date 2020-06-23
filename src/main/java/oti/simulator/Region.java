package oti.simulator;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.cluster.sharding.typed.javadsl.ClusterSharding;
import akka.cluster.sharding.typed.javadsl.EntityRef;
import akka.cluster.sharding.typed.javadsl.EntityTypeKey;
import akka.persistence.typed.PersistenceId;
import akka.persistence.typed.SnapshotSelectionCriteria;
import akka.persistence.typed.javadsl.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;

import java.util.List;
import java.util.Objects;

class Region extends EventSourcedBehavior<Region.Command, Region.Event, Region.State> {
  final String entityId;
  final WorldMap.Region region;
  final ClusterSharding clusterSharding;
  final ActorContext<Command> actorContext;
  final HttpClient httpClient;
  static final EntityTypeKey<Command> entityTypeKey = EntityTypeKey.create(Command.class, Region.class.getSimpleName());

  static Behavior<Command> create(String entityId, ClusterSharding clusterSharding) {
    return Behaviors.setup(actorContext -> new Region(entityId, clusterSharding, actorContext));
  }

  private Region(String entityId, ClusterSharding clusterSharding, ActorContext<Command> actorContext) {
    super(PersistenceId.of(entityTypeKey.name(), entityId));
    this.entityId = entityId;
    this.region = WorldMap.regionForEntityId(entityId);
    this.clusterSharding = clusterSharding;
    this.actorContext = actorContext;
    this.httpClient = new HttpClient(actorContext.getSystem());
  }

  @Override
  public State emptyState() {
    return new State(region);
  }

  @Override
  public CommandHandler<Command, Event, State> commandHandler() {
    return newCommandHandlerBuilder().forAnyState()
        .onCommand(SelectionCreate.class, this::onSelectionCreate)
        .onCommand(SelectionDelete.class, this::onSelectionDelete)
        .onCommand(SelectionHappy.class, this::onSelectionHappyOrSad)
        .onCommand(SelectionSad.class, this::onSelectionHappyOrSad)
        .build();
  }

  @Override
  public EventHandler<State, Event> eventHandler() {
    return newEventHandlerBuilder().forAnyState()
        .onEvent(SelectionAccepted.class, State::selectionAccepted)
        .build();
  }

  @Override
  public Recovery recovery() {
    return Recovery.withSnapshotSelectionCriteria(SnapshotSelectionCriteria.none());
  }

  private Effect<Event, State> onSelectionCreate(State state, SelectionCreate selectionCreate) {
    if (state.doesSelectionOverlapRegion(selectionCreate)) {
      if (state.isPartialSelection(selectionCreate) && state.isNotSelected()) {
        return acceptSelection(selectionCreate);
      } else if (state.isFullSelection(selectionCreate) && (state.isNotSelected() || state.isPartiallySelected())) {
        return acceptSelection(selectionCreate);
      } else {
        forwardSelectionToSubRegions(state, selectionCreate);
        return Effect().none();
      }
    } else {
      return Effect().none();
    }
  }

  private Effect<Event, State> onSelectionDelete(State state, SelectionDelete selectionDelete) {
    if (state.doesSelectionOverlapRegion(selectionDelete)) {
      if (state.isFullySelected()) {
        return acceptSelection(selectionDelete);
      } else if (state.isFullSelection(selectionDelete) & state.isPartiallySelected()) {
        return acceptSelection(selectionDelete);
      } else {
        forwardSelectionToSubRegions(state, selectionDelete);
        return Effect().none();
      }
    } else {
      return Effect().none();
    }
  }

  private Effect<Event, State> onSelectionHappyOrSad(State state, SelectionCommand selectionHappyOrSad) {
    if (state.isPartiallySelected() || state.isFullySelected()) {
      if (state.region.isDevice()) {
        notifyTwin(selectionHappyOrSad.with(state.region));
      } else {
        forwardSelectionToSubRegions(state, selectionHappyOrSad);
      }
    }
    return Effect().none();
  }

  private Effect<Event, State> acceptSelection(SelectionCommand selectionCommand) {
    SelectionAccepted selectionAccepted = new SelectionAccepted(selectionCommand.action, selectionCommand.region);
    return Effect().persist(selectionAccepted)
        .thenRun(newState -> eventPersisted(newState, selectionCommand));
  }

  private void eventPersisted(State state, SelectionCommand selectionCommand) {
    if (region.isDevice() && selectionCommand.replyTo != null) {
      selectionCommand.replyTo.tell(selectionCommand); // hack for unit testing
    }
    if (state.region.isDevice()) {
      notifyTwin(selectionCommand.with(state.region));
    } else {
      forwardSelectionToSubRegions(state, selectionCommand);
    }
  }

  private void notifyTwin(SelectionCommand selectionCommand) {
    httpClient.post(selectionCommand)
        .thenAccept(t -> {
          log().debug("{}", t);
          if (t.httpStatusCode != 200) {
            log().warn("Telemetry request failed {}", t);
          }
        });
  }

  private void forwardSelectionToSubRegions(State state, SelectionCommand selectionCommand) {
    List<WorldMap.Region> subRegions = WorldMap.subRegionsFor(state.region);
    subRegions.forEach(region -> {
      EntityRef<Command> entityRef = clusterSharding.entityRefFor(entityTypeKey, WorldMap.entityIdOf(region));
      entityRef.tell(selectionCommand);
    });
  }

  interface Command extends CborSerializable {
  }

  public abstract static class SelectionCommand implements Command {
    enum Action {
      create, delete, happy, sad
    }

    public final Action action;
    public final WorldMap.Region region;
    public final ActorRef<Command> replyTo;

    public SelectionCommand(Action action, WorldMap.Region region, ActorRef<Command> replyTo) {
      this.action = action;
      this.region = region;
      this.replyTo = replyTo; // used for unit testing
    }

    abstract SelectionCommand with(WorldMap.Region region);

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      SelectionCommand that = (SelectionCommand) o;
      return action == that.action &&
          region.equals(that.region) &&
          Objects.equals(replyTo, that.replyTo);
    }

    @Override
    public int hashCode() {
      return Objects.hash(action, region, replyTo);
    }

    @Override
    public String toString() {
      return String.format("%s[%s, %s]", getClass().getSimpleName(), action, region);
    }
  }

  public static final class SelectionCreate extends SelectionCommand {
    public SelectionCreate(@JsonProperty("region") WorldMap.Region region, @JsonProperty("replyTo") ActorRef<Command> replyTo) {
      super(Action.create, region, replyTo);
    }

    SelectionCreate with(WorldMap.Region region) {
      return new SelectionCreate(region, this.replyTo);
    }
  }

  public static final class SelectionDelete extends SelectionCommand {
    public SelectionDelete(@JsonProperty("region") WorldMap.Region region, @JsonProperty("replyTo") ActorRef<Command> replyTo) {
      super(Action.delete, region, replyTo);
    }

    SelectionDelete with(WorldMap.Region region) {
      return new SelectionDelete(region, this.replyTo);
    }
  }

  public static final class SelectionHappy extends SelectionCommand {
    public SelectionHappy(@JsonProperty("region") WorldMap.Region region, @JsonProperty("replyTo") ActorRef<Command> replyTo) {
      super(Action.happy, region, replyTo);
    }

    SelectionHappy with(WorldMap.Region region) {
      return new SelectionHappy(region, this.replyTo);
    }
  }

  public static final class SelectionSad extends SelectionCommand {
    public SelectionSad(@JsonProperty("region") WorldMap.Region region, @JsonProperty("replyTo") ActorRef<Command> replyTo) {
      super(Action.sad, region, replyTo);
    }

    SelectionSad with(WorldMap.Region region) {
      return new SelectionSad(region, this.replyTo);
    }
  }

  interface Event extends CborSerializable {
  }

  public static final class SelectionAccepted implements Event {
    public final SelectionCommand.Action action;
    public final WorldMap.Region region;

    SelectionAccepted(SelectionCommand.Action action, WorldMap.Region region) {
      this.action = action;
      this.region = region;
    }

    @Override
    public String toString() {
      return String.format("%s[%s, %s]", getClass().getSimpleName(), action, region);
    }
  }

  static final class State implements CborSerializable {
    final WorldMap.Region region;
    Status status;

    State(WorldMap.Region region) {
      this.region = region;
      status = Status.notSelected;
    }

    enum Status {
      notSelected, partiallySelected, fullySelected
    }

    boolean isNotSelected() {
      return Status.notSelected.equals(status);
    }

    boolean isPartiallySelected() {
      return Status.partiallySelected.equals(status);
    }

    boolean isFullySelected() {
      return Status.fullySelected.equals(status);
    }

    boolean doesSelectionOverlapRegion(SelectionCommand selectionCommand) {
      return region.overlaps(selectionCommand.region);
    }

    boolean doesSelectionContainRegion(SelectionCommand selectionCommand) {
      return doesSelectionContainRegion(selectionCommand.region);
    }

    boolean doesSelectionContainRegion(SelectionAccepted selectionAccepted) {
      return doesSelectionContainRegion(selectionAccepted.region);
    }

    private boolean doesSelectionContainRegion(WorldMap.Region region) {
      return region.contains(this.region);
    }

    boolean doesRegionContainSelection(SelectionCommand selectionCommand) {
      return doesSelectionContainRegion(selectionCommand.region);
    }

    boolean doesRegionContainSelection(SelectionAccepted selectionAccepted) {
      return doesRegionContainSelection(selectionAccepted.region);
    }

    private boolean doesRegionContainSelection(WorldMap.Region region) {
      return this.region.contains(region);
    }

    boolean isPartialSelection(SelectionCommand selectionCommand) {
      return region.contains(selectionCommand.region);
    }

    boolean isFullSelection(SelectionCommand selectionCommand) {
      return selectionCommand.region.contains(region);
    }

    State selectionAccepted(SelectionAccepted selectionAccepted) {
      switch (selectionAccepted.action) {
        case create:
          if (doesSelectionContainRegion(selectionAccepted)) {
            status = Status.fullySelected;
          } else if (doesRegionContainSelection(selectionAccepted)) {
            status = Status.partiallySelected;
          }
          break;
        case delete:
          if (doesSelectionContainRegion(selectionAccepted)) {
            status = Status.notSelected;
          } else if (doesRegionContainSelection(selectionAccepted) && isFullySelected()) {
            status = Status.partiallySelected;
          }
          break;
      }
      return this;
    }
  }

  Logger log() {
    return actorContext.getSystem().log();
  }
}

