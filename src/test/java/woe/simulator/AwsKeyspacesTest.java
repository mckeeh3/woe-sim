package woe.simulator;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import org.junit.Ignore;
import org.junit.Test;

import javax.net.ssl.SSLContext;
import java.net.InetSocketAddress;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.List;

public class AwsKeyspacesTest {
  @Ignore
  @Test
  public void t() throws NoSuchAlgorithmException {
    final List<InetSocketAddress> contactPoints = Collections.singletonList(InetSocketAddress.createUnresolved("cassandra.us-east-1.amazonaws.com", 9142));
    try (final CqlSession cqlSession = CqlSession.builder()
             .addContactPoints(contactPoints)
             .withSslContext(SSLContext.getDefault())
             .build()) {

      final ResultSet resultSet = cqlSession.execute("select * from woe_simulator.messages");
      resultSet.forEach(System.out::println);
    }
  }
}
