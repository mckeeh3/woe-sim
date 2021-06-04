
# Setup and configure a Container Registry

See the below section for the container registry for your specific environment.

> **Note** Docker Hub can be used for any Kubernetes environment.

- [Setup and configure a Container Registry](#setup-and-configure-a-container-registry)
  - [Amazon Elastic Container Registry (ECR)](#amazon-elastic-container-registry-ecr)
    - [Create a repository](#create-a-repository)
    - [Authenticate to your default registry](#authenticate-to-your-default-registry)
    - [Build and push a Docker image to the AWS ECR repository](#build-and-push-a-docker-image-to-the-aws-ecr-repository)
  - [Google Cloud Container Registry](#google-cloud-container-registry)
    - [Authorize Docker](#authorize-docker)
    - [Build, tag, and push the Docker image](#build-tag-and-push-the-docker-image)
  - [Docker Hub Container Registry](#docker-hub-container-registry)
    - [Log in to a Docker registry](#log-in-to-a-docker-registry)
    - [Build and push a Docker image to Docker Hub repository](#build-and-push-a-docker-image-to-docker-hub-repository)

## Amazon Elastic Container Registry (ECR)

You need one repository for each application that you will deploy, with the same name as the application.

See [Using Amazon ECR with the AWS CLI](https://docs.aws.amazon.com/AmazonECR/latest/userguide/getting-started-cli.html) for detailed instructions.

### Create a repository

Create a repository using either the [ECR Console](https://console.aws.amazon.com/ecr/repositories?region=us-east-1) or the `aws ecr` CLI.

~~~bash
aws ecr create-repository --repository-name <my-app-name>
~~~

~~~bash
aws ecr describe-repositories --repository-names woe-sim
~~~

### Authenticate to your default registry

~~~bash
aws ecr get-login-password --region region | \
docker login --username AWS --password-stdin aws_account_id.dkr.ecr.region.amazonaws.com
~~~

~~~bash
aws ecr get-login-password --region us-east-1 | \
docker login --username AWS --password-stdin 405074255555.dkr.ecr.us-east-1.amazonaws.com
~~~

### Build and push a Docker image to the AWS ECR repository

~~~bash
mvn clean package -Ddocker.registry=<aws-account-id>.dkr.ecr.<region>.amazonaws.com
~~~

~~~bash
mvn clean package -Ddocker.registry=405074255555.dkr.ecr.us-east-1.amazonaws.com
~~~

## Google Cloud Container Registry

See [Quickstart for Container Registry](https://cloud.google.com/container-registry/docs/quickstart) and
[Using Container Registry with Google Cloud](https://cloud.google.com/container-registry/docs/using-with-google-cloud-platform) for detailed instructions.

Complete the steps in the Quickstart for Container Registry [Before you begin](https://cloud.google.com/container-registry/docs/quickstart#before-you-begin) section.

### Authorize Docker

Authorize Docker to use `gcloud` to push/pull images to/from the container registry.

~~~bash
gcloud auth configure-docker
~~~

### Build, tag, and push the Docker image

Next, use Maven to create a Docker image and push it to the Google Container Registry.

~~~bash
mvn clean package
~~~

Once the Docker image has been created tag and push it to the repo.

~~~bash
docker tag <image-name> gcr.io/<project-name>/<image-name>:<tag>
~~~

For example.

~~~bash
docker tag woe-sim gcr.io/woe-yugabyte/woe-sim:latest
~~~

Push the image to the repo.

~~~bash
docker push gcr.io/<project-name>/<image-name>:<tag>
~~~

For example.

~~~bash
docker push gcr.io/woe-yugabyte/woe-sim:latest
~~~

~~~text
The push refers to repository [gcr.io/woe-yugabyte/woe-sim]
00bf4dae6fb5: Pushed
f770ac324967: Layer already exists
80b956beb7fc: Layer already exists
ddc500d84994: Layer already exists
c64c52ea2c16: Layer already exists
5930c9e5703f: Layer already exists
b187ff70b2e4: Layer already exists
latest: digest: sha256:fb7ebf0a66c84e753f77f4e02751ec180068f105510136b78438d946d7b783bf size: 1788
~~~

---


## Docker Hub Container Registry

See the [Docker Hub Quickstart](https://docs.docker.com/docker-hub/) for detailed instructions.

### Log in to a Docker registry

~~~bash
docker login --password-stdin --username <your-docker-user>
~~~

### Build and push a Docker image to Docker Hub repository

Change the Kubernetes deployment `.yml` file changing the image name required to pull images from Docker Hub.

In the `spec/template/spec/contaimers/image` field, change the image name to `<your-docker-user>/<image-name>`.
Such as `mckeeh3/woe-sim`, `mckeeh3/woe-twin`, `mckeeh3/visualizer`, etc.

For Docker Hub, the image name format is `<your-docker-username>/<app name>`.

~~~bash
mvn clean package docker:push -Ddocker.name=mckeeh3/woe-sim
~~~

~~~bash
mvn clean package docker:push -Ddocker.name=mckeeh3/woe-twin
~~~

~~~bash
mvn clean package docker:push -Ddocker.name=mckeeh3/woe-simulator
~~~

~~~bash
mvn clean package docker:push -Ddocker.name=mckeeh3/woe-visualizer
~~~

---
Return to the deployment [README](README.md#setup-docker-repository).
