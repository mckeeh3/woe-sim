package woe.simulator;

import akka.actor.testkit.typed.javadsl.TestKitJunitResource;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class GrpcClientTest {
  @ClassRule
  public static final TestKitJunitResource testKit = new TestKitJunitResource();

  @Test
  public void noConfigSettingsHostPortIsEmpty() {
    final Optional<GrpcClient.HostPort> hostPort = GrpcClient.hostPort(testKit.system());
    assertTrue(hostPort.isEmpty());
  }
}
