package oti.simulator;

import akka.persistence.typed.PersistenceId;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

interface WorldMap {

  static String entityIdOf(Region region) {
    return String.format("region:%d:%f:%f:%f:%f", region.zoom,
        region.topLeft.lat, region.topLeft.lng, region.botRight.lat, region.botRight.lng);
  }

  static PersistenceId persistenceIdOf(Region region) {
    return PersistenceId.ofUniqueId(entityIdOf(region));
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
  static List<Region> subRegionFor(Region region) {
    switch (region.zoom) {
      case 0:
        return subRegionsForZoom0(region);
      case 1:
      case 2:
        return subRegionsForZoomX(region, 3);
      default:
        return subRegionsForZoomX(region, 2);
    }
  }

  private static List<Region> subRegionsForZoom0(Region region) {
    List<Region> regions = new ArrayList<>();
    regions.add(region(1, topLeft(90, -180), botRight(-90, 0)));
    regions.add(region(1, topLeft(90, 0), botRight(-90, 180)));
    return regions;
  }

  private static List<Region> subRegionsForZoomX(Region region, int splits) {
    final double length = (region.topLeft.lat - region.botRight.lat) / splits;
    List<Region> regions = new ArrayList<>();
    IntStream.range(0, splits).forEach(latIndex -> {
      IntStream.range(0, splits).forEach(lngIndex -> {
        final LatLng topLeft = topLeft(region.topLeft.lat - latIndex * length, region.topLeft.lng + lngIndex * length);
        final LatLng botRight = botRight(region.topLeft.lat - (latIndex + 1) * length, region.topLeft.lng + (lngIndex + 1) * length);
        regions.add(region(region.zoom + 1, topLeft, botRight));
      });
    });
    return regions;
  }

  static Region regionForEntityId(String entityId) {
    String[] fields = entityId.split(":");
    int zoom = Integer.parseInt(fields[1]);
    double topLeftLat = Double.parseDouble(fields[2]);
    double topLeftLng = Double.parseDouble(fields[3]);
    double botRightLat = Double.parseDouble(fields[4]);
    double botRightLng = Double.parseDouble(fields[5]);
    return new WorldMap.Region(zoom, topLeft(topLeftLat, topLeftLng), botRight(botRightLat, botRightLng));
  }

  class LatLng implements CborSerializable {
    final double lat;
    final double lng;

    LatLng(double lat, double lng) {
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
      return String.format("%s[lat %f, lng %f]", getClass().getSimpleName(), lat, lng);
    }
  }

  class Region implements CborSerializable {
    final int zoom;
    final LatLng topLeft;
    final LatLng botRight;

    Region(int zoom, LatLng topLeft, LatLng botRight) {
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
      return isInside(region.topLeft) && isInside(region.botRight);
    }

    private boolean isInside(LatLng latLng) {
      return topLeft.lat >= latLng.lat && botRight.lat <= latLng.lat
          && topLeft.lng <= latLng.lng && botRight.lng >= latLng.lng;
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
