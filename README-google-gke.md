# TODO

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
