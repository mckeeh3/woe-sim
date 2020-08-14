package woe.simulator;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

interface Telemetry {
  class TelemetryRequest {
    public final String action;
    public final int zoom;
    public final double topLeftLat;
    public final double topLeftLng;
    public final double botRightLat;
    public final double botRightLng;

    TelemetryRequest(String action, WorldMap.Region region) {
      this(action, region.zoom, region.topLeft.lat, region.topLeft.lng, region.botRight.lat, region.botRight.lng);
    }

    @JsonCreator
    public TelemetryRequest(
        @JsonProperty("action") String action,
        @JsonProperty("zoom") int zoom,
        @JsonProperty("topLeftLat") double topLeftLat,
        @JsonProperty("topLeftLng") double topLeftLng,
        @JsonProperty("botRightLat") double botRightLat,
        @JsonProperty("botRightLng") double botRightLng) {
      this.action = action;
      this.zoom = zoom;
      this.topLeftLat = topLeftLat;
      this.topLeftLng = topLeftLng;
      this.botRightLat = botRightLat;
      this.botRightLng = botRightLng;
    }

    @Override
    public String toString() {
      return String.format("%s[%s, %d, %1.9f, %1.9f, %1.9f, %1.9f]", getClass().getSimpleName(), action, zoom, topLeftLat, topLeftLng, botRightLat, botRightLng);
    }
  }

  class TelemetryResponse {
    public final String message;
    public final int httpStatusCode;
    public final TelemetryRequest telemetryRequest;

    @JsonCreator
    public TelemetryResponse(
        @JsonProperty("message") String message,
        @JsonProperty("httpStatusCode") int httpStatusCode,
        @JsonProperty("deviceTelemetryRequest") TelemetryRequest telemetryRequest) {
      this.message = message;
      this.httpStatusCode = httpStatusCode;
      this.telemetryRequest = telemetryRequest;
    }

    @Override
    public String toString() {
      return String.format("%s[%d, %s, %s]", getClass().getSimpleName(), httpStatusCode, message, telemetryRequest);
    }
  }
}
