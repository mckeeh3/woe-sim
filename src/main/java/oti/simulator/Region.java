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
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

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
        .onEvent(SelectionAccepted.class, State::addSelection)
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
        notifyTwin(state, selectionHappyOrSad);
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

  private Effect<Event, State> onSelectionOLD(State state, SelectionCommand selectionCommand) {
    if (state.isNewSelection(selectionCommand)) {
      log().debug("{} accepted {}", region, selectionCommand);
      SelectionAccepted selectionAccepted = new SelectionAccepted(selectionCommand.action, selectionCommand.region);
      return Effect().persist(selectionAccepted)
          .thenRun(newState -> eventPersisted(newState, selectionCommand));
    } else {
      return Effect().none();
    }
  }

  private void eventPersisted(State state, SelectionCommand selectionCommand) {
    if (region.isDevice() && selectionCommand.replyTo != null) {
      selectionCommand.replyTo.tell(selectionCommand); // hack for unit testing
    }
    if (state.region.isDevice()) {
      notifyTwin(state, selectionCommand);
    } else {
      forwardSelectionToSubRegions(state, selectionCommand);
    }
  }

  private void notifyTwin(State state, SelectionCommand selectionCommand) {
    if (state.region.isDevice()) {
      httpClient.post(selectionCommand);
    }
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

    @JsonCreator
    public SelectionCommand(@JsonProperty("action") Action action, @JsonProperty("region") WorldMap.Region region,
                            @JsonProperty("replyTo") ActorRef<Command> replyTo) {
      this.action = action;
      this.region = region;
      this.replyTo = replyTo; // used for unit testing
    }

    boolean isCreate() {
      return Action.create.equals(action);
    }

    boolean isDelete() {
      return Action.delete.equals(action);
    }

    @Override
    public String toString() {
      return String.format("%s[%s, %s]", getClass().getSimpleName(), action, region);
    }
  }

  public static final class SelectionCreate extends SelectionCommand {
    @JsonCreator
    public SelectionCreate(@JsonProperty("region") WorldMap.Region region, @JsonProperty("replyTo") ActorRef<Command> replyTo) {
      super(Action.create, region, replyTo);
    }
  }

  static final class SelectionDelete extends SelectionCommand {
    SelectionDelete(WorldMap.Region region, ActorRef<Command> replyTo) {
      super(Action.delete, region, replyTo);
    }
  }

  static final class SelectionHappy extends SelectionCommand {
    SelectionHappy(WorldMap.Region region, ActorRef<Command> replyTo) {
      super(Action.happy, region, replyTo);
    }
  }

  static final class SelectionSad extends SelectionCommand {
    SelectionSad(WorldMap.Region region, ActorRef<Command> replyTo) {
      super(Action.sad, region, replyTo);
    }
  }

  interface Event extends CborSerializable {
  }

  public static final class SelectionAccepted implements Event {
    public final SelectionCommand.Action action;
    public final WorldMap.Region region;

    @JsonCreator
    SelectionAccepted(@JsonProperty("action") SelectionCommand.Action action, @JsonProperty("region") WorldMap.Region region) {
      this.action = action;
      this.region = region;
    }

    @Override
    public String toString() {
      return String.format("%s[%s, %s]", getClass().getSimpleName(), action, region);
    }
  }

  static final class State implements CborSerializable {
    enum Status {
      notSelected, partiallySelected, fullySelected
    }

    enum StatusOLD {
      happy, sad, neutral
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
      return selectionCommand.region.contains(region);
    }

    boolean doesRegionContainSelection(SelectionCommand selectionCommand) {
      return region.contains(selectionCommand.region);
    }

    boolean isPartialSelection(SelectionCommand selectionCommand) {
      return region.contains(selectionCommand.region);
    }

    boolean isFullSelection(SelectionCommand selectionCommand) {
      return selectionCommand.region.contains(region);
    }

    final WorldMap.Region region;
    final Selections selections;
    StatusOLD statusOLD;
    Status status;

    State(WorldMap.Region region) {
      this.region = region;
      selections = new Selections(region);
      statusOLD = region.isDevice() ? StatusOLD.happy : StatusOLD.neutral;
    }

    void selected(SelectionCommand selectionCommand) {
      if (selectionCommand.isCreate() || selectionCommand.isDelete()) {
        if (doesSelectionContainRegion(selectionCommand)) {
          status = Status.fullySelected;
        } else if (doesRegionContainSelection(selectionCommand)) {
          status = Status.partiallySelected;
        }
      }
    }

    boolean isNewSelection(SelectionCommand selectionCommand) {
      switch (selectionCommand.action) {
        case create:
          return selections.isContainedWithin(selectionCommand.region) || selections.isContainerOfVisible(selectionCommand.region);
        case delete:
        case happy:
        case sad:
          return selections.isContainedWithin(selectionCommand.region) || selections.isContainerOf(selectionCommand.region);
        default:
          return false;
      }
    }

    State addSelection(SelectionAccepted selectionAccepted) {
      switch (selectionAccepted.action) {
        case create:
          selections.create(selectionAccepted.region);
          break;
        case delete:
          selections.delete(selectionAccepted.region);
          break;
        case happy:
          statusOLD = region.isDevice() ? StatusOLD.happy : StatusOLD.neutral;
          break;
        case sad:
          statusOLD = region.isDevice() ? StatusOLD.sad : StatusOLD.neutral;
      }
      return this;
    }
  }

  static final class Selections implements CborSerializable {
    final WorldMap.Region region;
    final List<WorldMap.Region> currentSelections = new ArrayList<>();

    Selections(WorldMap.Region region) {
      this.region = region;
    }

    void create(WorldMap.Region regionCreate) {
      if (isContainedWithin(regionCreate)) {
        currentSelections.clear();
        currentSelections.add(regionCreate);
      } else if (isContainerOf(regionCreate)) {
        currentSelections.removeIf(regionCreate::contains);
        currentSelections.add(regionCreate);
      }
    }

    void delete(WorldMap.Region regionDelete) {
      if (isContainedWithin(regionDelete)) {
        currentSelections.clear();
      } else if (isContainerOf(regionDelete)) {
        currentSelections.removeIf(regionDelete::contains);
      }
    }

    boolean isContainedWithin(WorldMap.Region region) {
      return region.contains(this.region);
    }

    boolean isContainerOf(WorldMap.Region region) {
      return this.region.contains(region);
    }

    boolean isContainerOfVisible(WorldMap.Region region) {
      return isContainerOf(region) && isVisible(region);
    }

    private boolean isVisible(WorldMap.Region region) {
      return currentSelections.stream().noneMatch(currentRegion -> currentRegion.contains(region));
    }
  }

  Logger log() {
    return actorContext.getSystem().log();
  }
}

