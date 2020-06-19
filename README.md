# Of Things Internet Simulator - OTI Sim Microservice

TODO

### Design notes

TODO

### Deploy the OTI Sim Microservice

#### Yugabyte on Kubernetes or MiniKube

Follow the documentation for installing Kubernetes,
[MiniKube](https://kubernetes.io/docs/tasks/tools/install-minikube/)
and
[Yugabyte](https://download.yugabyte.com/#kubernetes).

The `kubectl` CLI provides a nice Kubectl Autocomplete feature for `bash` and `zsh`.
See the [kubectl Cheat Sheet](https://kubernetes.io/docs/reference/kubectl/cheatsheet/#kubectl-autocomplete) for instructions.

#### Create Cassandra Tables

Try the following commands to verify access to Cassandra CQL and PostgreSQL
CQL CLI tools once Yugabyte has been installed in a Kubernetes environment.

Cassandra CQL
~~~bash
$ kubectl --namespace yb-demo exec -it yb-tserver-0 -- /home/yugabyte/bin/ycqlsh yb-tserver-0

Defaulting container name to yb-tserver.
Use 'kubectl describe pod/yb-tserver-0 -n yb-demo' to see all of the containers in this pod.
Connected to local cluster at yb-tserver-0:9042.
[ycqlsh 5.0.1 | Cassandra 3.9-SNAPSHOT | CQL spec 3.4.2 | Native protocol v4]
Use HELP for help.
ycqlsh> quit
~~~

PostgreSQL
~~~bash
$ kubectl --namespace yb-demo exec -it yb-tserver-0 -- /home/yugabyte/bin/ysqlsh -h yb-tserver-0  --echo-queries

Defaulting container name to yb-tserver.
Use 'kubectl describe pod/yb-tserver-0 -n yb-demo' to see all of the containers in this pod.
ysqlsh (11.2-YB-2.1.8.1-b0)
Type "help" for help.

yugabyte=# \q
~~~

##### Copy CQL DDL commands to the Yugabyte server

From the oti-sim project directory.

~~~bash
$ kubectl cp src/main/resources/akka-persistence-journal.cql yb-demo/yb-tserver-0:/tmp                                                                  
Defaulting container name to yb-tserver.
~~~

##### Create the Cassandra and PostgreSQL Tables
Cassandra
~~~bash
$ kubectl --namespace yb-demo exec -it yb-tserver-0 -- /home/yugabyte/bin/ycqlsh yb-tserver-0                                                      
~~~
~~~
Defaulting container name to yb-tserver.
Use 'kubectl describe pod/yb-tserver-0 -n yb-demo' to see all of the containers in this pod.
Connected to local cluster at yb-tserver-0:9042.
[ycqlsh 5.0.1 | Cassandra 3.9-SNAPSHOT | CQL spec 3.4.2 | Native protocol v4]
Use HELP for help.
ycqlsh> source '/tmp/akka-persistence-journal.cql'
ycqlsh> describe keyspaces;

oti_simulator  system_schema  system_auth  system

ycqlsh> use oti_simulator;
ycqlsh:oti_simulator> describe tables;

tag_views  tag_scanning         tag_write_progress
messages   all_persistence_ids  metadata          

ycqlsh:oti_simulator>quit
~~~

### Build and Deploy to MiniKube

From the oti-sim project directory.

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
[INFO] --- docker-maven-plugin:0.26.1:build (default-cli) @ oti-sim ---
[INFO] Copying files to /home/hxmc/Lightbend/akka-java/oti-sim/target/docker/oti-sim/build/maven
[INFO] Building tar: /home/hxmc/Lightbend/akka-java/oti-sim/target/docker/oti-sim/tmp/docker-build.tar
[INFO] DOCKER> [oti-sim:latest]: Created docker-build.tar in 405 milliseconds
[INFO] DOCKER> [oti-sim:latest]: Built image sha256:ebe14
[INFO] DOCKER> [oti-sim:latest]: Tag with latest,20200617-143425.6247cf9
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  01:30 min
[INFO] Finished at: 2020-06-19T09:25:15-04:00
[INFO] ------------------------------------------------------------------------
~~~

Add the local docker image into MiniKube.
~~~bash
$ minikube cache add oti-sim:latest
$ minikube cache list
~~~
~~~              
oti-sim:latest
~~~

Create the Kubernetes namespace. The namespace only needs to be created once.
~~~bash
$ kubectl apply -f kubernetes/namespace.json     
~~~
~~~
namespace/oti-sim-1 created
~~~

Set this namespace as the default for subsequent `kubectl` commands.
~~~bash
$ kubectl config set-context --current --namespace=oti-sim-1
~~~
~~~
Context "minikube" modified.
~~~

Deploy the Docker images to the Kubernetes cluster.
~~~bash
$ kubectl apply -f kubernetes/akka-cluster.yml
~~~
~~~
deployment.apps/oti-sim created
role.rbac.authorization.k8s.io/pod-reader created
rolebinding.rbac.authorization.k8s.io/read-pods created
~~~
Check if the pods are running. This may take a few moments.
~~~bash
$ kubectl get pods                                          
~~~
~~~
NAME                      READY   STATUS    RESTARTS   AGE
oti-sim-bd5bf8ddc-mbjmv   1/1     Running   0          8m28s
oti-sim-bd5bf8ddc-tjtpk   1/1     Running   0          8m28s
oti-sim-bd5bf8ddc-z8gh5   1/1     Running   0          8m28s
~~~

### Enable External Access

Create a load balancer to enable access to the OTI Sim microservice HTTP endpoint.

~~~bash
$ kubectl expose deployment oti-sim --type=LoadBalancer --name=oti-sim-service
~~~
~~~
service/oti-sim-service exposed
~~~

Next, view to external port assignments.

~~~bash
$ kubectl get services oti-sim-service
~~~
~~~
NAME              TYPE           CLUSTER-IP      EXTERNAL-IP   PORT(S)                                        AGE
oti-sim-service   LoadBalancer   10.107.51.103   <pending>     2552:32361/TCP,8558:31809/TCP,8080:30968/TCP   108s
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
$ curl -v http://$(minikube ip):31809/cluster/members
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
    "leader": "akka://oti-sim@172.17.0.11:25520",
    "members": [
        {
            "node": "akka://oti-sim@172.17.0.11:25520",
            "nodeUid": "7176760119283282430",
            "roles": [
                "dc-default"
            ],
            "status": "Up"
        },
        {
            "node": "akka://oti-sim@172.17.0.12:25520",
            "nodeUid": "6695287075719844052",
            "roles": [
                "dc-default"
            ],
            "status": "Up"
        },
        {
            "node": "akka://oti-sim@172.17.0.13:25520",
            "nodeUid": "-7478917548710968969",
            "roles": [
                "dc-default"
            ],
            "status": "Up"
        }
    ],
    "oldest": "akka://oti-sim@172.17.0.11:25520",
    "oldestPerRole": {
        "dc-default": "akka://oti-sim@172.17.0.11:25520"
    },
    "selfNode": "akka://oti-sim@172.17.0.12:25520",
    "unreachable": []
}
~~~

### Verify Internal HTTP access
The OTI Twin and OTI Sim microservices communicate with each other via HTTP. Each
microservie needs to know the host name of the other service. Use the following to
verify the hostname of this service.

First, get the IP assigned to the load balancer.
~~~bash
$ kubectl get services oti-sim-service
~~~
~~~
NAME              TYPE           CLUSTER-IP      EXTERNAL-IP   PORT(S)                                        AGE
oti-sim-service   LoadBalancer   10.107.51.103   <pending>     2552:32361/TCP,8558:31809/TCP,8080:30968/TCP   15m
~~~

In this example, the internal load balancer IP is 10.107.51.103.

Next, run a shell that can be used to look around the Kubernetes network.
~~~bash
$ kubectl run -i --tty --image busybox:1.28 dns-test --restart=Never --rm
~~~
Use the nslookup command to see the DNS names assigned to the load balancer IP.
~~~
If you don't see a command prompt, try pressing enter.
/ # nslookup 10.107.51.103
Server:    10.96.0.10
Address 1: 10.96.0.10 kube-dns.kube-system.svc.cluster.local

Name:      10.107.51.103
Address 1: 10.107.51.103 oti-sim-service.oti-sim-1.svc.cluster.local
/ #
~~~
Note that the load balancer host name is `oti-sim-service.oti-sim-1.svc.cluster.local`.

Verify that the OTI Twin HTTP server is accessible via the host name.
~~~
/ # wget -qO- http://oti-sim-service.oti-sim-1.svc.cluster.local:8558/cluster/members
{"leader":"akka://oti-sim@172.17.0.11:25520","members":[{"node":"akka://oti-sim@172.17.0.11:25520","nodeUid":"7176760119283282430","roles":["dc-default"],"status":"Up"},{"node":"akka://oti-sim@172.17.0.12:25520","nodeUid":"6695287075719844052","roles":["dc-default"],"status":"Up"},{"node":"akka://oti-sim@172.17.0.13:25520","nodeUid":"-7478917548710968969","roles":["dc-default"],"status":"Up"}],"oldest":"akka://oti-sim@172.17.0.11:25520","oldestPerRole":{"dc-default":"akka://oti-sim@172.17.0.11:25520"},"selfNode":"akka://oti-sim@172.17.0.13:25520","unreachable":[]}/ #
/ #
~~~
Leave the shell using the `exit` command.
~~~
/ # exit
pod "dns-test" deleted
~~~



















end-of-line
