package woe.simulator;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import org.junit.Ignore;
import org.junit.Test;

import javax.net.ssl.SSLContext;
import java.security.NoSuchAlgorithmException;

public class AwsKeyspacesTest {
  @Ignore
  @Test
  public void queryTables() throws NoSuchAlgorithmException {
    try (final CqlSession cqlSession = CqlSession.builder()
             .withSslContext(SSLContext.getDefault())
             .build()) {

      ResultSet resultSet = cqlSession.execute("select * from woe_simulator.messages");
      System.out.printf("Rows in messages table %,d%n", resultSet.all().size());
      resultSet.forEach(System.out::println);

      resultSet = cqlSession.execute("select * from woe_simulator.metadata");
      System.out.printf("Rows in metedata table %,d%n", resultSet.all().size());
      resultSet.forEach(System.out::println);
    }
  }
}
