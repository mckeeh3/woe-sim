package oti.simulator;

import akka.actor.testkit.typed.javadsl.TestKitJunitResource;
import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.jupiter.api.Assertions.*;
import static oti.simulator.WorldMap.*;

public class RegionTest {

  @ClassRule
  public static final TestKitJunitResource testKit = new TestKitJunitResource();

  @Test
  public void entityIdOfZoomRegion0isOk() {
    WorldMap.Region region = regionForZoom0();
    String entityId = entityIdOf(region);

    assertEquals("region:0:90.000000:-180.000000:-90.000000:180.000000", entityId);
  }
}
