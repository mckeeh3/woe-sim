# Of Things Internet Simulator - OTI Sim Microservice

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



















end-of-line
