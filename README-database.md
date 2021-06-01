
# Setup a database

The woe-sim microservice uses Akka Persistence for storing events in a CQRS Event Store. See the
[Akka Persistence](https://doc.akka.io/docs/akka/current/typed/persistence.html) documentation for details.

You have the option to use either Cassandra or JDBC for the Akka Persistence event store.

Provided here are links to instructions for setting up various database in various environments.

[Cassandra](https://github.com/mckeeh3/woe-sim/blob/master/README-database-cassandra-helm.md)
deployed to a Kubernetes cluster using helm.

[PostgreSQL](https://github.com/mckeeh3/woe-sim/blob/master/README-database-postgresql-helm.md)
deployed to a kubernetes cluster using helm.

[Yugabyte](https://github.com/mckeeh3/woe-sim/blob/master/README-database-yugabyte-kubernetes.md), which provides both Cassandra and PostgreSQL, deployed to a kubernetes cluster.
