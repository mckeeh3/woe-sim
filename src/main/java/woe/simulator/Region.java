package woe.simulator;

import akka.Done;
import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.cluster.sharding.typed.javadsl.ClusterSharding;
import akka.cluster.sharding.typed.javadsl.EntityRef;
import akka.cluster.sharding.typed.javadsl.EntityTypeKey;
import akka.pattern.StatusReply;
import akka.persistence.typed.PersistenceId;
import akka.persistence.typed.SnapshotSelectionCriteria;
import akka.persistence.typed.javadsl.*;
import akka.stream.Materializer;
import akka.stream.javadsl.Source;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletionStage;

class Region extends EventSourcedBehavior<Region.Command, Region.Event, Region.State> {
  final String entityId;
  final WorldMap.Region region;
  final ClusterSharding clusterSharding;
  final ActorContext<Command> actorContext;
  final HttpClient httpClient;
  // FIXME this duration could be scaled with the zoom level of the region since the more zoomed out the more sub regions
  // will be involved in handling a request
  private final Duration subRegionRequestTimeout;
  private final int subRegionRequestParallelism;
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
    this.subRegionRequestTimeout = actorContext.getSystem().settings().config().getDuration("woe.simulator.subregion-request-timeout");
    this.subRegionRequestParallelism = actorContext.getSystem().settings().config().getInt("woe.simulator.subregion-request-parallelism");
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
        .onCommand(PingPartiallySelected.class, this::pingPartiallySelected)
        .onCommand(PingFullySelected.class, this::pingFullySelected)
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
    if (state.doesCommandRegionOverlapStateRegion(selectionCreate)) {
      if (state.isPartialSelection(selectionCreate) && state.isNotSelected()) {
        return acceptSelection(selectionCreate);
      } else if (state.isFullSelection(selectionCreate) && (state.isNotSelected() || state.isPartiallySelected())) {
        return acceptSelection(selectionCreate);
      } else {
        if (state.region.isDevice()) {
          notifyTwin(state, selectionCreate);
          // Note that we don't wait for notify twin to complete before we ack here so that will not be part
          // of backpressure
          selectionCreate.replyTo.tell(StatusReply.Ack());
        } else {
          forwardSelectionToSubRegions(state, selectionCreate);
        }
        return Effect().none();
      }
    } else {
      return Effect().none();
    }
  }

  private Effect<Event, State> onSelectionDelete(State state, SelectionDelete selectionDelete) {
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
        return Effect().none();
      }
    } else {
      return Effect().none();
    }
  }

  private Effect<Event, State> onSelectionHappyOrSad(State state, SelectionCommand selectionHappyOrSad) {
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

  private Effect<Event, State> pingPartiallySelected(State state, PingPartiallySelected pingPartiallySelected) {
    if (state.doesCommandRegionOverlapStateRegion(pingPartiallySelected)) {
      if (state.isFullySelected()) {
        if (state.region.isDevice()) {
          notifyTwin(state, pingPartiallySelected);
        } else {
          forwardSelectionToSubRegions(state, new PingFullySelected(state.region, pingPartiallySelected.replyTo));
        }
      } else if (state.isPartiallySelected()) {
        forwardSelectionToSubRegions(state, pingPartiallySelected);
      }
    }
    return Effect().none();
  }

  private Effect<Event, State> pingFullySelected(State state, PingFullySelected pingFullySelected) {
    if (state.doesCommandRegionOverlapStateRegion(pingFullySelected)) {
      if (state.isFullySelected()) {
        if (state.region.isDevice()) {
          notifyTwin(state, pingFullySelected);
        } else {
          forwardSelectionToSubRegions(state, pingFullySelected);
        }
      } else { // this region should be fully selected, so fix it. this is a form of self healing.
        return acceptSelection(new SelectionCreate(state.region, pingFullySelected.replyTo));
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
    if (state.region.isDevice()) {
      notifyTwin(state, selectionCommand);
    } else {
      forwardSelectionToSubRegions(state, selectionCommand);
    }
  }

  private void notifyTwin(State state, SelectionCommand selectionCommand) {
    httpClient.post(selectionCommand.with(state.region))
        .thenAccept(t -> {
          if (t.httpStatusCode != 200) {
            log().warn("Telemetry request failed {}", t);
          }
          // not sure if you want to fail the entire request if there is an error code here?
          selectionCommand.replyTo.tell(StatusReply.ack());
        });
  }

  private void forwardSelectionToSubRegions(State state, SelectionCommand selectionCommand) {
    final List<WorldMap.Region> subRegions = WorldMap.subRegionsFor(state.region);
    CompletionStage<Done> allDone = Source.from(subRegions)
      .mapAsync(
              subRegionRequestParallelism,
        subRegion -> {
          EntityRef<Command> entityRef = clusterSharding.entityRefFor(entityTypeKey, WorldMap.entityIdOf(region));
          return entityRef.askWithStatus(selectionCommand::withReplyTo, subRegionRequestTimeout);
        })
      // If we'd want to do an aggregate of how many actors were touched instead of ack
      // we could use StatusReply<Integer> instead of Ack and use .runFold here to compute
      .run(Materializer.matFromSystem(actorContext.getSystem()));

    final Logger log = actorContext.getSystem().log();
    allDone.whenComplete((ok, failure) -> {
      if (ok != null) {
        log.debug("Got ack from all {} subregions, sending ack back to requestee", subRegions.size());
        selectionCommand.replyTo.tell(StatusReply.Ack());
      } else {
        // note we are in a future callback here so it is not safe to use actor logger here
        log.error("Failed ack to get ack from all subregions", failure);
        selectionCommand.replyTo.tell(StatusReply.error("Forwarding to subregion failed in " + persistenceId() +
                " after " + subRegionRequestTimeout + ", " + failure.getMessage()));
      }
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
    public final ActorRef<StatusReply<Done>> replyTo;

    public SelectionCommand(Action action, WorldMap.Region region, ActorRef<StatusReply<Done>> replyTo) {
      this.action = action;
      this.region = region;
      this.replyTo = replyTo;
    }

    abstract SelectionCommand with(WorldMap.Region region);

    public abstract SelectionCommand withReplyTo(ActorRef<StatusReply<Done>> replyTo);

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
    // I think there's a compiler flag so you don't really need the @JsonProperty annotations but the field names can be used
    public SelectionCreate(@JsonProperty("region") WorldMap.Region region, @JsonProperty("replyTo") ActorRef<StatusReply<Done>> replyTo) {
      super(Action.create, region, replyTo);
    }

    @Override
    public SelectionCommand withReplyTo(ActorRef<StatusReply<Done>> replyTo) {
      return new SelectionCreate(region, replyTo);
    }

    SelectionCreate with(WorldMap.Region region) {
      return new SelectionCreate(region, this.replyTo);
    }
  }

  public static final class SelectionDelete extends SelectionCommand {
    public SelectionDelete(@JsonProperty("region") WorldMap.Region region, @JsonProperty("replyTo") ActorRef<StatusReply<Done>> replyTo) {
      super(Action.delete, region, replyTo);
    }

    @Override
    public SelectionCommand withReplyTo(ActorRef<StatusReply<Done>> replyTo) {
      return new SelectionDelete(region, replyTo);
    }

    SelectionDelete with(WorldMap.Region region) {
      return new SelectionDelete(region, this.replyTo);
    }
  }

  public static final class SelectionHappy extends SelectionCommand {

    @Override
    public SelectionCommand withReplyTo(ActorRef<StatusReply<Done>> replyTo) {
      return new SelectionHappy(region, replyTo);
    }

    public SelectionHappy(@JsonProperty("region") WorldMap.Region region, @JsonProperty("replyTo") ActorRef<StatusReply<Done>> replyTo) {
      super(Action.happy, region, replyTo);
    }

    SelectionHappy with(WorldMap.Region region) {
      return new SelectionHappy(region, this.replyTo);
    }
  }

  public static final class SelectionSad extends SelectionCommand {
    public SelectionSad(@JsonProperty("region") WorldMap.Region region, @JsonProperty("replyTo") ActorRef<StatusReply<Done>> replyTo) {
      super(Action.sad, region, replyTo);
    }

    @Override
    public SelectionCommand withReplyTo(ActorRef<StatusReply<Done>> replyTo) {
      return new SelectionSad(region, replyTo);
    }


    SelectionSad with(WorldMap.Region region) {
      return new SelectionSad(region, this.replyTo);
    }
  }

  public static final class PingPartiallySelected extends SelectionCommand {
    public PingPartiallySelected(@JsonProperty("region") WorldMap.Region region, @JsonProperty("replyTo") ActorRef<StatusReply<Done>> replyTo) {
      super(Action.ping, region, replyTo);
    }
    @Override
    public SelectionCommand withReplyTo(ActorRef<StatusReply<Done>> replyTo) {
      return new PingPartiallySelected(region, replyTo);
    }


    @Override
    PingPartiallySelected with(WorldMap.Region region) {
      return new PingPartiallySelected(region, replyTo);
    }
  }

  public static final class PingFullySelected extends SelectionCommand {
    public PingFullySelected(@JsonProperty("region") WorldMap.Region region, @JsonProperty("replyTo") ActorRef<StatusReply<Done>> replyTo) {
      super(Action.ping, region, replyTo);
    }

    @Override
    public SelectionCommand withReplyTo(ActorRef<StatusReply<Done>> replyTo) {
      return new PingFullySelected(region, replyTo);
    }


    @Override
    PingFullySelected with(WorldMap.Region region) {
      return new PingFullySelected(region, replyTo);
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

