# OLD - DO NOT USE - Where on Earth Simulator (woe-sim) Microservice

This microservice simulates geographically distributed IoT devices. This service manages the creation, state, and deletion of IoT devices.

## Design notes

TODO

## How to Deploy the WoE Simulator Microservice

- [Minikube with Yugabyte DB](README-minikube-yugabyte.md)
- Google Kubernetes Engine with Yugabyte DB (TODO)
- AWS Elastic Kubernetes Service (TODO)

-----
**Note** The following documentation is being replaced with environment specific README documents.

## Deploy the WOE Sim Microservice

The `kubectl` CLI provides a nice Kubectl Autocomplete feature for `bash` and `zsh`.
See the [kubectl Cheat Sheet](https://kubernetes.io/docs/reference/kubectl/cheatsheet/#kubectl-autocomplete) for instructions.

Also, consider installing [`kubectx`](https://github.com/ahmetb/kubectx), which also includes `kubens`.
Mac:
~~~bash
$ brew install kubectx
~~~
Arch Linux:
~~~bash
$ yay kubectx
~~~

#### Enable Access to Google Kubernetes Engine - GKE

TODO

#### Enable Access to Amazon Elastic Kubernetes Service - EKS

Go to [Getting started with eksctl](https://docs.aws.amazon.com/eks/latest/userguide/getting-started-eksctl.html)
for directions on setting up EKS and Kubernetes CLI tools.

See AWS EKS sections below for specific instructions on setting up Cassandra and deploying the woe-sim microserivce to an EKS cluster.

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

Add the local docker image into MiniKube.
~~~bash
$ minikube cache add woe-sim:latest
$ minikube cache list
~~~
~~~              
woe-sim:latest
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

### Verify Internal HTTP access
The WOE Twin and WOE Sim microservices communicate with each other via HTTP. Each
microservie needs to know the host name of the other service. Use the following to
verify the hostname of this service.

First, get the IP assigned to the load balancer.
~~~bash
$ kubectl get services woe-sim-service
~~~
~~~
NAME              TYPE           CLUSTER-IP      EXTERNAL-IP   PORT(S)                                        AGE
woe-sim-service   LoadBalancer   10.107.51.103   <pending>     2552:32361/TCP,8558:31809/TCP,8080:30968/TCP   15m
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
Address 1: 10.107.51.103 woe-sim-service.woe-sim-1.svc.cluster.local
/ #
~~~
Note that the load balancer host name is `woe-sim-service.woe-sim-1.svc.cluster.local`.

Verify that the WOE Twin HTTP server is accessible via the host name.
~~~
/ # wget -qO- http://woe-sim-service.woe-sim-1.svc.cluster.local:8558/cluster/members
{"leader":"akka://woe-sim@172.17.0.11:25520","members":[{"node":"akka://woe-sim@172.17.0.11:25520","nodeUid":"7176760119283282430","roles":["dc-default"],"status":"Up"},{"node":"akka://woe-sim@172.17.0.12:25520","nodeUid":"6695287075719844052","roles":["dc-default"],"status":"Up"},{"node":"akka://woe-sim@172.17.0.13:25520","nodeUid":"-7478917548710968969","roles":["dc-default"],"status":"Up"}],"oldest":"akka://woe-sim@172.17.0.11:25520","oldestPerRole":{"dc-default":"akka://woe-sim@172.17.0.11:25520"},"selfNode":"akka://woe-sim@172.17.0.13:25520","unreachable":[]}/ #
/ #
~~~
Leave the shell using the `exit` command.
~~~
/ # exit
pod "dns-test" deleted
~~~

### Build and Deploy to Google Cloud Container Registry

First, create a GKE (Google Kubernetes Engine) project. From the
[Google Cloud Platform](https://console.cloud.google.com) Dashboard, click The
triple bar icon at the top left and click Kubernetes Engine/Clusters. Follow the
documentation TODO for creating a cluster and a project.

Use the [Quickstart for Container Registry](https://cloud.google.com/container-registry/docs/quickstart)
to create a Docker image container registry.

Deploy [Yugabyte](https://docs.yugabyte.com/latest/deploy/kubernetes/single-zone/gke/helm-chart/) to the GKE cluster.

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

Configure authentication to the Container Registry.
See [Authentication methods](https://cloud.google.com/container-registry/docs/advanced-authentication).
Here the [gcloud as a Docker credential helper](https://cloud.google.com/container-registry/docs/advanced-authentication#gcloud-helper)
method is used.
~~~bash
$ gcloud auth login
~~~

Configure Docker with the following command:
~~~bash
$ gcloud auth configure-docker
~~~

Tag the Docker image.
~~~bash
$ docker tag woe-sim gcr.io/$(gcloud config get-value project)/woe-sim:latest
~~~

Push the Docker image to the ContainerRegistry.
~~~bash
$ docker push gcr.io/$(gcloud config get-value project)/woe-sim:latest
~~~

To view the uploaded container search for "container registry" from the Google Cloud Console.
You can also list the uploaded containers via the CLI.
~~~bash
$ gcloud container images list                    
~~~
~~~
NAME
gcr.io/akka-yuga/woe-sim
Only listing images in gcr.io/akka-yuga. Use --repository to list images in other repositories.
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
Context "gke_akka-yuga_us-central1-c_yugadb" modified.
~~~

Deploy the Docker images to the Kubernetes cluster.
~~~bash
$ kubectl apply -f kubernetes/akka-cluster-gke.yml
~~~
~~~
deployment.apps/woe-sim created
role.rbac.authorization.k8s.io/pod-reader created
rolebinding.rbac.authorization.k8s.io/read-pods created
~~~

View the status of the running pods.
~~~bash
$ kubectl get pods   
~~~
~~~
NAME                       READY   STATUS    RESTARTS   AGE
woe-sim-5d4949bf95-7z8mw   1/1     Running   0          21h
woe-sim-5d4949bf95-b2hv9   1/1     Running   0          21h
woe-sim-5d4949bf95-ggqsm   1/1     Running   0          21h
~~~

Open a shell on one of the pods.
~~~bash
$ kubectl exec -it woe-sim-5d4949bf95-7z8mw -- /bin/bash                        
~~~
~~~
root@woe-sim-5d4949bf95-7z8mw:/# ll maven/woe-sim-1.0-SNAPSHOT.jar
-rw-r--r-- 1 root root 301036 Jun 29 18:08 maven/woe-sim-1.0-SNAPSHOT.jar
root@woe-sim-5d4949bf95-7z8mw:/# exit
exit
~~~
#### Scale Running Akka Nodes/K8 pods

~~~bash
$ kubectl scale --replicas=10 deployment/woe-sim
~~~

### Enable External Access

Create a load balancer to enable access to the WOE Sim microservice HTTP endpoint.

~~~bash
$ kubectl expose deployment woe-sim --type=LoadBalancer --name=woe-sim-service
~~~
~~~
service/woe-sim-service exposed
~~~

~~~bash
$ kubectl get services woe-sim-service
~~~
~~~
NAME              TYPE           CLUSTER-IP   EXTERNAL-IP   PORT(S)                                        AGE
woe-sim-service   LoadBalancer   10.89.3.57   <pending>     2552:31693/TCP,8558:31376/TCP,8080:31238/TCP   9s
~~~

It takes a few minutes for an external IP to be assigned. Note the `EXTERNAL-IP` above eventually changes from `<pending>` to the assigned external IP, shown below.

~~~bash
$ kubectl get services woe-sim-service
~~~
~~~
NAME              TYPE           CLUSTER-IP   EXTERNAL-IP     PORT(S)                                        AGE
woe-sim-service   LoadBalancer   10.89.3.57   35.232.168.63   2552:31693/TCP,8558:31376/TCP,8080:31238/TCP   5m21s
~~~

### Deploy to AWS EKS

At this point a GKE cluster has been created and is ready for deployment. See section *Enable Access to Amazon Elastic Kubernetes Service - EKS* above on how to get started.

Ensure that you have access to the GKE cluster.
~~~bash
$ kubectl get svc
~~~
~~~
NAME         TYPE        CLUSTER-IP   EXTERNAL-IP   PORT(S)   AGE
kubernetes   ClusterIP   10.100.0.1   <none>        443/TCP   8d
~~~