package woe.simulator;

import org.junit.Ignore;
import org.junit.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static woe.simulator.WorldMap.*;

public class WorldMapTest {
  /*  4 + lat
   *    |
   *  3 +
   *    | 2,1
   *  2 +  +--+
   *    |  |  |
   *  1 +  +--+
   *    |    1,2
   *  0 +--+--+--+--+--+ lng
   *    0  1  2  3  4  5
   */
  @Test
  public void overlapRegionSelf() {
    WorldMap.Region region = region(18, topLeft(2, 1), botRight(1, 2));

    assertTrue(region.overlaps(region));
    assertTrue(region.contains(region));
  }

  /*  4 + lat
   *    |
   *  3 +
   *    | 2,1   2,3
   *  2 +  +--+  +--+
   *    |  |  |  |  |
   *  1 +  +--+  +--+
   *    |    1,2   1,4
   *  0 +--+--+--+--+--+ lng
   *    0  1  2  3  4  5
   */
  @Test
  public void nonOverlappingRegionsSideBySide() {
    WorldMap.Region regionLeft = region(18, topLeft(2, 1), botRight(1, 2));
    WorldMap.Region regionRight = region(18, topLeft(2, 3), botRight(1, 4));

    assertFalse(regionRight.overlaps(regionLeft));
    assertFalse(regionLeft.overlaps(regionRight));

    assertFalse(regionRight.contains(regionLeft));
    assertFalse(regionLeft.contains(regionRight));
  }

  /*  6 + lat
   *    |       5,3
   *  5 +        +--+
   *    |        |  |
   *  4 +        +--+
   *    |          4,4
   *  3 +
   *    |       2,3
   *  2 +        +--+
   *    |        |  |
   *  1 +        +--+
   *    |          1,4
   *  0 +--+--+--+--+--+ lng
   *    0  1  2  3  4  5
   */
  @Test
  public void noOverlappingRegionsAboveAndBelow() {
    WorldMap.Region regionAbove = region(18, topLeft(5, 3), botRight(4, 4));
    WorldMap.Region regionBelow = region(18, topLeft(2, 3), botRight(1, 4));

    assertFalse(regionBelow.overlaps(regionAbove));
    assertFalse(regionAbove.overlaps(regionBelow));
  }

  /*            4,3
   *  4 + lat    +--+
   *    |        |  |
   *  3 +        +--+
   *    | 2,1      3,4
   *  2 +  +--+
   *    |  |  |
   *  1 +  +--+
   *    |    1,2
   *  0 +--+--+--+--+--+ lng
   *    0  1  2  3  4  5
   */
  @Test
  public void nonOverlappingRegionsLowerLeftAndUpperRight() {
    WorldMap.Region regionLowerLeft = region(18, topLeft(2, 1), botRight(1, 2));
    WorldMap.Region regionUpperRight = region(18, topLeft(4, 3), botRight(3, 4));

    assertFalse(regionUpperRight.overlaps(regionLowerLeft));
    assertFalse(regionLowerLeft.overlaps(regionUpperRight));
  }

  /*  5 + lat
   *    | 4,1
   *  4 +  +--+--+--+
   *    |  | 3,2    |
   *  3 +  +  +--+  +
   *    |  |  |  |  |
   *  2 +  +  +--+  +
   *    |  |    2,3 |
   *  1 +  +--+--+--+
   *    |          1,4
   *  0 +--+--+--+--+--+ lng
   *    0  1  2  3  4  5
   */
  @Test
  public void overlappingRegions() {
    WorldMap.Region regionOutside = region(18, topLeft(4, 1), botRight(1, 4));
    WorldMap.Region regionInside = region(18, topLeft(3, 2), botRight(2, 3));

    assertTrue(regionOutside.overlaps(regionInside));
    assertTrue(regionInside.overlaps(regionOutside));

    assertTrue(regionOutside.contains(regionInside));
    assertFalse(regionInside.contains(regionOutside));
  }

  /*  5 + lat
   *    |    4,2
   *  4 +     +--+
   *    | 3,1 |  |
   *  3 +  +--+--+--+
   *    |  |  |  |  |
   *  2 +  +--+--+--+
   *    |     |  | 2,4
   *  1 +     +--+
   *    |       1,3
   *  0 +--+--+--+--+--+ lng
   *    0  1  2  3  4  5
   */
  @Test
  public void nonOverlappingAdjoiningLeftRightAboveBelow() {
    WorldMap.Region regionCenter = region(18, topLeft(3, 2), botRight(2, 3));
    WorldMap.Region regionUpperAdjoining = region(18, topLeft(4, 2), botRight(3, 3));
    WorldMap.Region regionLowerAdjoining = region(18, topLeft(2, 2), botRight(1, 3));
    WorldMap.Region regionLeftAdjoining = region(18, topLeft(3, 1), botRight(2, 2));
    WorldMap.Region regionRightAdjoining = region(18, topLeft(3, 3), botRight(2, 4));

    assertFalse(regionCenter.overlaps(regionUpperAdjoining));
    assertFalse(regionCenter.overlaps(regionLowerAdjoining));
    assertFalse(regionCenter.overlaps(regionLeftAdjoining));
    assertFalse(regionCenter.overlaps(regionRightAdjoining));

    assertFalse(regionUpperAdjoining.overlaps(regionCenter));
    assertFalse(regionLowerAdjoining.overlaps(regionCenter));
    assertFalse(regionLeftAdjoining.overlaps(regionCenter));
    assertFalse(regionRightAdjoining.overlaps(regionCenter));

    assertFalse(regionCenter.contains(regionUpperAdjoining));
    assertFalse(regionCenter.contains(regionLowerAdjoining));
    assertFalse(regionCenter.contains(regionLeftAdjoining));
    assertFalse(regionCenter.contains(regionRightAdjoining));

    assertFalse(regionUpperAdjoining.contains(regionCenter));
    assertFalse(regionLowerAdjoining.contains(regionCenter));
    assertFalse(regionLeftAdjoining.contains(regionCenter));
    assertFalse(regionRightAdjoining.contains(regionCenter));
  }

  /*  5 + lat
   *    |    4,2
   *  4 +     +--+--+
   *    | 3,1 |     |
   *  3 +  +--+--+  +
   *    |  |  |  |  |
   *  2 +  +  +--+--+
   *    |  |     | 2,4
   *  1 +  +--+--+
   *    |       1,3
   *  0 +--+--+--+--+--+ lng
   *    0  1  2  3  4  5
   */
  @Test
  public void partiallyOverlappingRegions() {
    WorldMap.Region regionLowerLeft = region(18, topLeft(3, 1), botRight(1, 3));
    WorldMap.Region regionUpperRight = region(18, topLeft(4, 2), botRight(2, 4));

    assertTrue(regionLowerLeft.overlaps(regionUpperRight));
    assertTrue(regionUpperRight.overlaps(regionLowerLeft));
    assertFalse(regionLowerLeft.contains(regionUpperRight));
    assertFalse(regionUpperRight.contains(regionLowerLeft));
  }

  /*
   *  5 +
   *    | 4,1
   *  4 +  +--+--+--+
   *    |  |  |     |
   *  3 +  +--+     +
   *    |  | 3,2    |
   *  2 +  +        +
   *    |  |        |
   *  1 +  +--+--+--+
   *    |          1,4
   *  0 +--+--+--+--+--+
   *    0  1  2  3  4  5
   */
  @Test
  public void regionInsideTopLeft() {
    WorldMap.Region regionOuter = region(18, topLeft(4, 1), botRight(1, 4));
    WorldMap.Region regionInner = region(18, topLeft(4, 1), botRight(3, 2));

    assertTrue(regionInner.overlaps(regionOuter));
    assertTrue(regionOuter.overlaps(regionInner));
    assertTrue(regionOuter.contains(regionInner));
    assertFalse(regionInner.contains(regionOuter));
  }

  /*
   *  5 +
   *    | 4,1   4,3
   *  4 +  +--+--+--+
   *    |  |     |  |
   *  3 +  +     +--+
   *    |  |       3,4
   *  2 +  +        +
   *    |  |        |
   *  1 +  +--+--+--+
   *    |          1,4
   *  0 +--+--+--+--+--+
   *    0  1  2  3  4  5
   */
  @Test
  public void regionInsideTopRight() {
    WorldMap.Region regionOuter = region(18, topLeft(4, 1), botRight(1, 4));
    WorldMap.Region regionInner = region(18, topLeft(4, 3), botRight(3, 4));

    assertTrue(regionInner.overlaps(regionOuter));
    assertTrue(regionOuter.overlaps(regionInner));
    assertTrue(regionOuter.contains(regionInner));
    assertFalse(regionInner.contains(regionOuter));
  }

  /*
   *  5 +
   *    | 4,1
   *  4 +  +--+--+--+
   *    |  |        |
   *  3 +  +        +
   *    |  |    2,3 |
   *  2 +  +     +--+
   *    |  |     |  |
   *  1 +  +--+--+--+
   *    |          1,4
   *  0 +--+--+--+--+--+
   *    0  1  2  3  4  5
   */
  @Test
  public void regionInsideBotRight() {
    WorldMap.Region regionOuter = region(18, topLeft(4, 1), botRight(1, 4));
    WorldMap.Region regionInner = region(18, topLeft(2, 3), botRight(1, 4));

    assertTrue(regionInner.overlaps(regionOuter));
    assertTrue(regionOuter.overlaps(regionInner));
    assertTrue(regionOuter.contains(regionInner));
    assertFalse(regionInner.contains(regionOuter));
  }

  /*
   *  5 +
   *    | 4,1
   *  4 +  +--+--+--+
   *    |  |        |
   *  3 +  +        +
   *    | 2,1       |
   *  2 +  +--+     +
   *    |  |  |     |
   *  1 +  +--+--+--+
   *    |    1,2   1,4
   *  0 +--+--+--+--+--+
   *    0  1  2  3  4  5
   */
  @Test
  public void regionInsideBotLeft() {
    WorldMap.Region regionOuter = region(18, topLeft(4, 1), botRight(1, 4));
    WorldMap.Region regionInner = region(18, topLeft(2, 1), botRight(1, 2));

    assertTrue(regionInner.overlaps(regionOuter));
    assertTrue(regionOuter.overlaps(regionInner));
    assertTrue(regionOuter.contains(regionInner));
    assertFalse(regionInner.contains(regionOuter));
  }

  @Test
  public void adjoiningRegionsDoNotOverLap() {
    final WorldMap.Region region11 = regionAtLatLng(18, topLeft(51.50112939, -0.11687458));
    final WorldMap.Region region12 = regionAtLatLng(18, topLeft(51.50114608, -0.11625767));
    final WorldMap.Region region21 = regionAtLatLng(18, topLeft(51.50059175, -0.11680484));
    final WorldMap.Region region22 = regionAtLatLng(18, topLeft(51.50056837, -0.11625767));

    assertFalse(region11.overlaps(region12));
    assertFalse(region11.overlaps(region21));
    assertFalse(region11.overlaps(region22));
    assertTrue(region11.overlaps(region11));

    assertFalse(region12.overlaps(region11));
    assertFalse(region12.overlaps(region21));
    assertFalse(region12.overlaps(region22));
    assertTrue(region12.overlaps(region12));

    assertFalse(region21.overlaps(region11));
    assertFalse(region21.overlaps(region12));
    assertFalse(region21.overlaps(region22));
    assertTrue(region21.overlaps(region21));

    assertFalse(region22.overlaps(region11));
    assertFalse(region22.overlaps(region12));
    assertFalse(region22.overlaps(region21));
    assertTrue(region22.overlaps(region22));
  }

  @Test
  public void subRegionsForZoom0() {
    List<WorldMap.Region> regions = subRegionsFor(regionForZoom0());

    assertEquals(2, regions.size());

    List<WorldMap.Region> expectedRegions = new ArrayList<>();
    expectedRegions.add(region(1, topLeft(90, -180), botRight(-90, 0)));
    expectedRegions.add(region(1, topLeft(90, 0), botRight(-90, 180)));

    assertTrue(regions.contains(expectedRegions.get(0)));
    assertTrue(regions.contains(expectedRegions.get(1)));
  }

  @Test
  public void subRegionsForZoom1() {
    List<WorldMap.Region> regions = subRegionsFor(regionForZoom0());
    regions = subRegionsFor(regions.get(0));

    assertEquals(9, regions.size());

    List<WorldMap.Region> expectedRegions = new ArrayList<>();
    expectedRegions.add(region(2,
        regions.get(0).topLeft,
        botRight(regions.get(0).topLeft.lat - 60, regions.get(0).topLeft.lng + 60)));
    expectedRegions.add(region(2,
        topLeft(regions.get(8).botRight.lat + 60, regions.get(8).botRight.lng - 60),
        regions.get(8).botRight));

    assertTrue(regions.contains(expectedRegions.get(0)));
    assertTrue(regions.contains(expectedRegions.get(1)));
  }

  @Test
  public void subRegionsForZoom2() {
    List<WorldMap.Region> regions = subRegionsFor(regionForZoom0());
    regions = subRegionsFor(regions.get(0));
    regions = subRegionsFor(regions.get(0));

    assertEquals(9, regions.size());

    List<WorldMap.Region> expectedRegions = new ArrayList<>();
    expectedRegions.add(region(3,
        regions.get(0).topLeft,
        botRight(regions.get(0).topLeft.lat - 20, regions.get(0).topLeft.lng + 20)));
    expectedRegions.add(region(3,
        topLeft(regions.get(8).botRight.lat + 20, regions.get(8).botRight.lng - 20),
        regions.get(8).botRight));

    assertTrue(regions.contains(expectedRegions.get(0)));
    assertTrue(regions.contains(expectedRegions.get(1)));
  }

  @Test
  public void subRegionsForZoom3() {
    List<WorldMap.Region> regions = subRegionsFor(regionForZoom0());
    regions = subRegionsFor(regions.get(0));
    regions = subRegionsFor(regions.get(0));
    regions = subRegionsFor(regions.get(0));

    assertEquals(4, regions.size());

    List<WorldMap.Region> expectedRegions = new ArrayList<>();
    expectedRegions.add(region(4,
        regions.get(0).topLeft,
        botRight(regions.get(0).topLeft.lat - 10, regions.get(0).topLeft.lng + 10)));
    expectedRegions.add(region(4,
        topLeft(regions.get(3).botRight.lat + 10, regions.get(3).botRight.lng - 10),
        regions.get(3).botRight));

    assertTrue(regions.contains(expectedRegions.get(0)));
    assertTrue(regions.contains(expectedRegions.get(1)));
  }

  @Test
  public void regionForEntityIdWorks() {
    WorldMap.Region region0 = regionForZoom0();
    WorldMap.Region region1 = regionForEntityId(entityIdOf(region0));

    assertEquals(region0, region1);

    List<WorldMap.Region> regions = subRegionsFor(regionForZoom0());
    regions = subRegionsFor(regions.get(0));
    for (int z = 1; z < zoomMax - 1; z++) {
      regions = subRegionsFor(regions.get(0));
    }
    regions.forEach(region -> assertEquals(region, regionForEntityId(entityIdOf(region))));
  }

  @Test
  public void subRegionsContainedWithinSuperRegion() {
    List<List<WorldMap.Region>> zoomRegions = zoomRegions();

    WorldMap.Region region0 = regionForZoom0();
    assertTrue(region0.contains(zoomRegions.get(0).get(0)));
    assertTrue(region0.contains(zoomRegions.get(0).get(1)));

    IntStream.range(1, zoomRegions.size()).forEach(zoom ->
        IntStream.range(0, zoomRegions.get(zoom).size()).forEach(srIdx ->
            assertTrue(zoomRegions.get(zoom - 1).get(0).contains(zoomRegions.get(zoom).get(srIdx)))));
  }

  @Test
  public void devicesWithRegionsAndZooms() {
    assertEquals(1, devicesWithin(18));
    assertEquals(1073741824, devicesWithin(3));
    assertEquals(1, devicesWithin(regionAtLatLng(18, topLeft(51, 2))));
    assertEquals(1073741824, devicesWithin(regionAtLatLng(3, topLeft(51, 2))));
  }

  @Test
  public void durationAtRateWorks() {
    assertEquals(Duration.ofSeconds(17 * 60 + 53), durationAtRate(1000000, 3));
    assertEquals(Duration.ofSeconds(1), durationAtRate(1, 18));
    assertEquals(Duration.ofSeconds(4), durationAtRate(1, 17));
    assertEquals(Duration.ofSeconds(256), durationAtRate(4, 13));
    assertEquals(Duration.ofSeconds(devicesWithin(10) / 1000), durationAtRate(1000, 10));
    assertEquals(Duration.ofSeconds(devicesWithin(8) / 4000), durationAtRate(4000, 8));
    assertEquals(Duration.ofSeconds(devicesWithin(8) / 1000), durationAtRate(1000, 8));
  }

  @Test
  public void regionCountForSelectionStackWorks() {
    assertEquals(19, regionCountForSelectionStack(18));
    int count = 17 + IntStream.rangeClosed(17, 18).map(WorldMap::devicesWithin).reduce(0, Integer::sum);
    assertEquals(count, regionCountForSelectionStack(17));
    count = 16 + IntStream.rangeClosed(16, 18).map(WorldMap::devicesWithin).reduce(0, Integer::sum);
    assertEquals(count, regionCountForSelectionStack(16));
    count = 15 + IntStream.rangeClosed(15, 18).map(WorldMap::devicesWithin).reduce(0, Integer::sum);
    assertEquals(count, regionCountForSelectionStack(15));
    count = 3 + IntStream.rangeClosed(3, 18).map(WorldMap::devicesWithin).reduce(0, Integer::sum);
    assertEquals(count, regionCountForSelectionStack(3));
  }

  @Test
  public void regionCountForSelectionAtZoomWorks() {
    assertEquals(4, regionCountForSelectionAtZoom(17, 18));
    assertEquals(1, regionCountForSelectionAtZoom(17, 17));

    assertEquals(16, regionCountForSelectionAtZoom(16, 18));
    assertEquals(4, regionCountForSelectionAtZoom(16, 17));
    assertEquals(1, regionCountForSelectionAtZoom(16, 16));
    assertEquals(1, regionCountForSelectionAtZoom(16, 15));
    assertEquals(1, regionCountForSelectionAtZoom(16, 3));

    assertEquals(devicesWithin(18 - (18 - 10)), regionCountForSelectionAtZoom(10, 18));
    assertEquals(devicesWithin(18 - (17 - 10)), regionCountForSelectionAtZoom(10, 17));
    assertEquals(devicesWithin(18 - (11 - 10)), regionCountForSelectionAtZoom(10, 11));

    assertEquals(devicesWithin(18 - (18 - 6)), regionCountForSelectionAtZoom(6, 18));
    assertEquals(devicesWithin(18 - (7 - 6)), regionCountForSelectionAtZoom(6, 7));
  }

  @Test
  public void rateDelayed() {
    final WorldMap.Region regionSelection = regionAtLatLng(10, topLeft(50, 1));
    final WorldMap.Region regionState = regionAtLatLng(17, topLeft(50, 1));
    final Region.State state = new Region.State(regionState);
    final Instant deadline = Instant.ofEpochMilli(System.currentTimeMillis() + 1000);
    final Region.SelectionCommand selectionCommand = new Region.SelectionCreate(regionSelection, deadline, false, null);
    final Duration untilDeadline = Duration.between(Instant.now(), selectionCommand.deadline);
    final double untilDeadlinePercent = percentForSelectionAtZoom(selectionCommand.region.zoom, state.region.zoom);
    final double randomPercent = untilDeadlinePercent * Math.random();
    final Duration delay = Duration.ofMillis((long) (untilDeadline.toMillis() * randomPercent));
    assertTrue(delay.toMillis() > 0);
  }

  @Ignore
  @Test
  public void percentForSelectionAtZoomWorks() {
    System.out.printf("Selection zoom %d, zoom %d, percent %1.3f%n", 16, 18, percentForSelectionAtZoom(16, 18));
    System.out.printf("Selection zoom %d, zoom %d, percent %1.3f%n", 8, 17, percentForSelectionAtZoom(8, 17));
    final Instant deadline = Instant.ofEpochMilli(2000);
    final Duration between = Duration.between(Instant.ofEpochMilli(1000), deadline);
    final double percent = percentForSelectionAtZoom(3, 18);
    final double delayMs = percent * between.toMillis();
    final double delayPercent = Math.random();
    final Duration delay = Duration.ofMillis((long) (delayMs * delayPercent));
    System.out.printf("delay ms %1.3f, %s%n", delayMs, delay);
  }

  @Ignore
  @Test
  public void percentForSelections() {
    IntStream.rangeClosed(3, 18).forEach(zoomSelection -> {
      IntStream.rangeClosed(3, 18).forEach(zoomCount -> {
        final double regionsForStack = regionCountForSelectionStack(zoomSelection);
        final double regionsForZoom = regionCountForSelectionAtZoom(zoomSelection, zoomCount);
        final double percent = percentForSelectionAtZoom(zoomSelection, zoomCount);
        System.out.printf("%2d, %2d, %1.9f, %,13.0f, %,13.0f%n", zoomSelection, zoomCount, percent, regionsForZoom, regionsForStack);
      });
      // Add 3 to total for zooms 0, 1, 2
      final int total = 3 + IntStream.rangeClosed(3, 18).reduce(0, (t, z) -> t + regionCountForSelectionAtZoom(zoomSelection, z));
      System.out.printf("%,34d->=============%n%n", total);
    });
  }

  // Not a test. Shows the number of devices created per region at zoom levels 3 through 18.
  @Ignore
  @Test
  public void calculateIotDevicesSelectedPerZoomLevel() {
    IntStream.range(0, 16).forEach(zoom ->
        System.out.printf("Zoom %2d %,d%n", 18 - zoom, devicesWithin(18 - zoom)));
  }

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
