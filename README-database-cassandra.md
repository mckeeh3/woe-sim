
# Installation and Setup of Cassandra

Follow these instructions for setting up a database for the `woe-sim` microservice.

## Akka Persistence Cassandra

There are a number of available database services to choose from. Akka Persistence provides multiple 
[Persistence Plugins](https://doc.akka.io/docs/akka/current/persistence-plugins.html).

This section provides instructions for setting up Cassandra. Please see the the 
[Akka Persistence Cassandra](https://doc.akka.io/docs/akka-persistence-cassandra/current/)
documentation for additional details.

See either 
[README-database-cassandra-postgres.md](https://github.com/mckeeh3/woe-sim/blob/master/README-database-cassandra-postgres.md) 
or 
[README-database-yugabyte.md](https://github.com/mckeeh3/woe-sim/blob/master/README-database-yugabyte.md)
for instructions for setting up Cassandra.


### Amazon Keyspaces

[Amazon Keyspaces](https://aws.amazon.com/keyspaces/) is a serverless AWS service for Cassandra. The service provides a web interface for creating keypsaces and tables.

Use the online CQL Editor to create each of the siz required Cassandra tables. To access the CQL Editor, from the 
[Amazon Keyspaces](https://aws.amazon.com/keyspaces/)
page, click **Get started with Amazon Keyspaces**.

On the left panel, click **Keypsaces**. Then on the right click **Create keyspace**. Create a keyspace  with the name `woe-sim`.

Next, on the left panel, click **CQL editor**. From the `/src/main/resources/akka-persistence-journal-create.cql` file copy the lines for creating each table and past them into the CQL editor then click **Run command**. Repeat these steps for each of the siz tables.

### Local Cassandra for Minikube

With Minikube you can use a local Cassandra installation. First, 
[download Cassandra](https://cassandra.apache.org/download/). Follow the 
[Installation Instructions](https://cassandra.apache.org/doc/latest/getting_started/installing.html).

**Important:** change the `listen_address` setting in the `<install-dir>/conf/conf/cassandra.yaml` configuration file to an IP address that is accessible from within Minikube. This is usually the IP of your local system. Processes running within Minikube cannot access the default localhost that Cassandra is configured to use. 

### Other Cassandra Providers

We will update this document as we test with other Cassandra installations. 

If you have tried other Cassandra providers please submit a pull request.
