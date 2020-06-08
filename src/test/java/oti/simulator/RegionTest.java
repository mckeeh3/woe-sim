package oti.simulator;

import akka.actor.testkit.typed.javadsl.TestKitJunitResource;
import akka.actor.testkit.typed.javadsl.TestProbe;
import akka.cluster.Cluster;
import akka.cluster.sharding.typed.javadsl.ClusterSharding;
import akka.cluster.sharding.typed.javadsl.Entity;
import akka.cluster.sharding.typed.javadsl.EntityRef;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static oti.simulator.WorldMap.*;

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
  }

  @Test
  @Ignore
  public void createZoom18Selection() {
    testKit.system().log().debug("enter createZoom18Selection");
    TestProbe<Region.Command> probe = testKit.createTestProbe();

    int zoom = 18;
    String entityId = entityIdOf(regionForZoom0());
    EntityRef<Region.Command> entityRef = clusterSharding.entityRefFor(Region.entityTypeKey, entityId);

    // London across Westminster Bridge at Park Plaza Hotel
    WorldMap.Region region = regionAtLatLng(zoom, new LatLng(51.50079211, -0.11682093));
    entityRef.tell(new Region.SelectionCreate(region, probe.ref()));

    probe.receiveSeveralMessages(1, Duration.ofSeconds(30));
    testKit.system().log().debug("exit createZoom18Selection");
  }

  @Ignore
  @Test
  public void createZoom17Selection() {
    testKit.system().log().debug("enter createZoom17Selection");
    TestProbe<Region.Command> probe = testKit.createTestProbe();

    int zoom = 17;
    String entityId = entityIdOf(regionForZoom0());
    EntityRef<Region.Command> entityRef = clusterSharding.entityRefFor(Region.entityTypeKey, entityId);

    // London across Westminster Bridge at Park Plaza Hotel
    WorldMap.Region region = regionAtLatLng(zoom, new LatLng(51.50079211, -0.11682093));
    entityRef.tell(new Region.SelectionCreate(region, probe.ref()));

    probe.receiveSeveralMessages(4, Duration.ofSeconds(30));
    testKit.system().log().debug("exit createZoom17Selection");
  }

  @Ignore
  @Test
  public void createZoom16Selection() {
    testKit.system().log().debug("enter createZoom16Selection");
    TestProbe<Region.Command> probe = testKit.createTestProbe();

    int zoom = 16;
    String entityId = entityIdOf(regionForZoom0());
    EntityRef<Region.Command> entityRef = clusterSharding.entityRefFor(Region.entityTypeKey, entityId);

    // London across Westminster Bridge at Park Plaza Hotel
    WorldMap.Region region = regionAtLatLng(zoom, new LatLng(51.50079211, -0.11682093));
    entityRef.tell(new Region.SelectionCreate(region, probe.ref()));

    probe.receiveSeveralMessages(16, Duration.ofSeconds(60));
    testKit.system().log().debug("exit createZoom16Selection");
  }

  @Test
  public void createZoom15Selection() {
    testKit.system().log().debug("enter createZoom15Selection");
    TestProbe<Region.Command> probe = testKit.createTestProbe();

    int zoom = 15;
    String entityId = entityIdOf(regionForZoom0());
    EntityRef<Region.Command> entityRef = clusterSharding.entityRefFor(Region.entityTypeKey, entityId);

    // London across Westminster Bridge at Park Plaza Hotel
    WorldMap.Region region = regionAtLatLng(zoom, new LatLng(51.50079211, -0.11682093));
    entityRef.tell(new Region.SelectionCreate(region, probe.ref()));

    probe.receiveSeveralMessages(64, Duration.ofSeconds(60));
    testKit.system().log().debug("exit createZoom15Selection");
  }

  @Ignore
  @Test
  public void createZoom10Selection() {
    testKit.system().log().debug("enter createZoom10Selection");
    TestProbe<Region.Command> probe = testKit.createTestProbe();

    int zoom = 10;
    String entityId = entityIdOf(regionForZoom0());
    EntityRef<Region.Command> entityRef = clusterSharding.entityRefFor(Region.entityTypeKey, entityId);

    // London across Westminster Bridge at Park Plaza Hotel
    WorldMap.Region region = regionAtLatLng(zoom, new LatLng(51.50079211, -0.11682093));
    entityRef.tell(new Region.SelectionCreate(region, probe.ref()));

    probe.receiveSeveralMessages(65536, Duration.ofSeconds(60));
    testKit.system().log().debug("exit createZoom10Selection");
  }

  @Ignore
  @Test
  public void createZoom09Selection() {
    testKit.system().log().debug("enter createZoom09Selection");
    TestProbe<Region.Command> probe = testKit.createTestProbe();

    int zoom = 9;
    String entityId = entityIdOf(regionForZoom0());
    EntityRef<Region.Command> entityRef = clusterSharding.entityRefFor(Region.entityTypeKey, entityId);

    // London across Westminster Bridge at Park Plaza Hotel
    WorldMap.Region region = regionAtLatLng(zoom, new LatLng(51.50079211, -0.11682093));
    entityRef.tell(new Region.SelectionCreate(region, probe.ref()));

    probe.receiveSeveralMessages(262144, Duration.ofSeconds(60));
    testKit.system().log().debug("exit createZoom09Selection");
  }

  @Ignore
  @Test
  public void createZoom08Selection() {
    testKit.system().log().debug("enter createZoom08Selection");
    TestProbe<Region.Command> probe = testKit.createTestProbe();

    int zoom = 8;
    String entityId = entityIdOf(regionForZoom0());
    EntityRef<Region.Command> entityRef = clusterSharding.entityRefFor(Region.entityTypeKey, entityId);

    // London across Westminster Bridge at Park Plaza Hotel
    WorldMap.Region region = regionAtLatLng(zoom, new LatLng(51.50079211, -0.11682093));
    entityRef.tell(new Region.SelectionCreate(region, probe.ref()));

    probe.receiveSeveralMessages(1048576, Duration.ofMinutes(10));
    testKit.system().log().debug("exit createZoom08Selection");
  }

  @Test
  public void selectionCreateWithLargerContainingSelectionsOverrideSmallerSelections() {
    List<List<WorldMap.Region>> zoomRegions = zoomRegions();

    WorldMap.Region selectionZoom4 = zoomRegions.get(3).get(0);
    WorldMap.Region selectionZoom5 = zoomRegions.get(4).get(0);
    WorldMap.Region selectionZoom6 = zoomRegions.get(5).get(0);

    Region.Selections selections = new Region.Selections(selectionZoom5);

    // add 4 non-overlapping sub-selections
    subRegionsFor(selectionZoom6).forEach(selections::create);

    assertEquals(4, selections.currentSelections.size());

    // add selection that contains the current 4 sub-selections
    // causing them to be removed and replaced by the new selection
    selections.create(selectionZoom6);

    assertEquals(1, selections.currentSelections.size());

    selections.create(selectionZoom4);

    assertEquals(1, selections.currentSelections.size());
  }

  @Test
  public void selectionDeleteWithSmallerDeleteRegionThanCurrentSelections() {
    List<List<WorldMap.Region>> zoomRegions = zoomRegions();

    WorldMap.Region selectionZoom15 = zoomRegions.get(14).get(0);
    Region.Selections selections = new Region.Selections(selectionZoom15);

    zoomRegions.get(15).forEach(selections::create);
    assertEquals(4, selections.currentSelections.size());

    WorldMap.Region selectionZoom17 = zoomRegions.get(16).get(0);
    selections.delete(selectionZoom17);
    assertEquals(4, selections.currentSelections.size());
  }

  @Test
  public void selectionDeleteWithLargerDeleteRegionThanCurrentSelections() {
    List<List<WorldMap.Region>> zoomRegions = zoomRegions();

    WorldMap.Region selectionZoom15 = zoomRegions.get(14).get(0);
    Region.Selections selections = new Region.Selections(selectionZoom15);

    zoomRegions.get(15).forEach(selections::create);
    assertEquals(4, selections.currentSelections.size());

    WorldMap.Region selectionZoom14 = zoomRegions.get(13).get(0);
    selections.delete(selectionZoom14);
    assertEquals(0, selections.currentSelections.size());
  }

  private void clusterShardingInit(ClusterSharding clusterSharding) {
    clusterSharding.init(
        Entity.of(
            Region.entityTypeKey,
            entityContext -> Region.create(entityContext.getEntityId(), clusterSharding)
        )
    );
  }

  private static WorldMap.Region regionAtLatLng(int zoom, WorldMap.LatLng latLng) {
    return regionAtLatLng(zoom, latLng, WorldMap.regionForZoom0());
  }

  private static WorldMap.Region regionAtLatLng(int zoom, WorldMap.LatLng latLng, WorldMap.Region region) {
    if (zoom == region.zoom) {
      return region;
    }
    List<WorldMap.Region> subRegions = subRegionsFor(region);
    Optional<WorldMap.Region> subRegionOpt = subRegions.stream().filter(r -> r.isInside(latLng)).findFirst();
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
}
