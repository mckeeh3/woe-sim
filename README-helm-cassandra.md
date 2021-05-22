
# Deploy Cassandra to Kubernetes cluster

Follow these instructions to deploy Cassandra to a Kubernetes cluster.

## Prerequisites

Clone the weo-sim Github project.

~~~bash
git clone https://github.com/mckeeh3/woe-sim.git
~~~

[Install Helm](https://helm.sh/docs/intro/install/).

## Install Cassandra using Helm

You can find Cassandra Helm charts at [Artifact Hub](https://artifacthub.io/).

Shown here is the installation of the [Bitnami Cassandra chart](https://artifacthub.io/packages/helm/bitnami/cassandra).

~~~bash
helm repo add bitnami https://charts.bitnami.com/bitnami
~~~

~~~text
"bitnami" has been added to your repositories
~~~

### Create a `cassandra` namespace

~~~bash
kubectl create namespace cassandra
~~~

### Install Cassandra into to `cassandra` namespace

~~~bash
helm install -n cassandra cassandra bitnami/cassandra
~~~

~~~text
NAME: cassandra
LAST DEPLOYED: Fri May 21 12:46:19 2021
NAMESPACE: cassandra
STATUS: deployed
REVISION: 1
TEST SUITE: None
NOTES:
** Please be patient while the chart is being deployed **

Cassandra can be accessed through the following URLs from within the cluster:

  - CQL: cassandra.cassandra.svc.cluster.local:9042
  - Thrift: cassandra.cassandra.svc.cluster.local:9160

To get your password run:

   export CASSANDRA_PASSWORD=$(kubectl get secret --namespace "cassandra" cassandra -o jsonpath="{.data.cassandra-password}" | base64 --decode)

Check the cluster status by running:

   kubectl exec -it --namespace cassandra $(kubectl get pods --namespace cassandra -l app=cassandra,release=cassandra -o jsonpath='{.items[0].metadata.name}') nodetool status

To connect to your Cassandra cluster using CQL:

1. Run a Cassandra pod that you can use as a client:

   kubectl run --namespace cassandra cassandra-client --rm --tty -i --restart='Never' \
   --env CASSANDRA_PASSWORD=$CASSANDRA_PASSWORD \
    \
   --image docker.io/bitnami/cassandra:3.11.10-debian-10-r78 -- bash

2. Connect using the cqlsh client:

   cqlsh -u cassandra -p $CASSANDRA_PASSWORD cassandra

To connect to your database from outside the cluster execute the following commands:

   kubectl port-forward --namespace cassandra svc/cassandra 9042:9042 &
   cqlsh -u cassandra -p $CASSANDRA_PASSWORD 127.0.0.1 9042
~~~

Use the following command to show the above output again for reference.

~~~bash
helm test -n cassandra cassandra
~~~

### Create Cassandra environment variable

Create an environment variable that contains the Cassandra password.

~~~bash
export CASSANDRA_PASSWORD=$(kubectl get secret --namespace "cassandra" cassandra -o jsonpath="{.data.cassandra-password}" | base64 --decode)
~~~

### Create Cassandra secret

Create a secret that contains the Cassandra username, password, and JDBC URL.

~~~bash
kubectl create secret -n woe-sim generic cassandra-env \
--from-literal=cassandra_username=cassandra \
--from-literal=cassandra_password=$CASSANDRA_PASSWORD \
--from-literal=cassandra_host_port=cassandra.cassandra.svc.cluster.local:9042
~~~

## Create the Akka Persistence Cassandra tables

Create an environment variable that contains the Cassandra password.

~~~bash
export CASSANDRA_PASSWORD=$(kubectl get secret --namespace "cassandra" cassandra -o jsonpath="{.data.cassandra-password}" | base64 --decode)
~~~

Run a Cassandra pod that can be used to create the Akka Persistence tables.

~~~bash
kubectl run --namespace cassandra cassandra-client --rm --tty -i --restart='Never' \
--env CASSANDRA_PASSWORD=$CASSANDRA_PASSWORD \
--image docker.io/bitnami/cassandra:3.11.10-debian-10-r78 -- bash
~~~

From the client pod run the `cqlsh` command to start a Cassandra shell connected to the database.

~~~bash
cqlsh -u cassandra -p $CASSANDRA_PASSWORD cassandra
~~~

~~~text
Connected to cassandra at cassandra:9042.
[cqlsh 5.0.1 | Cassandra 3.11.10 | CQL spec 3.4.4 | Native protocol v4]
Use HELP for help.
cassandra@cqlsh>
~~~

From another terminal, copy the CQL DDL file from the woe-sim project directory to the Cassandra client pod.

~~~bash
kubectl get pods -n cassandra
~~~

~~~text
NAME               READY   STATUS    RESTARTS   AGE
cassandra-0        1/1     Running   0          73m
cassandra-client   1/1     Running   0          10m
~~~

~~~bash
kubectl cp src/main/resources/akka-persistence-journal-create-sim.cql cassandra/cassandra-client:/tmp
~~~

From the terminal running `cqlsh` run the following command.

~~~bash
cassandra@cqlsh> source '/tmp/akka-persistence-journal-create-sim.cql'
~~~

Verify that the `woe_simulator` keyspace has been created.

~~~bash
cassandra@cqlsh> describe keyspaces;
~~~

~~~text
system_schema  system         system_distributed
system_auth    woe_simulator  system_traces
~~~

You should see that the `woe_simulator` keyspace in the list.

Next, verify the the required tables have been created.

~~~bash
cassandra@cqlsh> use woe_simulator;
cassandra@cqlsh:woe_simulator> describe tables;
~~~

~~~text
tag_views  tag_scanning         tag_write_progress
messages   all_persistence_ids  metadata
~~~

Six tables should be listed in the `woe_simulator` keyspace.

Finally, quit the `cqlsh` and exit the shell. This will terminate the `cassandra-client` pod.

~~~bash
cassandra@cqlsh:woe_simulator> quit
I have no name!@cassandra-client:/$ exit
exit
pod "cassandra-client" deleted
~~~
