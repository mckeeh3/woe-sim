package oti.simulator;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static oti.simulator.WorldMap.*;

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
  public void subRegionsForZoom0() {
    List<WorldMap.Region> regions = subRegionFor(regionForZoom0());

    assertEquals(2, regions.size());

    List<WorldMap.Region> expectedRegions = new ArrayList<>();
    expectedRegions.add(region(1, topLeft(90, -180), botRight(-90, 0)));
    expectedRegions.add(region(1, topLeft(90, 0), botRight(-90, 180)));

    assertTrue(regions.contains(expectedRegions.get(0)));
    assertTrue(regions.contains(expectedRegions.get(1)));
  }

  @Test
  public void subRegionsForZoom1() {
    List<WorldMap.Region> regions = subRegionFor(regionForZoom0());
    regions = subRegionFor(regions.get(0));

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
    List<WorldMap.Region> regions = subRegionFor(regionForZoom0());
    regions = subRegionFor(regions.get(0));
    regions = subRegionFor(regions.get(0));

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
    List<WorldMap.Region> regions = subRegionFor(regionForZoom0());
    regions = subRegionFor(regions.get(0));
    regions = subRegionFor(regions.get(0));
    regions = subRegionFor(regions.get(0));

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
  public void regionForEntityId() {
    WorldMap.Region region = regionForZoom0();
    WorldMap.Region region0 = WorldMap.regionForEntityId(entityIdOf(region));
  }
}
