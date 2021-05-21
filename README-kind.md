
# `kind` (`minikube` alternative) Installation and Setup

Follow these instructions for installing and running the woe-sim microservice using Minikube.

## Prerequisites

Clone the weo-sim Github project.

~~~bash
git clone https://github.com/mckeeh3/woe-sim.git
~~~

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

### Install `kind` and Kubernetes CLI

Follow the [instructions](https://github.com/kubernetes-sigs/kind#installation-and-usage) for installing kind.
[kind](https://kind.sigs.k8s.io/) is a tool for running local Kubernetes clusters using Docker container “nodes”.

### Create the `kind' local Kubernetes cluster

You only need to do this step once. See the `kind` documentation for details.

~~~bash
kind create cluster
~~~

~~~text
Creating cluster "kind" ...
 ✓ Ensuring node image (kindest/node:v1.20.2) 🖼
 ✓ Preparing nodes 📦
 ✓ Writing configuration 📜
 ✓ Starting control-plane 🕹️
 ✓ Installing CNI 🔌
 ✓ Installing StorageClass 💾
Set kubectl context to "kind-kind"
You can now use your cluster with:

kubectl cluster-info --context kind-kind

Not sure what to do next? 😅  Check out https://kind.sigs.k8s.io/docs/user/quick-start/
~~~

Once the `kind` Kubernetes cluster is up you can check its status using the following commands.

~~~bash
kubectl get events
~~~

~~~text
LAST SEEN   TYPE     REASON                    OBJECT                    MESSAGE
52s         Normal   Starting                  node/kind-control-plane   Starting kubelet.
52s         Normal   NodeHasSufficientMemory   node/kind-control-plane   Node kind-control-plane status is now: NodeHasSufficientMemory
52s         Normal   NodeHasNoDiskPressure     node/kind-control-plane   Node kind-control-plane status is now: NodeHasNoDiskPressure
52s         Normal   NodeHasSufficientPID      node/kind-control-plane   Node kind-control-plane status is now: NodeHasSufficientPID
52s         Normal   NodeAllocatableEnforced   node/kind-control-plane   Updated Node Allocatable limit across pods
32s         Normal   Starting                  node/kind-control-plane   Starting kubelet.
32s         Normal   NodeAllocatableEnforced   node/kind-control-plane   Updated Node Allocatable limit across pods
32s         Normal   NodeHasSufficientMemory   node/kind-control-plane   Node kind-control-plane status is now: NodeHasSufficientMemory
32s         Normal   NodeHasNoDiskPressure     node/kind-control-plane   Node kind-control-plane status is now: NodeHasNoDiskPressure
32s         Normal   NodeHasSufficientPID      node/kind-control-plane   Node kind-control-plane status is now: NodeHasSufficientPID
29s         Normal   RegisteredNode            node/kind-control-plane   Node kind-control-plane event: Registered Node kind-control-plane in Controller
26s         Normal   Starting                  node/kind-control-plane   Starting kube-proxy.
22s         Normal   NodeReady                 node/kind-control-plane   Node kind-control-plane status is now: NodeReady

~~~

~~~bash
kubectl get all -A
~~~

~~~text
NAMESPACE            NAME                                             READY   STATUS    RESTARTS   AGE
kube-system          pod/coredns-74ff55c5b-dq24t                      1/1     Running   0          2m41s
kube-system          pod/coredns-74ff55c5b-ls8s5                      1/1     Running   0          2m41s
kube-system          pod/etcd-kind-control-plane                      1/1     Running   0          2m50s
kube-system          pod/kindnet-l48gm                                1/1     Running   0          2m42s
kube-system          pod/kube-apiserver-kind-control-plane            1/1     Running   0          2m50s
kube-system          pod/kube-controller-manager-kind-control-plane   1/1     Running   0          2m50s
kube-system          pod/kube-proxy-md5j8                             1/1     Running   0          2m42s
kube-system          pod/kube-scheduler-kind-control-plane            1/1     Running   0          2m50s
local-path-storage   pod/local-path-provisioner-78776bfc44-lzxlg      1/1     Running   0          2m41s

NAMESPACE     NAME                 TYPE        CLUSTER-IP   EXTERNAL-IP   PORT(S)                  AGE
default       service/kubernetes   ClusterIP   10.96.0.1    <none>        443/TCP                  2m58s
kube-system   service/kube-dns     ClusterIP   10.96.0.10   <none>        53/UDP,53/TCP,9153/TCP   2m57s

NAMESPACE     NAME                        DESIRED   CURRENT   READY   UP-TO-DATE   AVAILABLE   NODE SELECTOR            AGE
kube-system   daemonset.apps/kindnet      1         1         1       1            1           <none>                   2m56s
kube-system   daemonset.apps/kube-proxy   1         1         1       1            1           kubernetes.io/os=linux   2m57s

NAMESPACE            NAME                                     READY   UP-TO-DATE   AVAILABLE   AGE
kube-system          deployment.apps/coredns                  2/2     2            2           2m57s
local-path-storage   deployment.apps/local-path-provisioner   1/1     1            1           2m55s

NAMESPACE            NAME                                                DESIRED   CURRENT   READY   AGE
kube-system          replicaset.apps/coredns-74ff55c5b                   2         2         2       2m42s
local-path-storage   replicaset.apps/local-path-provisioner-78776bfc44   1         1         1       2m42s
~~~

### Deploy either Cassandra or PostgreSQL database

See the instructions for deploying to Kubernetes either
[Cassandra](https://github.com/mckeeh3/woe-sim/blob/master/README-cassandra-kubernetes.md) or
[PostgreSQL](https://github.com/mckeeh3/woe-sim/blob/master/README-postgresql-kubernetes.md).
