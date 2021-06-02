
# Setup Yugabyte in your Kubernetes Cluster

These instructions are for setting up Yugabyte in your Kubernetes cluster. This applies to Minikube, EKS, GKE, and other Kubernetes environments.

Yugabyte provides APIs for both Cassandra and PostgreSQL.

- [Setup Yugabyte in your Kubernetes Cluster](#setup-yugabyte-in-your-kubernetes-cluster)
  - [Deploy Yugabyte to Kubernetes](#deploy-yugabyte-to-kubernetes)
    - [Create namespace](#create-namespace)
    - [Deploy Yugabyte using helm](#deploy-yugabyte-using-helm)
  - [Select database type](#select-database-type)
  - [Yugabyte Cassandra](#yugabyte-cassandra)
    - [Verify Access to the CQL shell](#verify-access-to-the-cql-shell)
    - [Copy CQL DDL commands to the Yugabyte server](#copy-cql-ddl-commands-to-the-yugabyte-server)
    - [Create the CQL Tables](#create-the-cql-tables)
    - [Verify that the CQL tables have been created](#verify-that-the-cql-tables-have-been-created)
  - [Yugabyte JDBC PostgreSQL](#yugabyte-jdbc-postgresql)
    - [Verify access to the SQL shell](#verify-access-to-the-sql-shell)
    - [Copy SQL DDL commands to the Yugabyte server](#copy-sql-ddl-commands-to-the-yugabyte-server)
    - [Create the SQL Tables](#create-the-sql-tables)
    - [Verify that the SQL tables have been created](#verify-that-the-sql-tables-have-been-created)

## Deploy Yugabyte to Kubernetes

You can follow the recommended steps below or follow the documentation for installing
[Yugabyte](https://docs.yugabyte.com/latest/deploy/).

For performance testing use the above Yugabyte documentation.

### Create namespace

~~~bash
kubectl create namespace yugabyte-db
~~~

~~~text
namespace/yugabyte-db created
~~~

### Deploy Yugabyte using helm

The following steps are similar to the steps provided [here](https://docs.yugabyte.com/latest/deploy/kubernetes/single-zone/oss/helm-chart/).

To add the YugabyteDB charts repository, run the following command.

~~~bash
helm repo add yugabytedb https://charts.yugabyte.com
~~~

Make sure that you have the latest updates to the repository by running the following command.

~~~bash
helm repo update
~~~

Validate the chart version

~~~bash
helm search repo yugabytedb/yugabyte
~~~

~~~text
NAME               	CHART VERSION	APP VERSION	DESCRIPTION
yugabytedb/yugabyte	2.7.1        	2.7.1.1-b1 	YugabyteDB is the high-performance distributed ...
~~~

Specify Kubernetes pod replicas, CPU request and limit when doing the `helm install` step.
Adjust the number of replicas and CPU settings as needed.

~~~bash
helm install yugabyte-db yugabytedb/yugabyte --namespace yugabyte-db --wait --set \
replicas.tserver=3,\
resource.tserver.requests.cpu=2,\
resource.tserver.limits.cpu=4
~~~

As shown in the Yugabyte documentation, verify the status of the deployment using the following command.

~~~bash
helm status yugabyte-db -n yugabyte-db
~~~

~~~text
NAME: yugabyte-db
LAST DEPLOYED: Wed Jun  2 11:34:56 2021
NAMESPACE: yugabyte-db
STATUS: deployed
REVISION: 1
TEST SUITE: None
NOTES:
1. Get YugabyteDB Pods by running this command:
  kubectl --namespace yugabyte-db get pods

2. Get list of YugabyteDB services that are running:
  kubectl --namespace yugabyte-db get services

3. Get information about the load balancer services:
  kubectl get svc --namespace yugabyte-db

4. Connect to one of the tablet server:
  kubectl exec --namespace yugabyte-db -it yb-tserver-0 bash

5. Run YSQL shell from inside of a tablet server:
  kubectl exec --namespace yugabyte-db -it yb-tserver-0 -- /home/yugabyte/bin/ysqlsh -h yb-tserver-0.yb-tservers.yugabyte-db

6. Cleanup YugabyteDB Pods
  For helm 2:
  helm delete yugabyte-db --purge
  For helm 3:
  helm delete yugabyte-db -n yugabyte-db
  NOTE: You need to manually delete the persistent volume
  kubectl delete pvc --namespace yugabyte-db -l app=yb-master
  kubectl delete pvc --namespace yugabyte-db -l app=yb-tserver
~~~

## Select database type

The Yugabyte DB provides APIs for either Cassandra or JDBC. You can use either DB type with Akka Persistence, which is what the `woe-sim` uses for its event log.
Follow the steps in one of the sections below based on the database type you plan to use.

## Yugabyte Cassandra

### Verify Access to the CQL shell

Try the following commands to verify access to Cassandra CQL
CQL CLI tool once Yugabyte has been installed in a Kubernetes environment.

~~~bash
kubectl --namespace yugabyte-db exec -it yb-tserver-0 -- /home/yugabyte/bin/ycqlsh yb-tserver-0
~~~

~~~text
Defaulting container name to yb-tserver.
Use 'kubectl describe pod/yb-tserver-0 -n yugabyte-db' to see all of the containers in this pod.
Connected to local cluster at yb-tserver-0:9042.
[ycqlsh 5.0.1 | Cassandra 3.9-SNAPSHOT | CQL spec 3.4.2 | Native protocol v4]
Use HELP for help.
ycqlsh>
~~~

Quit the CQL shell.

~~~bash
quit
~~~

### Copy CQL DDL commands to the Yugabyte server

From the woe-sim project directory.

~~~bash
kubectl cp src/main/resources/akka-persistence-journal-create-sim.cql yugabyte-db/yb-tserver-0:/tmp
~~~

### Create the CQL Tables

Start `ycqlsh` on a pod.

~~~bash
kubectl --namespace yugabyte-db exec -it yb-tserver-0 -- /home/yugabyte/bin/ycqlsh yb-tserver-0
~~~

~~~text
Defaulting container name to yb-tserver.
Use 'kubectl describe pod/yb-tserver-0 -n yugabyte-db' to see all of the containers in this pod.
Connected to local cluster at yb-tserver-0:9042.
[ycqlsh 5.0.1 | Cassandra 3.9-SNAPSHOT | CQL spec 3.4.2 | Native protocol v4]
Use HELP for help.
ycqlsh>
~~~

Execute the CQL DDL from the copied in the above step file.

~~~bash
source '/tmp/akka-persistence-journal-create-sim.cql'
~~~

### Verify that the CQL tables have been created

~~~bash
describe keyspaces;
~~~

~~~text
woe_simulator  system_schema  system_auth  system
~~~

Verify that the CQL tables have been created.

~~~bash
use woe_simulator;
describe tables;
~~~

~~~text
tag_views  tag_scanning         tag_write_progress
messages   all_persistence_ids  metadata

ycqlsh:woe_simulator>quit
~~~

Quit the `ycqlsh'.

~~~bash
quit
~~~

## Yugabyte JDBC PostgreSQL

### Verify access to the SQL shell

Try the following commands to verify access to PostgreSQL
SQL CLI tool once Yugabyte has been installed in a Kubernetes environment.

~~~bash
kubectl --namespace yugabyte-db exec -it yb-tserver-0 -- /home/yugabyte/bin/ysqlsh
~~~

~~~text
Defaulted container "yb-tserver" out of: yb-tserver, yb-cleanup
ysqlsh (11.2-YB-2.7.1.1-b0)
Type "help" for help.

yugabyte=#
~~~

Quit the SQL shell.

~~~bash
\q
~~~

### Copy SQL DDL commands to the Yugabyte server

From the woe-sim project directory.

~~~bash
kubectl cp src/main/resources/akka-persistence-journal-create-sim.sql yugabyte-db/yb-tserver-0:/tmp
~~~

### Create the SQL Tables

Start `ysqlsh` on a pod.

~~~bash
kubectl --namespace yugabyte-db exec -it yb-tserver-0 -- /home/yugabyte/bin/ysqlsh
~~~

~~~text
Defaulted container "yb-tserver" out of: yb-tserver, yb-cleanup
ysqlsh (11.2-YB-2.7.1.1-b0)
Type "help" for help.

yugabyte=#
~~~

Execute the SQL DDL from the copied in the above step file.

~~~bash
\i /tmp/akka-persistence-journal-create-sim.sql
~~~

~~~text
CREATE TABLE
CREATE INDEX
CREATE TABLE
yugabyte=#
~~~

### Verify that the SQL tables have been created

~~~bash
\d
~~~

~~~text
                         List of relations
 Schema |                Name                |   Type   |  Owner
--------+------------------------------------+----------+----------
 public | woe_sim_event_journal              | table    | yugabyte
 public | woe_sim_event_journal_ordering_seq | sequence | yugabyte
 public | woe_sim_event_tag                  | table    | yugabyte
(3 rows)
~~~

Quit the SQL shell

~~~bash
\q
~~~

Return to the deployment [README](README.md#setup-a-database-for-the-woe-sim-microservice).
