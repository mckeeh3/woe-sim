
# Deploy woe-sim to Amazon EKS

Go to [Getting started with eksctl](https://docs.aws.amazon.com/eks/latest/userguide/getting-started-eksctl.html)
for directions on setting up EKS and Kubernetes CLI tools.

Recommend that you create an EKS cluster with two or more Kubernetes nodes.

## Some Additional CLI Setup

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

You may also want to create an alias for the `kubectl` command, such as `kc`.

~~~bash
alias kc=kubectl
~~~

## Setup Database

Follow the installation instructions in the
[README-database-cassandra.md](https://github.com/mckeeh3/woe-sim/blob/master/README-database-cassandra-postgres.md)
or, for a Yugabyte installation in the
[README-database-yugrbyte.md](https://github.com/mckeeh3/woe-sim/blob/master/README-minikube-yugabyte.md).

## Deploy the woe-sim microservice

TODO
