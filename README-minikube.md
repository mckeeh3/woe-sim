
# Minikube Installation and Setup

Follow these instructions for installing and using Minikube.

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

## Install Minikube

Follow the [instructions](https://kubernetes.io/docs/tasks/tools/install-minikube/) for installing Minikube.

### Start Minikube

You may want to allocate more CPU and memory capacity to run the WoW application than the defaults. There are two `minikube` command options available for adjusting the CPU and memory allocation settings.

~~~bash
minikube start --driver=virtualbox --cpus=C --memory=M
~~~

For example, allocate 4 CPUs and 10 gig of memory.

~~~bash
minikube start --driver=virtualbox --cpus=4 --memory=10g
~~~

~~~text
😄  minikube v1.19.0 on Arch
🎉  minikube 1.20.0 is available! Download it: https://github.com/kubernetes/minikube/releases/tag/v1.20.0
💡  To disable this notice, run: 'minikube config set WantUpdateNotification false'

✨  Automatically selected the docker driver. Other choices: virtualbox, ssh
❗  Your cgroup does not allow setting memory.
    ▪ More information: https://docs.docker.com/engine/install/linux-postinstall/#your-kernel-does-not-support-cgroup-swap-limit-capabilities
👍  Starting control plane node minikube in cluster minikube
🚜  Pulling base image ...
💾  Downloading Kubernetes v1.20.2 preload ...
    > preloaded-images-k8s-v10-v1...: 491.71 MiB / 491.71 MiB  100.00% 39.83 Mi
    > gcr.io/k8s-minikube/kicbase...: 357.67 MiB / 357.67 MiB  100.00% 7.98 MiB
🔥  Creating docker container (CPUs=8, Memory=20480MB) ...
🐳  Preparing Kubernetes v1.20.2 on Docker 20.10.5 ...
    ▪ Generating certificates and keys ...
    ▪ Booting up control plane ...
    ▪ Configuring RBAC rules ...
🔎  Verifying Kubernetes components...
    ▪ Using image gcr.io/k8s-minikube/storage-provisioner:v5
🌟  Enabled addons: storage-provisioner, default-storageclass
🏄  Done! kubectl is now configured to use "minikube" cluster and "default" namespace by default
~~~

Once the `minikube` Kubernetes cluster is up you can check its status using the following commands.

~~~bash
kubectl get events
~~~

~~~text
LAST SEEN   TYPE     REASON                    OBJECT          MESSAGE
17s         Normal   Starting                  node/minikube   Starting kubelet.
17s         Normal   NodeHasSufficientMemory   node/minikube   Node minikube status is now: NodeHasSufficientMemory
17s         Normal   NodeHasNoDiskPressure     node/minikube   Node minikube status is now: NodeHasNoDiskPressure
17s         Normal   NodeHasSufficientPID      node/minikube   Node minikube status is now: NodeHasSufficientPID
17s         Normal   NodeAllocatableEnforced   node/minikube   Updated Node Allocatable limit across pods
17s         Normal   NodeReady                 node/minikube   Node minikube status is now: NodeReady
8s          Normal   RegisteredNode            node/minikube   Node minikube event: Registered Node minikube in Controller
~~~

~~~bash
kubectl get all -A
~~~

~~~text
kube-system   pod/coredns-74ff55c5b-g6c2d            0/1     Running   0          20s
kube-system   pod/etcd-minikube                      0/1     Running   0          29s
kube-system   pod/kube-apiserver-minikube            1/1     Running   0          29s
kube-system   pod/kube-controller-manager-minikube   0/1     Running   0          29s
kube-system   pod/kube-proxy-rzrd2                   0/1     Error     2          20s
kube-system   pod/kube-scheduler-minikube            0/1     Running   0          29s
kube-system   pod/storage-provisioner                1/1     Running   0          35s

NAMESPACE     NAME                 TYPE        CLUSTER-IP   EXTERNAL-IP   PORT(S)                  AGE
default       service/kubernetes   ClusterIP   10.96.0.1    <none>        443/TCP                  37s
kube-system   service/kube-dns     ClusterIP   10.96.0.10   <none>        53/UDP,53/TCP,9153/TCP   36s

NAMESPACE     NAME                        DESIRED   CURRENT   READY   UP-TO-DATE   AVAILABLE   NODE SELECTOR            AGE
kube-system   daemonset.apps/kube-proxy   1         1         0       1            0           kubernetes.io/os=linux   36s

NAMESPACE     NAME                      READY   UP-TO-DATE   AVAILABLE   AGE
kube-system   deployment.apps/coredns   0/1     1            0           36s

NAMESPACE     NAME                                DESIRED   CURRENT   READY   AGE
kube-system   replicaset.apps/coredns-74ff55c5b   1         1         0       20s
~~~
