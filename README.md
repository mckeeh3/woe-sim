# Where on Earth Simulator (woe-sim) Microservice

This microservice simulates geographically distributed IoT devices. This service simulates the creation, state, and deletion of IoT devices.

Via the map UI provided by the woe-twin microservice, requests to create and delete IoT devices or requests to change the state of existing devices are processed by this microservice. These processed requests are then sent as simulated device telemetry messages to the woe-twin microservice.

## Installation

How you install this Akka microservice depends on your target environment. There are environment specific README documents for each of the tested Kubernetes environments. With each deployment you also have to select which database you want to use. There are also README documents for the tested databases.

### Kubernetes Environments

* [Minikube](https://github.com/mckeeh3/woe-sim/blob/master/README-minikube.md)
* [Amazon EKS](https://github.com/mckeeh3/woe-sim/blob/master/README-amazon-eks.md)
* [Google GKE](https://github.com/mckeeh3/woe-sim/blob/master/README-google-gke.md)

### Database Environments

* [Cassandra local](https://github.com/mckeeh3/woe-sim/blob/master/README-database-cassandra-local.md)
* [Cassandra Amazon](https://github.com/mckeeh3/woe-sim/blob/master/README-database-cassandra-amazon.md)
* [Yugabyte local](https://github.com/mckeeh3/woe-sim/blob/master/README-database-yugabyte-kubernetes.md)
* [Yugabyte Kubernetes](https://github.com/mckeeh3/woe-sim/blob/master/README-database-yugabyte-kubernetes.md)

## Design notes

TODO
