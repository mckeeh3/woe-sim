package oti.simulator;

import akka.actor.ActorSystem;
import akka.actor.testkit.typed.javadsl.TestKitJunitResource;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.model.*;
import akka.http.javadsl.server.Route;
import akka.stream.Materializer;
import akka.util.ByteString;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.concurrent.CompletionStage;

import static akka.http.javadsl.server.Directives.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static oti.simulator.WorldMap.regionForZoom0;

public class HttpServerTest {
  private static CompletionStage<ServerBinding> httpServerBinding;

  @ClassRule
  public static final TestKitJunitResource testKit = new TestKitJunitResource();

  @BeforeClass
  public static void before() {
    httpServerBinding = httpServer("localhost", 28080);
  }

  @Test
  public void readTextFile() {
    HttpResponse httpResponse = Http.get(testKit.system().classicSystem())
        .singleRequest(HttpRequest.GET("http://localhost:28080/application-test.conf"))
        .toCompletableFuture().join();

    assertTrue(httpResponse.status().isSuccess());
    Materializer materializer = Materializer.matFromSystem(testKit.system().classicSystem());
    String entity = entityAsString(httpResponse, materializer);
    assertNotNull(entity);
    assertTrue(entity.startsWith("akka {"));
  }

  @Test
  public void readXmlFile() {
    HttpResponse httpResponse = Http.get(testKit.system().classicSystem())
        .singleRequest(HttpRequest.GET("http://localhost:28080/logback-test.xml"))
        .toCompletableFuture().join();

    assertTrue(httpResponse.status().isSuccess());
    Materializer materializer = Materializer.matFromSystem(testKit.system().classicSystem());
    String entity = entityAsString(httpResponse, materializer);
    assertNotNull(entity);
    assertTrue(entity.startsWith("<configuration>"));
  }

  @Test
  public void thatSelectionCreateIsSerializable() {
    Materializer materializer = Materializer.matFromSystem(testKit.system().classicSystem());

    String entity = Http.get(testKit.system().classicSystem())
        .singleRequest(HttpRequest.GET("http://localhost:28080/selection-create"))
        .thenCompose(rsp ->
            rsp.entity().getDataBytes()
                .runReduce(ByteString::concat, materializer)
                .thenApply(ByteString::utf8String)
        ).toCompletableFuture().join();

    assertNotNull(entity);
    assertTrue(entity.contains("\"zoom\":0"));
  }

  @Test
  public void thatSelectionCreatePostAndReplyAreSerializable() {
    WorldMap.Region selection = regionForZoom0();
    Region.SelectionCreate selectionCreate = new Region.SelectionCreate(selection, null);

    HttpResponse httpResponse = Http.get(testKit.system().classicSystem())
        .singleRequest(HttpRequest.POST("http://localhost:28080/selection-create")
            .withEntity(toHttpEntity(selectionCreate)))
        .toCompletableFuture().join();

    assertTrue(httpResponse.status().isSuccess());
    Materializer materializer = Materializer.matFromSystem(testKit.system().classicSystem());
    String entity = entityAsString(httpResponse, materializer);
    assertNotNull(entity);
  }

  private static HttpEntity.Strict toHttpEntity(Object pojo) {
    return HttpEntities.create(ContentTypes.APPLICATION_JSON, toJson(pojo).getBytes());
  }

  private static String toJson(Object pojo) {
    final ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
    try {
      return ow.writeValueAsString(pojo);
    } catch (JsonProcessingException e) {
      return String.format("{ \"error\" : \"%s\" }", e.getMessage());
    }
  }

  private static CompletionStage<ServerBinding> httpServer(String host, int port) {
    ActorSystem actorSystem = testKit.system().classicSystem();
    Materializer materializer = Materializer.matFromSystem(actorSystem);

    return Http.get(actorSystem.classicSystem())
        .bindAndHandle(route().flow(actorSystem.classicSystem(), materializer),
            ConnectHttp.toHost(host, port), materializer);
  }

  private static Route route() {
    return concat(
        path("application-test.conf", () -> getFromResource("application-test.conf", ContentTypes.TEXT_PLAIN_UTF8)),
        path("logback-test.xml", () -> getFromResource("logback-test.xml", ContentTypes.TEXT_XML_UTF8)),
        path("selection-create", () -> concat(
            get(() -> {
              WorldMap.Region selection = regionForZoom0();
              Region.SelectionCreate selectionCreate = new Region.SelectionCreate(selection, null);
              return complete(StatusCodes.OK, selectionCreate, Jackson.marshaller());
            }),
            post(() -> entity(
                Jackson.unmarshaller(Region.SelectionCreate.class),
                selectionCreate -> {
                  return complete(StatusCodes.CREATED, selectionCreate, Jackson.marshaller());
                })
            )
        ))
    );
  }

  private static String entityAsString(HttpResponse httpResponse, Materializer materializer) {
    return httpResponse.entity().getDataBytes()
        .runReduce(ByteString::concat, materializer)
        .thenApply(ByteString::utf8String)
        .toCompletableFuture().join();
  }
}
