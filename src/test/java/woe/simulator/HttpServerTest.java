package woe.simulator;

import akka.actor.testkit.typed.javadsl.ActorTestKit;
import akka.actor.testkit.typed.javadsl.SerializationTestKit;
import akka.actor.testkit.typed.javadsl.TestKitJunitResource;
import akka.actor.testkit.typed.javadsl.TestProbe;
import akka.cluster.Cluster;
import akka.cluster.sharding.typed.javadsl.ClusterSharding;
import akka.cluster.sharding.typed.javadsl.Entity;
import akka.http.javadsl.Http;
import akka.http.javadsl.model.*;
import akka.stream.Materializer;
import akka.util.ByteString;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static woe.simulator.WorldMap.regionForZoom0;

public class HttpServerTest {
  private static HttpServer httpServer;
  private static String selectionUrl;

  @ClassRule
  public static final TestKitJunitResource testKit = new TestKitJunitResource(config());

  private static Config config() {
    return ConfigFactory.parseString(
        String.format("akka.cluster.seed-nodes = [ \"akka://%s@127.0.0.1:25520\" ] %n", HttpServerTest.class.getSimpleName())
            + String.format("akka.persistence.snapshot-store.local.dir = \"%s-%s\" %n", "target/snapshot", UUID.randomUUID().toString())
    ).withFallback(ConfigFactory.load("application-test.conf"));
  }

  @BeforeClass
  public static void before() {
    ClusterSharding clusterSharding = ClusterSharding.get(testKit.system());

    clusterSharding.init(
        Entity.of(
            Region.entityTypeKey,
            entityContext ->
                Region.create(entityContext.getEntityId(), clusterSharding)
        )
    );
    testKit.system().log().info("Test cluster node {}", Cluster.get(testKit.system()).selfMember());

    String host = testKit.system().settings().config().getString("woe.simulator.http.server.host");
    int port = testKit.system().settings().config().getInt("woe.simulator.http.server.port");
    httpServer = HttpServer.start(host, port, testKit.system());
    selectionUrl = String.format("http://%s:%d/selection", host, port);
  }

  @Test
  public void submitHttpSelectionZoom16() {
    TestProbe<Region.Command> probe = testKit.createTestProbe();

    // Submit request to create a selected region in London across Westminster Bridge at Park Plaza Hotel
    WorldMap.Region region = WorldMap.regionAtLatLng(16, new WorldMap.LatLng(51.50079211, -0.11682093));
    HttpServer.SelectionActionRequest selectionActionRequest = new HttpServer.SelectionActionRequest("create", 100, region);

    httpServer.replyTo(probe.ref()); // hack to pass probe ref to entity messages

    HttpResponse httpResponse = Http.get(testKit.system().classicSystem())
        .singleRequest(HttpRequest.POST(selectionUrl)
            .withEntity(toHttpEntity(selectionActionRequest)))
        .toCompletableFuture().join();

    probe.receiveSeveralMessages(16, Duration.ofSeconds(30));
    assertTrue(httpResponse.status().isSuccess());

    String response = entityAsString(httpResponse, materializer());
    assertNotNull(response);
    assertTrue(response.contains("\"message\":\"Accepted\""));
  }

  @Ignore
  @Test
  public void thatSelectionCreateIsSerializable() {
    String entity = Http.get(testKit.system().classicSystem())
        .singleRequest(HttpRequest.GET("http://localhost:28080/selection-create"))
        .thenCompose(rsp ->
            rsp.entity().getDataBytes()
                .runReduce(ByteString::concat, materializer())
                .thenApply(ByteString::utf8String)
        ).toCompletableFuture().join();

    assertNotNull(entity);
    assertTrue(entity.contains("\"zoom\":0"));
  }

  @Ignore
  @Test
  public void thatSelectionCreatePostAndReplyAreSerializable() {
    WorldMap.Region selection = regionForZoom0();
    Region.SelectionCreate selectionCreate = new Region.SelectionCreate(selection, Instant.now(), false, null);

    HttpResponse httpResponse = Http.get(testKit.system().classicSystem())
        .singleRequest(HttpRequest.POST("http://localhost:28080/selection-create")
            .withEntity(toHttpEntity(selectionCreate)))
        .toCompletableFuture().join();

    assertTrue(httpResponse.status().isSuccess());
    String entity = entityAsString(httpResponse, materializer());
    assertNotNull(entity);
  }

  @Test
  public void serializeDeserializeSelectionActionRequest() {
    final SerializationTestKit serializationTestKit = ActorTestKit.create(testKit.system()).serializationTestKit();

    final HttpServer.SelectionActionRequest selectionActionRequest = new HttpServer.SelectionActionRequest("create", 100, 3, 1, 1, 0, 0);
    serializationTestKit.verifySerialization(selectionActionRequest, true);
  }

  @Test
  public void serializeDeserializeSelectionActionResponse() {
    final SerializationTestKit serializationTestKit = ActorTestKit.create(testKit.system()).serializationTestKit();

    final HttpServer.SelectionActionRequest selectionActionRequest = new HttpServer.SelectionActionRequest("create", 100, 3, 1, 1, 0, 0);
    final HttpServer.SelectionActionResponse selectionActionResponse = new HttpServer.SelectionActionResponse("test", 200, selectionActionRequest);
    serializationTestKit.verifySerialization(selectionActionResponse, true);
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

  private static Materializer materializer() {
    return Materializer.matFromSystem(testKit.system().classicSystem());
  }

  private static String entityAsString(HttpResponse httpResponse, Materializer materializer) {
    return httpResponse.entity().getDataBytes()
        .runReduce(ByteString::concat, materializer)
        .thenApply(ByteString::utf8String)
        .toCompletableFuture().join();
  }
}
