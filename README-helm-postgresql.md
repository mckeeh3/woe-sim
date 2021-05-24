
# Deploy PostgreSQL to Kubernetes cluster

Follow these instructions to deploy PostgreSQL to a Kubernetes cluster.

## Prerequisites

Clone the weo-sim Github project.

~~~bash
git clone https://github.com/mckeeh3/woe-sim.git
~~~

[Install Helm](https://helm.sh/docs/intro/install/).

## Install PostgreSQL

### Install PostgreSQL using Helm

You can find PostgreSQL Helm charts at [Artifact Hub](https://artifacthub.io/).

Shown here is the installation of the [Bitnami PostgreSQL chart](https://artifacthub.io/packages/helm/bitnami/postgresql).

~~~bash
helm repo add bitnami https://charts.bitnami.com/bitnami
~~~

~~~text
"bitnami" has been added to your repositories
~~~

### Create a `postgresql` namespace

~~~bash
kubectl create namespace postgresql
~~~

### Install PostgreSQL into to `postgresql` namespace

~~~bash
helm install -n postgresql postgresql bitnami/postgresql
~~~

~~~text
NAME: postgresql
LAST DEPLOYED: Fri May 21 20:00:12 2021
NAMESPACE: postgresql
STATUS: deployed
REVISION: 1
TEST SUITE: None
NOTES:
** Please be patient while the chart is being deployed **

PostgreSQL can be accessed via port 5432 on the following DNS name from within your cluster:

    postgresql.postgresql.svc.cluster.local - Read/Write connection

To get the password for "postgres" run:

    export POSTGRES_PASSWORD=$(kubectl get secret --namespace postgresql postgresql -o jsonpath="{.data.postgresql-password}" | base64 --decode)

To connect to your database run the following command:

    kubectl run postgresql-client --rm --tty -i --restart='Never' --namespace postgresql --image docker.io/bitnami/postgresql:11.12.0-debian-10-r5 --env="PGPASSWORD=$POSTGRES_PASSWORD" --command -- psql --host postgresql -U postgres -d postgres -p 5432



To connect to your database from outside the cluster execute the following commands:

    kubectl port-forward --namespace postgresql svc/postgresql 5432:5432 &
    PGPASSWORD="$POSTGRES_PASSWORD" psql --host 127.0.0.1 -U postgres -d postgres -p 5432
~~~

Use the following command to show the above output again for reference.

~~~bash
helm test -n postgresql postgresql
~~~

### Create PostgreSQL environment variable

Create an environment variable that contains the PostgreSQL password.

~~~bash
export POSTGRES_PASSWORD=$(kubectl get secret --namespace postgresql postgresql -o jsonpath="{.data.postgresql-password}" | base64 --decode)
~~~

### Create PostgreSQL secret

Create a secret that contains the PostgreSQL username, password, and JDBC URL.

~~~bash
kubectl create secret -n woe-sim generic postgresql-env \
--from-literal=postgresql_username=postgres \
--from-literal=postgresql_password=$POSTGRES_PASSWORD \
--from-literal=postgresql_url=jdbc:postgresql://postgresql.postgresql.svc.cluster.local:5432/postgres
~~~

## Create the Akka Persistence PostgreSQL tables

Run a PostgreSQL pod that can be used to create the Akka Persistence tables.

~~~bash
kubectl run postgresql-client --rm --tty -i --restart='Never' --namespace postgresql --image docker.io/bitnami/postgresql:11.12.0-debian-10-r5 --env="PGPASSWORD=$POSTGRES_PASSWORD" --command -- psql --host postgresql -U postgres -d postgres -p 5432
~~~

~~~text
If you don't see a command prompt, try pressing enter.

postgres=#
~~~

From another terminal, copy the SQL DDL file from the woe-sim project directory to the PostgreSQL client pod.

~~~bash
kubectl get pods -n postgresql
~~~

~~~text
NAME                      READY   STATUS    RESTARTS   AGE
postgresql-client         1/1     Running   0          2m43s
postgresql-postgresql-0   1/1     Running   0          11m
~~~

~~~bash
kubectl cp src/main/resources/akka-persistence-journal-create-sim.sql postgresql/postgresql-client:/tmp
~~~

From the terminal running the `psql` run the following command.

~~~bash
postgres=# \i /tmp/akka-persistence-journal-create-sim.sql
~~~

~~~text
CREATE TABLE
CREATE INDEX
CREATE TABLE
postgres=#
~~~

Verify that the tables have been created.

~~~bash
postgres=# \d
                         List of relations
 Schema |                Name                |   Type   |  Owner
--------+------------------------------------+----------+----------
 public | woe_sim_event_journal              | table    | postgres
 public | woe_sim_event_journal_ordering_seq | sequence | postgres
 public | woe_sim_event_tag                  | table    | postgres
(3 rows)

postgres=#
~~~

Finally, quit the `psql` and exit the shell. This will terminate the `postgresql-client` pod.

~~~text
postgres=# \q
pod "postgresql-client" deleted
~~~
