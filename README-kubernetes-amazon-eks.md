
# Setup AWS Elastic Kubernetes Service - EKS

- [Setup AWS Elastic Kubernetes Service - EKS](#setup-aws-elastic-kubernetes-service---eks)
  - [Install CLIs](#install-clis)
    - [Install Kubernetes CLI](#install-kubernetes-cli)
  - [Create an EKS cluster](#create-an-eks-cluster)
  - [Deploy the Kubernetes Dashboard (Optional)](#deploy-the-kubernetes-dashboard-optional)
  - [Delete a Kubernetes cluster](#delete-a-kubernetes-cluster)

## Install CLIs

Go to [Getting started with eksctl](https://docs.aws.amazon.com/eks/latest/userguide/getting-started-eksctl.html)
for directions on setting up EKS and Kubernetes CLI tools.

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

## Create an EKS cluster

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

## Deploy the Kubernetes Dashboard (Optional)

You may want to deploy the Kubernetes dashboard. This is an optional step. To deploy the dashboard follow the
[Tutorial: Deploy the Kubernetes Dashboard (web UI)](https://docs.aws.amazon.com/eks/latest/userguide/dashboard-tutorial.html).

## Delete a Kubernetes cluster

~~~bash
eksctl delete cluster \
    --region <cluster-region> \
    --name <cluster-name>
~~~

> **Note** Deleting the cluster from the Amazon EKS console new tab requires several steps and therefore it’s easiest to use eksctl for this.

---
Return to the deployment [README](README.md#setup-a-kubernetes-cluster).
