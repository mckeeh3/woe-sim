
# Setup Cassandra Using Amazon Keyspaces

[Amazon Keyspaces](https://aws.amazon.com/keyspaces/) is a serverless AWS service for Cassandra. The service provides a web interface for creating keypsaces and tables.

Use the online CQL Editor to create each of the siz required Cassandra tables. To access the CQL Editor, from the
[Amazon Keyspaces](https://aws.amazon.com/keyspaces/)
page, click **Get started with Amazon Keyspaces**.

## Create Amazon Keyspaces Cassandra tables

Go to the [Akazon Keyspaces](https://console.aws.amazon.com/keyspaces/home?region=us-east-1#keyspaces) and click `Create keyspace` at top right.

Keyspace name: `woe_simulator` then click `Create keyspace`.

Use the [CQL editor](https://console.aws.amazon.com/keyspaces/home?region=us-east-1#cql-editor)
or follow the steps for installing the `cqlsh` at
[Using cqlsh to connect to Amazon Keyspaces (for Apache Cassandra)](https://docs.aws.amazon.com/keyspaces/latest/devguide/programmatic.cqlsh.html).

The DDL file used to create the tables is located in the woe-twin project at `src/main/resources/akka-persistence-journal-create-sim.cql`.

## Adjust Configuration Settings for Amazon Keyspaces

In the woe-sim project, edit the file `akka-cluster-eks.yml`. In the `env` section, look for the environment variable name `cassandra_host_port`.
Adjust the value of the Keyspaces host and port as needed.

Keyspaces requires that the `AWS_ACCESS_KEY` and `AWS_SECRET_ACCESS_KEY` environment variables are set in the container. This requires that the service
namespace is created and these two key/values are defined using [Kubernetes secrets](https://kubernetes.io/docs/concepts/configuration/secret/).

Create the Kubernetes namespace. The namespace only needs to be created once.

~~~bash
$ kubectl create namespace woe-sim
namespace/woe-sim created
~~~

Set this namespace as the default for subsequent `kubectl` commands.

~~~bash
$ kubectl config set-context --current --namespace=woe-sim
Context "minikube" modified.
~~~

Once the namespace is defined and set as the current context, create the secrets.

~~~bash
$ kubectl create secret generic aws-access-key --from-literal=AWS_ACCESS_KEY='ZXCV.....' --from-literal=AWS_SECRET_ACCESS_KEY='VCXZ....'
secret/aws-access-key created
~~~
