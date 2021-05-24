
# `kind` (`minikube` alternative) Installation and Setup

Follow these instructions for installing and running the woe-sim microservice using Minikube.

## Prerequisites

Clone the weo-sim Github project.

~~~bash
git clone https://github.com/mckeeh3/woe-sim.git
~~~

## Install Kubernetes CLI

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

## Install `kind` and Kubernetes CLI

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

## Deploy either Cassandra or PostgreSQL database

See the instructions for deploying to Kubernetes either
[Cassandra](https://github.com/mckeeh3/woe-sim/blob/master/README-helm-cassandra.md) or
[PostgreSQL](https://github.com/mckeeh3/woe-sim/blob/master/README-helm-postgresql.md).

### Adjust application.conf

Edit the `application.conf` file as follows. Add the database configuration for the specific Akka Persistence event journal database.

For Cassandra, add the following line.

~~~text
include "application-helm-cassandra"
~~~

For PostgreSQL, add the following line.

~~~text
include "application-helm-postgresql"
~~~

### Adjust the pom fabric8 plugin for the specific Docker repository

When using Docker hub, add your Docker user to the image name in the pom.

~~~text
      <plugin>
        <!-- For latest version see - https://dmp.fabric8.io/ -->
        <groupId>io.fabric8</groupId>
        <artifactId>docker-maven-plugin</artifactId>
        <version>0.36.0</version>
        <configuration>
          <images>
            <image>
              <!-- Modify as needed for the target repo. For Docker hub use "your-docker-user"/%a -->
              <name>mckeeh3/%a</name>
~~~

### Build the Docker image

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

### Deploy the Docker images to the Kubernetes cluster

Select the deployment file for the database environment that you are using.

For Cassandra, use file `kubernetes/woe-sim-helm-cassandra.yml`. For PostgreSQL, use file `kubernetes/woe-sim-helm-postgresql.yml`.

~~~bash
kubectl apply -f kubernetes/woe-sim-helm-postgresql.yml
~~~

~~~text
deployment.apps/woe-sim created
role.rbac.authorization.k8s.io/pod-reader created
rolebinding.rbac.authorization.k8s.io/read-pods created
~~~

### Create a Load Balancer to enable external access

Create a load balancer to enable access to the WOE Sim microservice HTTP endpoint.

~~~bash
kubectl expose deployment woe-sim --type=LoadBalancer --name=woe-sim-service
~~~

~~~text
service/woe-sim-service exposed
~~~

Next, view to external port assignments.

~~~bash
kubectl get services woe-sim-service
~~~

~~~text
NAME              TYPE           CLUSTER-IP      EXTERNAL-IP   PORT(S)                                        AGE
woe-sim-service   LoadBalancer   10.107.51.103   <pending>     2552:32361/TCP,8558:31809/TCP,8080:30968/TCP   108s
~~~

Note that the `EXTERNAL-IP` is in a `<pending>` state.

The [Kind Load Balancer](https://kind.sigs.k8s.io/docs/user/loadbalancer/) documentation describes how to
get service of type LoadBalancer working in a kind cluster using Metallb.

The following are the steps provided in the Kind LoadBalancer documentation.

#### Installing metallb using default manifests

Create the metallb namespace

~~~bash
kubectl apply -f https://raw.githubusercontent.com/metallb/metallb/master/manifests/namespace.yaml
~~~

#### Create the memberlist secrets

~~~bash
kubectl create secret generic -n metallb-system memberlist --from-literal=secretkey="$(openssl rand -base64 128)" 
~~~

#### Apply metallb manifest

~~~bash
kubectl apply -f https://raw.githubusercontent.com/metallb/metallb/master/manifests/metallb.yaml
~~~

Wait for metallb pods to have a status of Running

~~~bash
kubectl get pods -n metallb-system --watch
~~~

#### Setup address pool used by loadbalancers

To complete layer2 configuration, we need to provide metallb a range of IP addresses it controls. We want this range to be on the docker kind network.

~~~bash
docker network inspect -f '{{.IPAM.Config}}' kind
~~~

Here is an example of the output from the above command.

~~~text
[{172.18.0.0/16  172.18.0.1 map[]} {fc00:f853:ccd:e793::/64  fc00:f853:ccd:e793::1 map[]}]
~~~

Create a file, such as `metallb-configmap.yaml`, that contains the following contents.

~~~text
apiVersion: v1
kind: ConfigMap
metadata:
  namespace: metallb-system
  name: config
data:
  config: |
    address-pools:
    - name: default
      protocol: layer2
      addresses:
      - 172.19.255.200-172.19.255.250
~~~

Edit the last line in the file to match the IP range that fits within the Docker network.
For example, based on the above `docker network inspect` command this IP range works.

~~~text
      - 172.18.255.200-172.18.255.250
~~~

The complete modified file looks like this.

~~~text
apiVersion: v1
kind: ConfigMap
metadata:
  namespace: metallb-system
  name: config
data:
  config: |
    address-pools:
    - name: default
      protocol: layer2
      addresses:
      - 172.18.255.200-172.18.255.250
~~~

Apply this modified file.

~~~bash
kc apply -f /tmp/metallb.yaml
~~~

Next, verify that the `woe-sim-service` LoadBalancer has an assigned external IP.

~~~bash
kubectl get services woe-sim-service
~~~

~~~text
NAME              TYPE           CLUSTER-IP      EXTERNAL-IP      PORT(S)                                        AGE
woe-sim-service   LoadBalancer   10.96.130.159   172.18.255.200   8080:30924/TCP,8558:30721/TCP,2552:31361/TCP   15h
~~~

Verify that the load balancer is working. Run a `curl` command using the load balancer IP and port 8558, that Akka Management port.

~~~bash
curl -v http://172.18.255.200:8558/cluster/members | python -m json.tool
~~~

~~~text
*   Trying 172.18.255.200:8558...
  % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
  0     0    0     0    0     0      0      0 --:--:-- --:--:-- --:--:--     0* Connected to 172.18.255.200 (172.18.255.200) port 8558 (#0)
> GET /cluster/members HTTP/1.1
> Host: 172.18.255.200:8558
> User-Agent: curl/7.76.1
> Accept: */*
>
* Mark bundle as not supporting multiuse
< HTTP/1.1 200 OK
< Server: akka-http/10.2.3
< Date: Mon, 24 May 2021 16:38:23 GMT
< Content-Type: application/json
< Content-Length: 568
<
{ [568 bytes data]
100   568  100   568    0     0   277k      0 --:--:-- --:--:-- --:--:--  277k
* Connection #0 to host 172.18.255.200 left intact
{
    "leader": "akka://woe-sim@10.244.0.26:25520",
    "members": [
        {
            "node": "akka://woe-sim@10.244.0.26:25520",
            "nodeUid": "925406639243941266",
            "roles": [
                "dc-default"
            ],
            "status": "Up"
        },
        {
            "node": "akka://woe-sim@10.244.0.27:25520",
            "nodeUid": "1608401123146447922",
            "roles": [
                "dc-default"
            ],
            "status": "Up"
        },
        {
            "node": "akka://woe-sim@10.244.0.28:25520",
            "nodeUid": "4831089820306204355",
            "roles": [
                "dc-default"
            ],
            "status": "Up"
        }
    ],
    "oldest": "akka://woe-sim@10.244.0.26:25520",
    "oldestPerRole": {
        "dc-default": "akka://woe-sim@10.244.0.26:25520"
    },
    "selfNode": "akka://woe-sim@10.244.0.28:25520",
    "unreachable": []
}
~~~

Next, deploy the [woe-twin microservice](https://github.com/mckeeh3/woe-twin).
