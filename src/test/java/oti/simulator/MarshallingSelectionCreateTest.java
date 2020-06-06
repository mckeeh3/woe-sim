package oti.simulator;

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

import static oti.simulator.WorldMap.regionForZoom0;

public class MarshallingSelectionCreateTest extends JUnitRouteTest {
  @Test
  public void testEntitySelectionCreate() {
    final WorldMap.Region selection = regionForZoom0();
    final Region.SelectionCreate selectionCreate = new Region.SelectionCreate(selection, null);
    final Unmarshaller<HttpEntity, Region.SelectionCreate> unmarshaller = Jackson.unmarshaller(Region.SelectionCreate.class);
    final Route route = entity(unmarshaller, sc ->
        complete(sc.toString()));

    testRoute(route).run(
        HttpRequest.POST("/")
            .withEntity(
                HttpEntities.create(ContentTypes.APPLICATION_JSON, toJson(selectionCreate))
            )
    ).assertEntity("SelectionCreate[create, Selection[Region[zoom 0, topLeft LatLng[lat 90.000000, lng -180.000000], botRight LatLng[lat -90.000000, lng 180.000000]]]]");
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
