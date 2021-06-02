
# Setup Google Kubernetes Engine (GKE)

- [Setup Google Kubernetes Engine (GKE)](#setup-google-kubernetes-engine-gke)
  - [Getting started with Google Cloud Platform](#getting-started-with-google-cloud-platform)
  - [Install CLIs](#install-clis)
    - [Install Kubernetes CLI](#install-kubernetes-cli)
    - [Install Google Cloud SDK](#install-google-cloud-sdk)
  - [Create a Google Kubernetes Engine (GKE) cluster](#create-a-google-kubernetes-engine-gke-cluster)
    - [Create a GKE cluster from the CLI](#create-a-gke-cluster-from-the-cli)
      - [Create a standard GKE cluster](#create-a-standard-gke-cluster)
      - [Create an autopilot GKE cluster](#create-an-autopilot-gke-cluster)

## Getting started with Google Cloud Platform

If you are a first time GCP user follow these steps to get started,
follow the steps in the [Before you begin](https://cloud.google.com/kubernetes-engine/docs/quickstart#before-you-begin)
section of the Google Kubernetes Engine Quickstart guide.

## Install CLIs

### Install Kubernetes CLI

Follow the instructions in the [Kubernetes documentation](https://kubernetes.io/docs/tasks/tools/#kubectl) new tab to install `kubectl`.

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

### Install Google Cloud SDK

Follow the steps in the [Google Cloud SDK Quickstart guide](https://cloud.google.com/sdk/docs/quickstart).

Make sure you complete the steps provided in the [Initialize the Cloud SDK](https://cloud.google.com/sdk/docs/quickstart#initializing_the) documentation.

## Create a Google Kubernetes Engine (GKE) cluster

You can create a GKE cluster from the [Google Cloud Console](https://console.cloud.google.com/?_ga=2.42693094.2131316053.1622567360-838226544.1591877114)
or from the CLI.

### Create a GKE cluster from the CLI

See [Create a GKE cluster](https://cloud.google.com/kubernetes-engine/docs/quickstart#create_cluster)
for detailed instructions.

There are two GKE cluster types, standard or autopilot.

#### Create a standard GKE cluster

~~~bash
gcloud container clusters create <my-cluster-name> --num-nodes=1
~~~

~~~bash
gcloud container clusters create woe-yugabyte-std --num-nodes=3 --machine-type=e2-standard-8
~~~

~~~text
WARNING: Starting in January 2021, clusters will use the Regular release channel by default when `--cluster-version`, `--release-channel`, `--no-enable-autoupgrade`, and `--no-enable-autorepair` flags are not specified.
WARNING: Currently VPC-native is not the default mode during cluster creation. In the future, this will become the default mode and can be disabled using `--no-enable-ip-alias` flag. Use `--[no-]enable-ip-alias` flag to suppress this warning.
WARNING: Starting with version 1.18, clusters will have shielded GKE nodes by default.
WARNING: Your Pod address range (`--cluster-ipv4-cidr`) can accommodate at most 1008 node(s).
WARNING: Starting with version 1.19, newly created clusters and node-pools will have COS_CONTAINERD as the default node image when no image type is specified.
Creating cluster woe-yugabyte-std in us-east1-b... Cluster is being health-checked (master is healthy)...done.
Created [https://container.googleapis.com/v1/projects/woe-yugabyte/zones/us-east1-b/clusters/woe-yugabyte-std].
To inspect the contents of your cluster, go to: https://console.cloud.google.com/kubernetes/workload_/gcloud/us-east1-b/woe-yugabyte-std?project=woe-yugabyte
kubeconfig entry generated for woe-yugabyte-std.
NAME              LOCATION    MASTER_VERSION   MASTER_IP    MACHINE_TYPE   NODE_VERSION     NUM_NODES  STATUS
woe-yugabyte-std  us-east1-b  1.19.9-gke.1400  34.73.8.176  e2-standard-8  1.19.9-gke.1400  3          RUNNING
~~~

#### Create an autopilot GKE cluster

For autopilot clusters a region must be specified.

~~~bash
gcloud compute regions list
~~~

~~~text
NAME                     CPUS  DISKS_GB  ADDRESSES  RESERVED_ADDRESSES  STATUS  TURNDOWN_DATE
asia-east1               0/24  0/4096    0/8        0/8                 UP
asia-east2               0/24  0/4096    0/8        0/8                 UP
asia-northeast1          0/24  0/4096    0/8        0/8                 UP
asia-northeast2          0/24  0/4096    0/8        0/8                 UP
asia-northeast3          0/24  0/4096    0/8        0/8                 UP
asia-south1              0/24  0/4096    0/8        0/8                 UP
asia-southeast1          0/24  0/4096    0/8        0/8                 UP
asia-southeast2          0/24  0/4096    0/8        0/8                 UP
australia-southeast1     0/24  0/4096    0/8        0/8                 UP
europe-central2          0/24  0/4096    0/8        0/8                 UP
europe-north1            0/24  0/4096    0/8        0/8                 UP
europe-west1             0/24  0/4096    0/8        0/8                 UP
europe-west2             0/24  0/4096    0/8        0/8                 UP
europe-west3             0/24  0/4096    0/8        0/8                 UP
europe-west4             0/24  0/4096    0/8        0/8                 UP
europe-west6             0/24  0/4096    0/8        0/8                 UP
northamerica-northeast1  0/24  0/4096    0/8        0/8                 UP
southamerica-east1       0/24  0/4096    0/8        0/8                 UP
us-central1              0/24  0/4096    0/8        0/8                 UP
us-east1                 5/24  500/4096  5/8        0/8                 UP
us-east4                 0/24  0/4096    0/8        0/8                 UP
us-west1                 0/24  0/4096    0/8        0/8                 UP
us-west2                 0/24  0/4096    0/8        0/8                 UP
us-west3                 0/24  0/4096    0/8        0/8                 UP
us-west4                 0/24  0/4096    0/8        0/8                 UP
~~~

~~~bash
gcloud container clusters create-auto <my-cluster-name> --region=<region>
~~~

~~~bash
gcloud container clusters create-auto woe-yugabyte-auto --region=us-east1
~~~

~~~text
WARNING: Starting with version 1.18, clusters will have shielded GKE nodes by default.
WARNING: The Pod address range limits the maximum size of the cluster. Please refer to https://cloud.google.com/kubernetes-engine/docs/how-to/flexible-pod-cidr to learn how to optimize IP address allocation.
WARNING: Starting with version 1.19, newly created clusters and node-pools will have COS_CONTAINERD as the default node image when no image type is specified.
Creating cluster woe-yugabyte-auto in us-east1... Cluster is being health-checked (master is healthy)...done.
Created [https://container.googleapis.com/v1/projects/woe-yugabyte/zones/us-east1/clusters/woe-yugabyte-auto].
To inspect the contents of your cluster, go to: https://console.cloud.google.com/kubernetes/workload_/gcloud/us-east1/woe-yugabyte-auto?project=woe-yugabyte
kubeconfig entry generated for woe-yugabyte-auto.
NAME               LOCATION  MASTER_VERSION   MASTER_IP      MACHINE_TYPE  NODE_VERSION     NUM_NODES  STATUS
woe-yugabyte-auto  us-east1  1.19.9-gke.1900  35.185.61.131  e2-medium     1.19.9-gke.1900  3          RUNNING
~~~
