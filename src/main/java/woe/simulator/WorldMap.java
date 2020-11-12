package woe.simulator;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

interface WorldMap {

  final int zoomMax = 18;

  static String entityIdOf(Region region) {
    return String.format("%d:%1.13f:%1.13f:%1.13f:%1.13f", region.zoom,
        region.topLeft.lat, region.topLeft.lng, region.botRight.lat, region.botRight.lng);
  }

  static Region regionForEntityId(String entityId) {
    final var fields = entityId.split(":");
    final var zoom = Integer.parseInt(fields[0]);
    final var topLeftLat = Double.parseDouble(fields[1]);
    final var topLeftLng = Double.parseDouble(fields[2]);
    final var botRightLat = Double.parseDouble(fields[3]);
    final var botRightLng = Double.parseDouble(fields[4]);
    return new WorldMap.Region(zoom, topLeft(topLeftLat, topLeftLng), botRight(botRightLat, botRightLng));
  }

  static LatLng topLeft(double lat, double lng) {
    return new LatLng(lat, lng);
  }

  static LatLng botRight(double lat, double lng) {
    return new LatLng(lat, lng);
  }

  static Region region(int zoom, LatLng topLeft, LatLng botRight) {
    return new Region(zoom, topLeft, botRight);
  }

  static Region regionForZoom0() {
    return region(0, topLeft(90, -180), botRight(-90, 180));
  }

  /* level 0 - 1 region  180 / 360
   * level 1 - 2 regions 180 / 1 x 180 / 2, 180 lat x 180 lng, on either side of lng 0 meridian
   * level 2 - 9 regions 180 / 3 x 180 / 3,  60 lat x  60 lng
   * level 3 - 9 regions  60 / 3 x  60 / 3,  20 lat x  20 lng
   * level 4 - 4 regions  20 / 2 x  20 / 2,  10 lat x  10 lng
   * level 5 - 4 regions  10 / 2 x  10 / 2,   5 lat x   5 lng, subdivide by 4 down to zoom 18
   */
  static List<Region> subRegionsFor(Region region) {
    switch (region.zoom) {
      case 0:
        return subRegionsForZoom0();
      case 1:
      case 2:
        return subRegionsForZoomX(region, 3);
      default:
        return subRegionsForZoomX(region, 2);
    }
  }

  static int devicesWithin(Region region) {
    return devicesWithin(region.zoom);
  }

  static int devicesWithin(int zoom) {
    return (int) Math.pow(4, 18 - zoom);
  }

  static Duration durationAtRate(int actionsPerSecond, int zoom) {
    return Duration.ofSeconds(devicesWithin(zoom) / actionsPerSecond);
  }

  static int regionCountForSelectionStack(int selectionZoom) {
    return IntStream.rangeClosed(0, 18).map(i -> selectionZoom + i).reduce(0, (sum, zoom) -> sum += zoom > 18 ? 1 : devicesWithin(zoom));
  }

  static int regionCountForSelectionAtZoom(int zoomSelection, int zoomCount) {
    return zoomCount < zoomSelection ? 1 : devicesWithin(18 - (zoomCount - zoomSelection));
  }

  static double percentForSelectionAtZoom(int zoomSelection, int zoomCount) {
    final var regionsForStack = regionCountForSelectionStack(zoomSelection);
    final var regionsForZoom = regionCountForSelectionAtZoom(zoomSelection, zoomCount);
    final var regionsForZoom18 = regionCountForSelectionAtZoom(zoomSelection, 18);
    return (regionsForZoom / regionsForStack) / (regionsForZoom18 / regionsForStack);
  }

  private static List<Region> subRegionsForZoom0() {
    final var regions = new ArrayList<Region>();
    regions.add(region(1, topLeft(90, -180), botRight(-90, 0)));
    regions.add(region(1, topLeft(90, 0), botRight(-90, 180)));
    return regions;
  }

  private static List<Region> subRegionsForZoomX(Region region, int splits) {
    final var length = (region.topLeft.lat - region.botRight.lat) / splits;
    final var regions = new ArrayList<Region>();
    if (region.zoom >= zoomMax) {
      return regions;
    }
    IntStream.range(0, splits).forEach(latIndex -> IntStream.range(0, splits).forEach(lngIndex -> {
      final var topLeft = topLeft(region.topLeft.lat - latIndex * length, region.topLeft.lng + lngIndex * length);
      final var botRight = botRight(region.topLeft.lat - (latIndex + 1) * length, region.topLeft.lng + (lngIndex + 1) * length);
      regions.add(region(region.zoom + 1, topLeft, botRight));
    }));
    return regions;
  }

  static Region regionAtLatLng(int zoom, LatLng latLng) {
    return regionAtLatLng(zoom, latLng, regionForZoom0());
  }

  private static Region regionAtLatLng(int zoom, LatLng latLng, Region region) {
    if (zoom == region.zoom) {
      return region;
    }
    final var subRegions = subRegionsFor(region);
    final var subRegionOpt = subRegions.stream().filter(r -> r.contains(latLng)).findFirst();
    return subRegionOpt.map(subRegion -> regionAtLatLng(zoom, latLng, subRegion)).orElse(null);
  }

  class LatLng implements CborSerializable {
    public final double lat;
    public final double lng;

    @JsonCreator
    public LatLng(@JsonProperty("lat") double lat, @JsonProperty("lng") double lng) {
      this.lat = lat;
      this.lng = lng;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      LatLng latLng = (LatLng) o;
      return Double.compare(latLng.lat, lat) == 0 &&
          Double.compare(latLng.lng, lng) == 0;
    }

    @Override
    public int hashCode() {
      return Objects.hash(lat, lng);
    }

    @Override
    public String toString() {
      return String.format("%s[lat %1.13f, lng %1.13f]", getClass().getSimpleName(), lat, lng);
    }
  }

  class Region implements CborSerializable {
    public final int zoom;
    public final LatLng topLeft;
    public final LatLng botRight;

    @JsonCreator
    Region(@JsonProperty("zoom") int zoom, @JsonProperty("topLeft") LatLng topLeft, @JsonProperty("botRight") LatLng botRight) {
      if (zoom < 0 || zoom > 18) {
        throw new IllegalArgumentException("Zoom must be in >= 0 and <= 18.");
      }
      if (topLeft.lat <= botRight.lat) {
        throw new IllegalArgumentException("Top left latitude must be greater than bottom right latitude.");
      }
      if (topLeft.lng >= botRight.lng) {
        throw new IllegalArgumentException("Top left longitude must be less than bottom right longitude.");
      }
      this.zoom = zoom;
      this.topLeft = topLeft;
      this.botRight = botRight;
    }

    boolean overlaps(Region region) {
      return !isThisAbove(region) && !isThisBelow(region) && !isThisLeft(region) && !isThisRight(region);
    }

    private boolean isThisAbove(Region region) {
      return botRight.lat >= region.topLeft.lat;
    }

    private boolean isThisBelow(Region region) {
      return topLeft.lat <= region.botRight.lat;
    }

    private boolean isThisLeft(Region region) {
      return botRight.lng <= region.topLeft.lng;
    }

    private boolean isThisRight(Region region) {
      return topLeft.lng >= region.botRight.lng;
    }

    boolean contains(Region region) {
      return contains(region.topLeft) && contains(region.botRight);
    }

    boolean contains(LatLng latLng) {
      return topLeft.lat >= latLng.lat && botRight.lat <= latLng.lat
          && topLeft.lng <= latLng.lng && botRight.lng >= latLng.lng;
    }

    boolean isDevice() {
      return zoom == zoomMax; // devices are represented at finest zoom in level.
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Region region = (Region) o;
      return zoom == region.zoom &&
          topLeft.equals(region.topLeft) &&
          botRight.equals(region.botRight);
    }

    @Override
    public int hashCode() {
      return Objects.hash(zoom, topLeft, botRight);
    }

    @Override
    public String toString() {
      return String.format("%s[zoom %d, topLeft %s, botRight %s]", getClass().getSimpleName(), zoom, topLeft, botRight);
    }
  }
}
