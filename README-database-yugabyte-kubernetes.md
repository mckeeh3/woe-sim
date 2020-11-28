
# Setup Yugabyte in your Kubernetes Cluster

These instructions are for setting up Yugabyte in your Kubernetes cluster. This applies to Minikube, EKS, GKE, and other Kubernetes environments.

Yugabyte provides APIs for both Cassandra and PostgreSQL.

## Deploy Yugabyte to Kubernetes

Follow the documentation for installing
[Yugabyte](https://docs.yugabyte.com/latest/deploy/).

Recommended default deployment changes.

* Deploy with [Helm](https://docs.yugabyte.com/latest/deploy/kubernetes/single-zone/gke/helm-chart/)
* Use namespace `yugabyte-db`. `kubectl create namespace yugabyte-db`
* Specify Kubernetes pod replicas, CPU request and limit when doing the `helm install` step.

~~~bash
$ helm install yugabyte-db yugabytedb/yugabyte --namespace yugabyte-db --wait \
--set replicas.tserver=4,\
resource.tserver.requests.cpu=4,\
resource.tserver.limits.cpu=8
~~~

As shown in the Yugabyte documentation, verify the status of the deployment using the following command.

~~~bash
$ helm status yugabyte-db -n yugabyte-db

NAME: yugabyte-db
LAST DEPLOYED: Mon Jul 27 14:36:03 2020
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

## Verify Access to CQL and SQL CLI

Try the following commands to verify access to Cassandra CQL and PostgreSQL
CQL CLI tools once Yugabyte has been installed in a Kubernetes environment.

Cassandra CQL shell

~~~bash
$ kubectl --namespace yugabyte-db exec -it yb-tserver-0 -- /home/yugabyte/bin/ycqlsh yb-tserver-0

Defaulting container name to yb-tserver.
Use 'kubectl describe pod/yb-tserver-0 -n yugabyte-db' to see all of the containers in this pod.
Connected to local cluster at yb-tserver-0:9042.
[ycqlsh 5.0.1 | Cassandra 3.9-SNAPSHOT | CQL spec 3.4.2 | Native protocol v4]
Use HELP for help.
ycqlsh> quit
~~~

## Copy CQL DDL commands to the Yugabyte server

From the woe-sim project directory.

~~~bash
$ kubectl cp src/main/resources/akka-persistence-journal-create-sim.cql yugabyte-db/yb-tserver-0:/tmp
Defaulting container name to yb-tserver.
~~~

## Create the CQL Tables

~~~bash
$ kubectl --namespace yugabyte-db exec -it yb-tserver-0 -- /home/yugabyte/bin/ycqlsh yb-tserver-0

Defaulting container name to yb-tserver.
Use 'kubectl describe pod/yb-tserver-0 -n yugabyte-db' to see all of the containers in this pod.
Connected to local cluster at yb-tserver-0:9042.
[ycqlsh 5.0.1 | Cassandra 3.9-SNAPSHOT | CQL spec 3.4.2 | Native protocol v4]
Use HELP for help.
~~~

~~~bash
ycqlsh> source '/tmp/akka-persistence-journal-create-sim.cql'
ycqlsh> describe keyspaces;

woe_simulator  system_schema  system_auth  system

ycqlsh> use woe_simulator;
ycqlsh:woe_simulator> describe tables;

tag_views  tag_scanning         tag_write_progress
messages   all_persistence_ids  metadata

ycqlsh:woe_simulator>quit
~~~
