package woe.simulator;

import akka.actor.testkit.typed.javadsl.TestKitJunitResource;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ClientsTest {
  @ClassRule
  public static final TestKitJunitResource testKit = new TestKitJunitResource();

  @Test
  public void t() {
    final List<Client> clients = Clients.configuredClients(testKit.system());
    assertTrue(clients.size() > 0);
  }
}
