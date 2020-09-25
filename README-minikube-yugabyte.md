
# Minikube Installation and Setup with Yugabyte

Follow these instructions for installing and running the woe-sim microservice using Minikube and Yugabyte.

### Prerequisites

Clone the weo-sim Github project.

~~~bash
$ git clone https://github.com/mckeeh3/woe-sim.git
~~~

### Install Minikube and Kubernetes CLI

Follow the [instructions](https://kubernetes.io/docs/tasks/tools/install-minikube/) for installing Minikube and the Kubernetes CLI `kubectl`.

The `kubectl` CLI provides a nice Kubectl Autocomplete feature for `bash` and `zsh`.
See the [kubectl Cheat Sheet](https://kubernetes.io/docs/reference/kubectl/cheatsheet/#kubectl-autocomplete) for instructions.

Also, consider installing [kubectx](https://github.com/ahmetb/kubectx), which also includes `kubens`.
Mac:
~~~bash
$ brew install kubectx
~~~
Arch Linux:
~~~bash
$ yay kubectx
~~~

### Install Yugabyte for use with MiniKube

Follow the [Yugabyte Quick Start](https://docs.yugabyte.com/latest/quick-start/) guide for instrucation on installing on your local system.

### Create Cassandra Tables - Yugabyte

Cd into the directory where you cloned the `woe-sim` repo.

~~~bash
$ <path-to-yugabyte>/bin/ycqlsh
~~~

~~~
Connected to local cluster at localhost:9042.
[ycqlsh 5.0.1 | Cassandra 3.9-SNAPSHOT | CQL spec 3.4.2 | Native protocol v4]
Use HELP for help.
ycqlsh> 
~~~

Run script to create the required Akka persistence tables.

~~~
ycqlsh> source 'src/main/resources/akka-persistence-journal-create-sim.cql'
~~~

Verify that the tables have been created.

~~~
ycqlsh> use woe_simulator;
ycqlsh:woe_simulator> describe tables;

tag_views  tag_scanning         tag_write_progress
messages   all_persistence_ids  metadata          

ycqlsh:woe_simulator> quit
~~~

### Start Minikube

You may want to allocate more CPU and memory capacity to run the WoW application than the defaults. There are two `minikube` command options available for adjusting the CPU and memory allocation settings.

~~~bash
$ minikube start --cpus=C --memory=M
~~~

For example, allocate 4 CPUs and 10 gig of memory.

~~~bash
$ minikube start --cpus=4 --memory=10g
~~~

### Build and Deploy to MiniKube

From the woe-sim project directory.

Before the build, set up the Docker environment variables using the following commands.
~~~bash
$ minikube docker-env
~~~
~~~
export DOCKER_TLS_VERIFY="1"
export DOCKER_HOST="tcp://192.168.99.102:2376"
export DOCKER_CERT_PATH="/home/hxmc/.minikube/certs"
export MINIKUBE_ACTIVE_DOCKERD="minikube"

# To point your shell to minikube's docker-daemon, run:
# eval $(minikube -p minikube docker-env)
~~~
Copy and paster the above `eval` command.
~~~bash
$ eval $(minikube -p minikube docker-env)
~~~

Build the project, which will create a new Docker image.
~~~bash
$ mvn clean package docker:build
~~~
~~~
...

[INFO]
[INFO] --- docker-maven-plugin:0.26.1:build (default-cli) @ woe-sim ---
[INFO] Copying files to /home/hxmc/Lightbend/akka-java/woe-sim/target/docker/woe-sim/build/maven
[INFO] Building tar: /home/hxmc/Lightbend/akka-java/woe-sim/target/docker/woe-sim/tmp/docker-build.tar
[INFO] DOCKER> [woe-sim:latest]: Created docker-build.tar in 405 milliseconds
[INFO] DOCKER> [woe-sim:latest]: Built image sha256:ebe14
[INFO] DOCKER> [woe-sim:latest]: Tag with latest,20200617-143425.6247cf9
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  01:30 min
[INFO] Finished at: 2020-06-19T09:25:15-04:00
[INFO] ------------------------------------------------------------------------
~~~

Create the Kubernetes namespace. The namespace only needs to be created once.
~~~bash
$ kubectl create namespace woe-sim-1
~~~
~~~
namespace/woe-sim-1 created
~~~

Set this namespace as the default for subsequent `kubectl` commands.
~~~bash
$ kubectl config set-context --current --namespace=woe-sim-1
~~~
~~~
Context "minikube" modified.
~~~

Deploy the Docker images to the Kubernetes cluster.
~~~bash
$ kubectl apply -f kubernetes/akka-cluster.yml
~~~
~~~
deployment.apps/woe-sim created
role.rbac.authorization.k8s.io/pod-reader created
rolebinding.rbac.authorization.k8s.io/read-pods created
~~~
Check if the pods are running. This may take a few moments.
~~~bash
$ kubectl get pods                                          
~~~
~~~
NAME                      READY   STATUS    RESTARTS   AGE
woe-sim-bd5bf8ddc-mbjmv   1/1     Running   0          8m28s
woe-sim-bd5bf8ddc-tjtpk   1/1     Running   0          8m28s
woe-sim-bd5bf8ddc-z8gh5   1/1     Running   0          8m28s
~~~

If there are configuration issues, start a `bash` shell in one of the pods using the following command.

~~~bash
kc exec -it woe-sim-bd5bf8ddc-mbjmv -- /bin/bash
~~~
~~~
root@woe-sim-65db649dc9-4qj8l:/# env | grep woe
HOSTNAME=woe-sim-65db649dc9-4qj8l
woe_twin_http_server_port=8080
NAMESPACE=woe-sim-1
woe_simulator_http_server_port=8080
woe_simulator_http_server_host=woe-sim-service.woe-sim-1.svc.cluster.local
woe_twin_http_server_host=woe-twin-service.woe-twin-1.svc.cluster.local
root@woe-sim-65db649dc9-4qj8l:/# exit
exit
~~~

### Enable External Access

Create a load balancer to enable access to the WOE Sim microservice HTTP endpoint.

~~~bash
$ kubectl expose deployment woe-sim --type=LoadBalancer --name=woe-sim-service
~~~
~~~
service/woe-sim-service exposed
~~~

Next, view to external port assignments.

~~~bash
$ kubectl get services woe-sim-service
~~~
~~~
NAME              TYPE           CLUSTER-IP      EXTERNAL-IP   PORT(S)                                        AGE
woe-sim-service   LoadBalancer   10.107.51.103   <pending>     2552:32361/TCP,8558:31809/TCP,8080:30968/TCP   108s
~~~

Note that in this example, the Kubernetes internal port 8558 external port assignment of 31809.

For MiniKube deployments, the full URL to access the HTTP endpoint is constructed using the MiniKube IP and the external port.

~~~bash
$ minikube ip       
~~~
In this example the MiniKube IP is:
~~~
192.168.99.102
~~~
Try accessing this endpoint using the curl command or from a browser.
~~~bash
$ curl -v http://$(minikube ip):31809/cluster/members | python -m json.tool
~~~
~~~
curl -v http://$(minikube ip):31809/cluster/members | python -m json.tool
*   Trying 192.168.99.102:31809...
  % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
  0     0    0     0    0     0      0      0 --:--:-- --:--:-- --:--:--     0* Connected to 192.168.99.102 (192.168.99.102) port 31809 (#0)
> GET /cluster/members HTTP/1.1
> Host: 192.168.99.102:31809
> User-Agent: curl/7.70.0
> Accept: */*
>
* Mark bundle as not supporting multiuse
< HTTP/1.1 200 OK
< Server: akka-http/10.1.12
< Date: Fri, 19 Jun 2020 17:46:13 GMT
< Content-Type: application/json
< Content-Length: 570
<
{ [570 bytes data]
100   570  100   570    0     0   6867      0 --:--:-- --:--:-- --:--:--  6867
* Connection #0 to host 192.168.99.102 left intact
{
    "leader": "akka://woe-sim@172.17.0.11:25520",
    "members": [
        {
            "node": "akka://woe-sim@172.17.0.11:25520",
            "nodeUid": "7176760119283282430",
            "roles": [
                "dc-default"
            ],
            "status": "Up"
        },
        {
            "node": "akka://woe-sim@172.17.0.12:25520",
            "nodeUid": "6695287075719844052",
            "roles": [
                "dc-default"
            ],
            "status": "Up"
        },
        {
            "node": "akka://woe-sim@172.17.0.13:25520",
            "nodeUid": "-7478917548710968969",
            "roles": [
                "dc-default"
            ],
            "status": "Up"
        }
    ],
    "oldest": "akka://woe-sim@172.17.0.11:25520",
    "oldestPerRole": {
        "dc-default": "akka://woe-sim@172.17.0.11:25520"
    },
    "selfNode": "akka://woe-sim@172.17.0.12:25520",
    "unreachable": []
}
~~~