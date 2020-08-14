package woe.simulator;

import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.model.ContentTypes;
import akka.http.javadsl.model.HttpEntities;
import akka.http.javadsl.model.HttpEntity;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.server.Route;
import akka.http.javadsl.testkit.JUnitRouteTest;
import akka.http.javadsl.unmarshalling.Unmarshaller;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.junit.Test;

import java.time.Instant;

import static woe.simulator.WorldMap.regionForZoom0;

public class MarshallingTelemetryRequestTest extends JUnitRouteTest {
  @Test
  public void testEntitySelectionCreate() {
    final WorldMap.Region selection = regionForZoom0();
    final Region.SelectionCreate selectionCreate = new Region.SelectionCreate(selection, Instant.ofEpochMilli(0), false, null);
    final Telemetry.TelemetryRequest telemetryRequest = new Telemetry.TelemetryRequest(selectionCreate.action.name(), selectionCreate.region);
    final Unmarshaller<HttpEntity, Telemetry.TelemetryRequest> unmarshaller = Jackson.unmarshaller(Telemetry.TelemetryRequest.class);
    final Route route = entity(unmarshaller, sc ->
        complete(sc.toString()));

    testRoute(route).run(
        HttpRequest.POST("/")
            .withEntity(
                HttpEntities.create(ContentTypes.APPLICATION_JSON, toJson(telemetryRequest))
            )
    ).assertEntity("TelemetryRequest[create, 0, 90.000000000, -180.000000000, -90.000000000, 180.000000000]");
  }

  private static String toJson(Object pojo) {
    final ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
    try {
      return ow.writeValueAsString(pojo);
    } catch (JsonProcessingException e) {
      return String.format("{ \"error\" : \"%s\" }", e.getMessage());
    }
  }
}
