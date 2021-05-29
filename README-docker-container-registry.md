
# Setup and configure a Container Registry

See the below section for the container registry for your specific environment.

Note, that Docker Hub can be used for any Kubernetes environment.

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
docker login --username AWS --password-stdin 405074236871.dkr.ecr.us-east-1.amazonaws.com
~~~

### Build and push a Docker image to the AWS ECR repository

~~~bash
mvn clean package -Ddocker.registry=aws_account_id.dkr.ecr.region.amazonaws.com
~~~

~~~bash
mvn clean package -Ddocker.registry=405074236871.dkr.ecr.us-east-1.amazonaws.com
~~~

## Google Cloud Container Registry

See [Quickstart for Container Registry](https://cloud.google.com/container-registry/docs/quickstart) and
[Using Container Registry with Google Cloud](https://cloud.google.com/container-registry/docs/using-with-google-cloud-platform) for detailed instructions.

TODO add CLI example steps

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
