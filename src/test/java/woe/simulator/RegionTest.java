package woe.simulator;

import akka.Done;
import akka.actor.ActorSystem;
import akka.actor.testkit.typed.javadsl.TestKitJunitResource;
import akka.actor.testkit.typed.javadsl.TestProbe;
import akka.cluster.Cluster;
import akka.cluster.sharding.typed.javadsl.ClusterSharding;
import akka.cluster.sharding.typed.javadsl.Entity;
import akka.cluster.sharding.typed.javadsl.EntityRef;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.model.StatusCodes;
import akka.http.javadsl.model.headers.RawHeader;
import akka.http.javadsl.server.Route;
import akka.pattern.StatusReply;
import akka.stream.Materializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.stream.IntStream;

import static akka.http.javadsl.server.Directives.*;
import static woe.simulator.WorldMap.*;
import static org.junit.jupiter.api.Assertions.*;

public class RegionTest {
  private static ClusterSharding clusterSharding;


  @ClassRule
  public static final TestKitJunitResource testKit = new TestKitJunitResource(config());

  private static Config config() {
    return ConfigFactory.parseString(
        String.format("akka.cluster.seed-nodes = [ \"akka://%s@127.0.0.1:25520\" ] %n", RegionTest.class.getSimpleName())
            + String.format("akka.persistence.snapshot-store.local.dir = \"%s-%s\" %n", "target/snapshot", UUID.randomUUID().toString())
    ).withFallback(ConfigFactory.load("application-test.conf"));
  }

  @BeforeClass
  public static void setupClass() {
    clusterSharding = ClusterSharding.get(testKit.system());

    clusterSharding.init(
        Entity.of(
            Region.entityTypeKey,
            entityContext ->
                Region.create(entityContext.getEntityId(), clusterSharding)
        )
    );
    testKit.system().log().info("Test cluster node {}", Cluster.get(testKit.system()).selfMember());

    final String host = testKit.system().settings().config().getString("woe.twin.http.server.host");
    final int port = testKit.system().settings().config().getInt("woe.twin.http.server.port");
    httpServer(host, port);
  }

  @Test
  @Ignore
  public void createZoom18Selection() {
    testKit.system().log().debug("enter createZoom18Selection");
    TestProbe<StatusReply<Done>> probe = testKit.createTestProbe();

    int zoom = 18;
    String entityId = entityIdOf(regionForZoom0());
    EntityRef<Region.Command> entityRef = clusterSharding.entityRefFor(Region.entityTypeKey, entityId);

    // London across Westminster Bridge at Park Plaza Hotel
    WorldMap.Region region = regionAtLatLng(zoom, new LatLng(51.50079211, -0.11682093));
    entityRef.tell(new Region.SelectionCreate(region, probe.ref()));

    probe.receiveMessage();
    testKit.system().log().debug("exit createZoom18Selection");
  }

  @Ignore
  @Test
  public void createZoom17Selection() {
    testKit.system().log().debug("enter createZoom17Selection");
    TestProbe<StatusReply<Done>> probe = testKit.createTestProbe();

    int zoom = 17;
    String entityId = entityIdOf(regionForZoom0());
    EntityRef<Region.Command> entityRef = clusterSharding.entityRefFor(Region.entityTypeKey, entityId);

    // London across Westminster Bridge at Park Plaza Hotel
    WorldMap.Region region = regionAtLatLng(zoom, new LatLng(51.50079211, -0.11682093));
    entityRef.tell(new Region.SelectionCreate(region, probe.ref()));

    probe.receiveMessage(Duration.ofSeconds(30));
    testKit.system().log().debug("exit createZoom17Selection");
  }

  @Ignore
  @Test
  public void createZoom16Selection() {
    testKit.system().log().debug("enter createZoom16Selection");
    TestProbe<StatusReply<Done>> probe = testKit.createTestProbe();

    int zoom = 16;
    String entityId = entityIdOf(regionForZoom0());
    EntityRef<Region.Command> entityRef = clusterSharding.entityRefFor(Region.entityTypeKey, entityId);

    // London across Westminster Bridge at Park Plaza Hotel
    WorldMap.Region region = regionAtLatLng(zoom, new LatLng(51.50079211, -0.11682093));
    entityRef.tell(new Region.SelectionCreate(region, probe.ref()));

    probe.receiveMessage(Duration.ofSeconds(60));
    testKit.system().log().debug("exit createZoom16Selection");
  }

  @Test
  public void createZoom15Selection() {
    testKit.system().log().debug("enter createZoom15Selection");
    TestProbe<StatusReply<Done>> probe = testKit.createTestProbe();

    int zoom = 15;
    String entityId = entityIdOf(regionForZoom0());
    EntityRef<Region.Command> entityRef = clusterSharding.entityRefFor(Region.entityTypeKey, entityId);

    // London across Westminster Bridge at Park Plaza Hotel
    WorldMap.Region region = regionAtLatLng(zoom, new LatLng(51.50079211, -0.11682093));
    entityRef.tell(new Region.SelectionCreate(region, probe.ref()));

    probe.receiveMessage(Duration.ofSeconds(30));
    testKit.system().log().debug("exit createZoom15Selection");
  }

  @Ignore
  @Test
  public void createZoom13Selection() {
    testKit.system().log().debug("enter createZoom15Selection");
    TestProbe<StatusReply<Done>> probe = testKit.createTestProbe();

    int zoom = 13;
    String entityId = entityIdOf(regionForZoom0());
    EntityRef<Region.Command> entityRef = clusterSharding.entityRefFor(Region.entityTypeKey, entityId);

    // London across Westminster Bridge at Park Plaza Hotel
    WorldMap.Region region = regionAtLatLng(zoom, new LatLng(51.50079211, -0.11682093));
    entityRef.tell(new Region.SelectionCreate(region, probe.ref()));

    probe.receiveMessage(Duration.ofSeconds(30));
    testKit.system().log().debug("exit createZoom15Selection");
  }

  @Ignore
  @Test
  public void createZoom10Selection() {
    testKit.system().log().debug("enter createZoom10Selection");
    TestProbe<StatusReply<Done>> probe = testKit.createTestProbe();

    int zoom = 10;
    String entityId = entityIdOf(regionForZoom0());
    EntityRef<Region.Command> entityRef = clusterSharding.entityRefFor(Region.entityTypeKey, entityId);

    // London across Westminster Bridge at Park Plaza Hotel
    WorldMap.Region region = regionAtLatLng(zoom, new LatLng(51.50079211, -0.11682093));
    entityRef.tell(new Region.SelectionCreate(region, probe.ref()));

    probe.receiveMessage(Duration.ofSeconds(60));
    testKit.system().log().debug("exit createZoom10Selection");
  }

  @Ignore
  @Test
  public void createZoom09Selection() {
    testKit.system().log().debug("enter createZoom09Selection");
    TestProbe<StatusReply<Done>> probe = testKit.createTestProbe();

    int zoom = 9;
    String entityId = entityIdOf(regionForZoom0());
    EntityRef<Region.Command> entityRef = clusterSharding.entityRefFor(Region.entityTypeKey, entityId);

    // London across Westminster Bridge at Park Plaza Hotel
    WorldMap.Region region = regionAtLatLng(zoom, new LatLng(51.50079211, -0.11682093));
    entityRef.tell(new Region.SelectionCreate(region, probe.ref()));

    probe.receiveMessage(Duration.ofSeconds(60));
    testKit.system().log().debug("exit createZoom09Selection");
  }

  @Ignore
  @Test
  public void createZoom08Selection() {
    testKit.system().log().debug("enter createZoom08Selection");
    TestProbe<StatusReply<Done>> probe = testKit.createTestProbe();

    int zoom = 8;
    String entityId = entityIdOf(regionForZoom0());
    EntityRef<Region.Command> entityRef = clusterSharding.entityRefFor(Region.entityTypeKey, entityId);

    // London across Westminster Bridge at Park Plaza Hotel
    WorldMap.Region region = regionAtLatLng(zoom, new LatLng(51.50079211, -0.11682093));
    entityRef.tell(new Region.SelectionCreate(region, probe.ref()));

    probe.receiveMessage(Duration.ofMinutes(10));
    testKit.system().log().debug("exit createZoom08Selection");
  }

  @Test
  public void serializeDeserializeSelectionCommands() throws IOException {
    CBORFactory cborFactory = new CBORFactory();
    ObjectMapper objectMapper = new ObjectMapper(cborFactory);

    final Region.SelectionCreate selectionCreate = new Region.SelectionCreate(regionForZoom0(), null);
    final byte[] bytesSelectionCreate = objectMapper.writeValueAsBytes(selectionCreate);
    final Region.SelectionCreate selectionCreate1 = objectMapper.readValue(bytesSelectionCreate, Region.SelectionCreate.class);
    assertEquals(selectionCreate, selectionCreate1);

    final Region.SelectionDelete selectionDelete = new Region.SelectionDelete(regionForZoom0(), null);
    final byte[] bytesSelectionDelete = objectMapper.writeValueAsBytes(selectionDelete);
    final Region.SelectionDelete selectionDelete1 = objectMapper.readValue(bytesSelectionDelete, Region.SelectionDelete.class);
    assertEquals(selectionDelete, selectionDelete1);

    final Region.SelectionHappy selectionHappy = new Region.SelectionHappy(regionForZoom0(), null);
    final byte[] bytesSelectionHappy = objectMapper.writeValueAsBytes(selectionHappy);
    final Region.SelectionHappy selectionHappy1 = objectMapper.readValue(bytesSelectionHappy, Region.SelectionHappy.class);
    assertEquals(selectionHappy, selectionHappy1);

    final Region.SelectionSad selectionSad = new Region.SelectionSad(regionForZoom0(), null);
    final byte[] bytesSelectionSad = objectMapper.writeValueAsBytes(selectionSad);
    final Region.SelectionSad selectionSad1 = objectMapper.readValue(bytesSelectionSad, Region.SelectionSad.class);
    assertEquals(selectionSad, selectionSad1);
  }

  private static WorldMap.Region regionAtLatLng(int zoom, WorldMap.LatLng latLng) {
    return regionAtLatLng(zoom, latLng, WorldMap.regionForZoom0());
  }

  private static WorldMap.Region regionAtLatLng(int zoom, WorldMap.LatLng latLng, WorldMap.Region region) {
    if (zoom == region.zoom) {
      return region;
    }
    List<WorldMap.Region> subRegions = subRegionsFor(region);
    Optional<WorldMap.Region> subRegionOpt = subRegions.stream().filter(r -> r.contains(latLng)).findFirst();
    return subRegionOpt.map(subRegion -> regionAtLatLng(zoom, latLng, subRegion)).orElse(null);
  }

  // Create a stack of regions from region 0, the entire earth, to region 18, the smallest region.
  // Each list item is a list of sub-regions of the region above by zoom levels.
  // Each list item's sub-region list is created by selecting the sub-regions from item 0 from he prior list.
  private List<List<WorldMap.Region>> zoomRegions() {
    List<List<WorldMap.Region>> zoomRegions = new ArrayList<>();

    List<WorldMap.Region> regions = subRegionsFor(regionForZoom0());
    zoomRegions.add(regions);

    IntStream.range(1, zoomMax).forEach(zoom -> {
      WorldMap.Region lastRegion = zoomRegions.get(zoomRegions.size() - 1).get(0);
      zoomRegions.add(subRegionsFor(lastRegion));
    });
    return zoomRegions;
  }

  private static CompletionStage<ServerBinding> httpServer(String host, int port) {
    ActorSystem actorSystem = testKit.system().classicSystem();

    return Http.get(actorSystem.classicSystem())
        .bindAndHandle(route().flow(actorSystem.classicSystem(), materializer()),
            ConnectHttp.toHost(host, port), materializer());
  }

  private static Route route() {
    return concat(
        path("telemetry", () -> concat(
            get(() -> {
              WorldMap.Region selection = regionForZoom0();
              Region.SelectionCreate selectionCreate = new Region.SelectionCreate(selection, null);
              return complete(StatusCodes.OK, selectionCreate, Jackson.marshaller());
            }),
            post(() -> entity(
                Jackson.unmarshaller(HttpClient.TelemetryRequest.class),
                telemetryRequest -> {
                  testKit.system().log().info("*****{}", telemetryRequest);
                  final HttpClient.TelemetryResponse telemetryResponse = new HttpClient.TelemetryResponse("ok", StatusCodes.CREATED.intValue(), telemetryRequest);
                  return respondWithHeader(RawHeader.create("Connection", "close"), () ->
                      complete(StatusCodes.CREATED, telemetryResponse, Jackson.marshaller()));
                })
            )
        ))
    );
  }

  private static Materializer materializer() {
    return Materializer.matFromSystem(testKit.system().classicSystem());
  }
}
