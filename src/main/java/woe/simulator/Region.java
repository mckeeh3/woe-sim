package woe.simulator;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.TimerScheduler;
import akka.cluster.sharding.typed.javadsl.ClusterSharding;
import akka.cluster.sharding.typed.javadsl.EntityRef;
import akka.cluster.sharding.typed.javadsl.EntityTypeKey;
import akka.persistence.typed.PersistenceId;
import akka.persistence.typed.SnapshotSelectionCriteria;
import akka.persistence.typed.javadsl.*;
import com.fasterxml.jackson.annotation.JsonCreator;
import org.slf4j.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

class Region extends EventSourcedBehavior<Region.Command, Region.Event, Region.State> {
  final String entityId;
  final WorldMap.Region region;
  final ClusterSharding clusterSharding;
  final TimerScheduler<Command> timerScheduler;
  final ActorContext<Command> actorContext;
  final Clients clients;
  static final EntityTypeKey<Command> entityTypeKey = EntityTypeKey.create(Command.class, Region.class.getSimpleName());

  static Behavior<Command> create(String entityId, ClusterSharding clusterSharding, Clients clients) {
    return Behaviors.setup(actorContext ->
        Behaviors.withTimers(timer -> new Region(entityId, clusterSharding, clients, actorContext, timer)));
  }

  private Region(String entityId, ClusterSharding clusterSharding, Clients clients, ActorContext<Command> actorContext, TimerScheduler<Command> timerScheduler) {
    super(PersistenceId.of(entityTypeKey.name(), entityId));
    this.entityId = entityId;
    this.region = WorldMap.regionForEntityId(entityId);
    this.clusterSharding = clusterSharding;
    this.actorContext = actorContext;
    this.clients = clients;
    this.timerScheduler = timerScheduler;
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
        .onCommand(PingPartiallySelected.class, this::onPingPartiallySelected)
        .onCommand(PingFullySelected.class, this::onPingFullySelected)
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
    if (rateDelayed(state, selectionCreate)) {
      return Effect().none();
    }
    if (state.doesCommandRegionOverlapStateRegion(selectionCreate)) {
      if (state.isPartialSelection(selectionCreate) && state.isNotSelected()) {
        return acceptSelection(selectionCreate);
      } else if (state.isFullSelection(selectionCreate) && (state.isNotSelected() || state.isPartiallySelected())) {
        return acceptSelection(selectionCreate);
      } else {
        if (state.region.isDevice()) {
          notifyTwin(state, selectionCreate);
        } else {
          forwardSelectionToSubRegions(state, selectionCreate);
        }
      }
    }
    return Effect().none();
  }

  private Effect<Event, State> onSelectionDelete(State state, SelectionDelete selectionDelete) {
    if (rateDelayed(state, selectionDelete)) {
      Effect().none();
    }
    if (state.doesCommandRegionOverlapStateRegion(selectionDelete)) {
      if (state.isFullySelected()) {
        return acceptSelection(selectionDelete);
      } else if (state.isFullSelection(selectionDelete) & state.isPartiallySelected()) {
        return acceptSelection(selectionDelete);
      } else {
        if (state.region.isDevice()) {
          notifyTwin(state, selectionDelete);
        } else {
          forwardSelectionToSubRegions(state, selectionDelete);
        }
      }
    }
    return Effect().none();
  }

  private Effect<Event, State> onSelectionHappyOrSad(State state, SelectionCommand selectionHappyOrSad) {
    if (rateDelayed(state, selectionHappyOrSad)) {
      return Effect().none();
    }
    if (state.doesCommandRegionOverlapStateRegion(selectionHappyOrSad)) {
      if (state.isPartiallySelected() || state.isFullySelected()) {
        if (state.region.isDevice()) {
          notifyTwin(state, selectionHappyOrSad);
        } else {
          forwardSelectionToSubRegions(state, selectionHappyOrSad);
        }
      }
    }
    return Effect().none();
  }

  private Effect<Event, State> onPingPartiallySelected(State state, PingPartiallySelected pingPartiallySelected) {
    if (rateDelayed(state, pingPartiallySelected)) {
      return Effect().none();
    }
    if (state.doesCommandRegionOverlapStateRegion(pingPartiallySelected)) {
      if (state.isFullySelected()) {
        if (state.region.isDevice()) {
          notifyTwin(state, pingPartiallySelected);
        } else {
          forwardSelectionToSubRegions(state, pingPartiallySelected.asPingFullySelected(state.region));
        }
      } else if (state.isPartiallySelected()) {
        forwardSelectionToSubRegions(state, pingPartiallySelected);
      }
    }
    return Effect().none();
  }

  private Effect<Event, State> onPingFullySelected(State state, PingFullySelected pingFullySelected) {
    if (rateDelayed(state, pingFullySelected)) {
      return Effect().none();
    }
    if (state.doesCommandRegionOverlapStateRegion(pingFullySelected)) {
      if (state.isFullySelected()) {
        if (state.region.isDevice()) {
          notifyTwin(state, pingFullySelected);
        } else {
          forwardSelectionToSubRegions(state, pingFullySelected);
        }
      } else { // this region should be fully selected, so fix it. this is a form of self healing.
        return acceptSelection((pingFullySelected.asSelectionCreate(state.region)));
      }
    }
    return Effect().none();
  }

  private boolean rateDelayed(State state, SelectionCommand selectionCommand) {
    final int delayMsMin = 5;
    if (selectionCommand instanceof PingFullySelected || selectionCommand instanceof PingPartiallySelected) {
      return false;
    }
    if (selectionCommand.delayed) {
      log().info("rate: delayed command received {}", selectionCommand);
      state.delayed = false;
      return false;
    }
    if (state.delayed) {
      log().info("rate: ignore while delayed {}", selectionCommand);
      return true;
    }
    final Duration untilDeadline = Duration.between(Instant.now(), selectionCommand.deadline);
    if (untilDeadline.toMillis() < delayMsMin) {
      return false;
    }
    final double untilDeadlinePercent = WorldMap.percentForSelectionAtZoom(selectionCommand.region.zoom, state.region.zoom);
    final double randomPercent = untilDeadlinePercent * Math.random();
    final Duration delay = Duration.ofMillis((long) (untilDeadline.toMillis() * randomPercent));
    if (delay.toMillis() < delayMsMin) {
      return false;
    }
    log().info("rate: delay command {} {}", delay, selectionCommand);
    timerScheduler.startSingleTimer(selectionCommand.asDelayed(true), delay);
    state.delayed = true;
    return true;
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
      notifyTwin(state, selectionCommand);
    } else {
      forwardSelectionToSubRegions(state, selectionCommand);
    }
  }

  private void notifyTwin(State state, SelectionCommand selectionCommand) {
    final SelectionCommand selectionCommandNotify = selectionCommand.with(state.region);
    if (!(selectionCommand instanceof PingFullySelected) && !(selectionCommand instanceof PingPartiallySelected)) {
      log().info("Notify twin {}", selectionCommandNotify);
    }
    clients.post(selectionCommandNotify);
  }

  private void forwardSelectionToSubRegions(State state, SelectionCommand selectionCommand) {
    List<WorldMap.Region> subRegions = WorldMap.subRegionsFor(state.region);
    subRegions.forEach(region -> {
      EntityRef<Command> entityRef = clusterSharding.entityRefFor(entityTypeKey, WorldMap.entityIdOf(region));
      entityRef.tell(selectionCommand.asDelayed(false));
    });
  }

  interface Command extends CborSerializable {
  }

  public abstract static class SelectionCommand implements Command {
    enum Action {
      create, delete, happy, sad, ping
    }

    public final Action action;
    public final WorldMap.Region region;
    public final Instant deadline;
    public final boolean delayed;
    public final ActorRef<Command> replyTo;

    public SelectionCommand(Action action, WorldMap.Region region, Instant deadline, boolean delayed, ActorRef<Command> replyTo) {
      this.action = action;
      this.region = region;
      this.deadline = deadline;
      this.delayed = delayed;
      this.replyTo = replyTo; // used for unit testing
    }

    abstract SelectionCommand with(WorldMap.Region region);

    abstract SelectionCommand asDelayed(boolean delayed);

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      SelectionCommand that = (SelectionCommand) o;
      return delayed == that.delayed &&
          action == that.action &&
          region.equals(that.region) &&
          deadline.equals(that.deadline) &&
          Objects.equals(replyTo, that.replyTo);
    }

    @Override
    public int hashCode() {
      return Objects.hash(action, region, deadline, delayed, replyTo);
    }

    @Override
    public String toString() {
      return String.format("%s[%s, %s, %s, %b]", getClass().getSimpleName(), action, region, deadline, delayed);
    }
  }

  public static final class SelectionCreate extends SelectionCommand {
    @JsonCreator
    public SelectionCreate(WorldMap.Region region, Instant deadline, boolean delayed, ActorRef<Command> replyTo) {
      super(Action.create, region, deadline, delayed, replyTo);
    }

    @Override
    SelectionCreate with(WorldMap.Region region) {
      return new SelectionCreate(region, deadline, delayed, replyTo);
    }

    @Override
    SelectionCommand asDelayed(boolean delayed) {
      return new SelectionCreate(region, deadline, delayed, replyTo);
    }
  }

  public static final class SelectionDelete extends SelectionCommand {
    @JsonCreator
    public SelectionDelete(WorldMap.Region region, Instant deadline, boolean delayed, ActorRef<Command> replyTo) {
      super(Action.delete, region, deadline, delayed, replyTo);
    }

    @Override
    SelectionDelete with(WorldMap.Region region) {
      return new SelectionDelete(region, deadline, delayed, replyTo);
    }

    @Override
    SelectionCommand asDelayed(boolean delayed) {
      return new SelectionDelete(region, deadline, delayed, replyTo);
    }
  }

  public static final class SelectionHappy extends SelectionCommand {
    @JsonCreator
    public SelectionHappy(WorldMap.Region region, Instant deadline, boolean delayed, ActorRef<Command> replyTo) {
      super(Action.happy, region, deadline, delayed, replyTo);
    }

    @Override
    SelectionHappy with(WorldMap.Region region) {
      return new SelectionHappy(region, deadline, delayed, replyTo);
    }

    @Override
    SelectionCommand asDelayed(boolean delayed) {
      return new SelectionHappy(region, deadline, delayed, replyTo);
    }
  }

  public static final class SelectionSad extends SelectionCommand {
    @JsonCreator
    public SelectionSad(WorldMap.Region region, Instant deadline, boolean delayed, ActorRef<Command> replyTo) {
      super(Action.sad, region, deadline, delayed, replyTo);
    }

    @Override
    SelectionSad with(WorldMap.Region region) {
      return new SelectionSad(region, deadline, delayed, replyTo);
    }

    @Override
    SelectionCommand asDelayed(boolean delayed) {
      return new SelectionSad(region, deadline, delayed, replyTo);
    }
  }

  public static final class PingPartiallySelected extends SelectionCommand {
    @JsonCreator
    public PingPartiallySelected(WorldMap.Region region, Instant deadline, boolean delayed, ActorRef<Command> replyTo) {
      super(Action.ping, region, deadline, delayed, replyTo);
    }

    @Override
    PingPartiallySelected with(WorldMap.Region region) {
      return new PingPartiallySelected(region, deadline, delayed, replyTo);
    }

    @Override
    SelectionCommand asDelayed(boolean delayed) {
      return new PingPartiallySelected(region, deadline, delayed, replyTo);
    }

    PingFullySelected asPingFullySelected(WorldMap.Region region) {
      return new PingFullySelected(region, deadline, delayed, replyTo);
    }
  }

  public static final class PingFullySelected extends SelectionCommand {
    @JsonCreator
    public PingFullySelected(WorldMap.Region region, Instant deadline, boolean delayed, ActorRef<Command> replyTo) {
      super(Action.ping, region, deadline, delayed, replyTo);
    }

    @Override
    PingFullySelected with(WorldMap.Region region) {
      return new PingFullySelected(region, deadline, delayed, replyTo);
    }

    @Override
    SelectionCommand asDelayed(boolean delayed) {
      return new PingFullySelected(region, deadline, delayed, replyTo);
    }

    SelectionCreate asSelectionCreate(WorldMap.Region region) {
      return new SelectionCreate(region, deadline, delayed, replyTo);
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
    boolean delayed;

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

    boolean doesCommandRegionOverlapStateRegion(SelectionCommand selectionCommand) {
      return region.overlaps(selectionCommand.region);
    }

    boolean doesSelectionContainRegion(SelectionAccepted selectionAccepted) {
      return doesSelectionContainRegion(selectionAccepted.region);
    }

    private boolean doesSelectionContainRegion(WorldMap.Region region) {
      return region.contains(this.region);
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
