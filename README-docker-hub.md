
# Docker Hub Container Registry

See the [Docker Hub Quickstart](https://docs.docker.com/docker-hub/) for detailed instructions.

## Log in to a Docker registry

~~~bash
docker login --password-stdin --username <your-docker-user>
~~~

## Modify the Kubernetes deployment yaml file for Docker Hub

Change the Kubernetes deployment `.yml` file changing the image name required to pull images from Docker Hub.

In the `spec/template/spec/contaimers/image` field, change the image name to `<your-docker-user>/<image-name>`.
Such as `mckeeh3/woe-sim`, `mckeeh3/woe-twin`, `mckeeh3/visualizer`, etc.

For Docker Hub, the image name format is `<your-docker-username>/<app name>`.

## Build and push a Docker image to Docker Hub repository

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
