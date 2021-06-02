
# Deploy woe-sim to AWS Elastic Kubernetes Service - EKS

Go to [Getting started with eksctl](https://docs.aws.amazon.com/eks/latest/userguide/getting-started-eksctl.html)
for directions on setting up EKS and Kubernetes CLI tools.

Recommend that you create an EKS cluster with two or more Kubernetes nodes.

From the [Akka Platform Guide](https://developer.lightbend.com/docs/akka-platform-guide/deployment/aws-install.html).
You can create a Kubernetes cluster with the eksctl command line tool. For example:

~~~bash
eksctl create cluster \
  --name eks-akka-demo \
  --version 1.18 \
  --region <your-aws-region> \
  --nodegroup-name linux-nodes \
  --nodes 3 \
  --nodes-min 1 \
  --nodes-max 4 \
  --with-oidc \
  --managed
~~~

An alternative is to create it from the [Amazon EKS console](https://console.aws.amazon.com/eks/home).

## Some Additional CLI Setup

The `kubectl` CLI provides a nice Kubectl Autocomplete feature for `bash` and `zsh`.
See the [kubectl Cheat Sheet](https://kubernetes.io/docs/reference/kubectl/cheatsheet/#kubectl-autocomplete) for instructions.

Also, consider installing [`kubectx`](https://github.com/ahmetb/kubectx), which also includes `kubens`.
Mac:

~~~bash
brew install kubectx
~~~

Arch Linux:

~~~bash
yay kubectx
~~~

You may also want to create an alias for the `kubectl` command, such as `kc`.

~~~bash
alias kc=kubectl
~~~

## Deploy the Kubernetes Dashboard (Optional)

You may want to deploy the Kubernetes dashboard. This is an optional step. To deploy the dashboard follow the
[Tutorial: Deploy the Kubernetes Dashboard (web UI)](https://docs.aws.amazon.com/eks/latest/userguide/dashboard-tutorial.html).

## Setup Database

The WoE Simulator service uses an event log with [Akka Persistence](https://doc.akka.io/docs/akka/current/typed/persistence.html).

Follow the installation instructions in the
[README-database-cassandra-amazon.md](README-database-cassandra-amazon.md)
or, for a Yugabyte installation in the
[README-database-yugabyte-kubernetes.md](README-database-yugabyte-kubernetes.md).

TODO

## Clone the GitHub Repo

Git clone the project repoisitory.

~~~bash
git clone https://github.com/mckeeh3/woe-sim
~~~

## Adjust the configuration based on the selected database

Modify the `application.conf` file to include the required database configuration settings.
At the end of the `application.conf` file select the appropriate database specific configuration file.

~~~text
# Uncomment as needed for specific Kubernetes environments
#include "application-minikube-ws-cdb"
#include "application-minikube-ws-rdb"
include "application-eks-ws-cdb"
#include "application-eks-ws-rdb"
#include "application-gke-ws-cdb"
#include "application-gke-ws-rdb"
~~~

Verify that the selected include file has the necessary settings for your selected database.
If necessary, create a new include file if the one you need is not included with the project.

## Deploy the woe-sim microservice

From the woe-sim project directory.

Build the project, which will create a new Docker image.

~~~bash
$ mvn clean package

[INFO] Scanning for projects...
[INFO]
[INFO] ------------------------< org.example:woe-sim >-------------------------
[INFO] Building woe-sim 1.0-SNAPSHOT
[INFO] --------------------------------[ jar ]---------------------------------

...

[INFO] --- docker-maven-plugin:0.26.1:build (build-docker-image) @ woe-sim ---
[INFO] artifact io.grpc:grpc-api: checking for updates from central
[INFO] artifact io.grpc:grpc-api: checking for updates from sonatypestaging
[INFO] artifact io.grpc:grpc-api: checking for updates from bintrayakkasnapshots
[INFO] artifact io.grpc:grpc-api: checking for updates from bintrayakkamaven
[INFO] artifact io.grpc:grpc-core: checking for updates from central
[INFO] artifact io.grpc:grpc-core: checking for updates from sonatypestaging
[INFO] artifact io.grpc:grpc-core: checking for updates from bintrayakkasnapshots
[INFO] artifact io.grpc:grpc-core: checking for updates from bintrayakkamaven
[INFO] Copying files to /home/hxmc/Lightbend/akka-java/woe-sim/target/docker/woe-sim/build/maven
[INFO] Building tar: /home/hxmc/Lightbend/akka-java/woe-sim/target/docker/woe-sim/tmp/docker-build.tar
[INFO] DOCKER> [woe-sim:latest]: Created docker-build.tar in 4 seconds
[INFO] DOCKER> [woe-sim:latest]: Built image sha256:010cc
[INFO] DOCKER> [woe-sim:latest]: Removed old image sha256:6458e
[INFO] DOCKER> [woe-sim:latest]: Tag with latest,20210219-102458.4319740
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  35.008 s
[INFO] Finished at: 2021-03-15T11:21:28-04:00
[INFO] ------------------------------------------------------------------------
~~~

### Create the Kubernetes namespace

The namespace only needs to be created once.

~~~bash
$ kubectl create namespace woe-sim
namespace/woe-sim created
~~~

### Set this namespace as the default for subsequent `kubectl` commands

~~~bash
$ kubectl config set-context --current --namespace=woe-sim
Context "minikube" modified.
~~~
