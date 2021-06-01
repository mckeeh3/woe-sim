# Where on Earth Simulator (woe-sim) Microservice

This microservice simulates geographically distributed IoT devices. This service simulates the creation, state, and deletion of IoT devices.

Via the map UI provided by the woe-twin microservice, requests to create and delete IoT devices or requests to change the state of existing devices are processed by this microservice. These processed requests are then sent as simulated device telemetry messages to the woe-twin microservice.

- [Where on Earth Simulator (woe-sim) Microservice](#where-on-earth-simulator-woe-sim-microservice)
  - [Deploy the woe-sim microservice](#deploy-the-woe-sim-microservice)
    - [Setup a Kubernetes cluster](#setup-a-kubernetes-cluster)
    - [Create the Kubernetes namespace](#create-the-kubernetes-namespace)
    - [Setup a database for the woe-sim microservice](#setup-a-database-for-the-woe-sim-microservice)
    - [Setup Docker repository](#setup-docker-repository)
    - [Build and push the Docker image](#build-and-push-the-docker-image)
    - [Deploy the Docker image to the Kubernetes cluster](#deploy-the-docker-image-to-the-kubernetes-cluster)
    - [Check if the pods are running](#check-if-the-pods-are-running)
    - [Create a Load Balancer to enable external access](#create-a-load-balancer-to-enable-external-access)
  - [Design notes](#design-notes)

## Deploy the woe-sim microservice

Follow these instructions for installing and running the woe-sim microservice.

First, clone the weo-sim Github project.

~~~bash
git clone https://github.com/mckeeh3/woe-sim.git
~~~

### Setup a Kubernetes cluster

See [README-kuberbetes](https://github.com/mckeeh3/woe-sim/blob/master/README-kubernetes.md) for instructions on setting up a Kubernetes cluster in various environments.

### Create the Kubernetes namespace

The namespace only needs to be created once.

~~~bash
kubectl create namespace woe-sim
~~~

~~~text
namespace/woe-sim created
~~~

Set this namespace as the default for subsequent `kubectl` commands.

~~~bash
kubectl config set-context --current --namespace=woe-sim
~~~

~~~text
Context "kind-kind" modified.
~~~

### Setup a database for the woe-sim microservice

See [README-database](https://github.com/mckeeh3/woe-sim/blob/master/README-database.md) for instructions on setting up a Cassandra or PostgreSQL database in various environments.

### Setup Docker repository

See [README-docker-container-registry](https://github.com/mckeeh3/woe-sim/blob/master/README-docker-container-registry.md) for instructions on setting up a Docker image registry.

### Build and push the Docker image

From the woe-sim project directory.

Build the project, which will create a new Docker image.

~~~bash
mvn clean package docker:push
~~~

~~~text
...

[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  36.566 s
[INFO] Finished at: 2021-05-22T14:59:56-04:00
[INFO] ------------------------------------------------------------------------
~~~

### Deploy the Docker image to the Kubernetes cluster

Select the deployment file for the database environment that you are using.

For Cassandra deployed to the Kubernetes cluster using helm.

~~~bash
kubectl apply -f kubernetes/woe-sim-helm-cassandra.yml
~~~

For PostgreSQL deployed to the Kubernetes cluster using helm.

~~~bash
kubectl apply -f kubernetes/woe-sim-helm-postgresql.yml
~~~

~~~text
deployment.apps/woe-sim created
role.rbac.authorization.k8s.io/pod-reader created
rolebinding.rbac.authorization.k8s.io/read-pods created
~~~

### Check if the pods are running

This may take a few moments.

~~~bash
kubectl get pods
~~~

~~~text
NAME                      READY   STATUS    RESTARTS   AGE
woe-sim-77dfcc864b-6cvrg   1/1     Running   0          3h10m
woe-sim-77dfcc864b-trmz7   1/1     Running   0          3h10m
woe-sim-77dfcc864b-vf78s   1/1     Running   0          3h10m
~~~

### Create a Load Balancer to enable external access

See [README-load-balancer](https://github.com/mckeeh3/woe-sim/blob/master/README-load-balancer.md) for instructions on setting up a Kubernetes load balancer in various environments.

Next, deploy the [woe-twin microservice](https://github.com/mckeeh3/woe-twin).

## Design notes

TODO
