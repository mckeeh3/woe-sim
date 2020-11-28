
# Deploy Yugabyte Locally

You have two options for how you deploy Yugabyte. You can either do a local deployment or deploy Yugabyete to a Kubernetes environment. This section has instructions for a local deployment. The next section has instructions for deploying Yugabyte to Kubernetes.

Follow the [Yugabyte Quick Start](https://docs.yugabyte.com/latest/quick-start/) guide for instrucation on installing on your local system.

## Create Cassandra Tables

Cd into the directory where you cloned the `woe-sim` repo.

~~~bash
$ <path-to-yugabyte>/bin/ycqlsh

Connected to local cluster at localhost:9042.
[ycqlsh 5.0.1 | Cassandra 3.9-SNAPSHOT | CQL spec 3.4.2 | Native protocol v4]
Use HELP for help.
ycqlsh>
~~~

Run script to create the required Akka persistence tables.

~~~bash
ycqlsh> source 'src/main/resources/akka-persistence-journal-create-sim.cql'
~~~

Verify that the tables have been created.

~~~bash
ycqlsh> use woe_simulator;
ycqlsh:woe_simulator> describe tables;

tag_views  tag_scanning         tag_write_progress
messages   all_persistence_ids  metadata

ycqlsh:woe_simulator> quit
~~~
