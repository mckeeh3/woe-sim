
# Google Cloud Container Registry

See [Quickstart for Container Registry](https://cloud.google.com/container-registry/docs/quickstart) and
[Using Container Registry with Google Cloud](https://cloud.google.com/container-registry/docs/using-with-google-cloud-platform) for detailed instructions.

Complete the steps in the Quickstart for Container Registry [Before you begin](https://cloud.google.com/container-registry/docs/quickstart#before-you-begin) section.

## Authorize Docker

Authorize Docker to use `gcloud` to push/pull images to/from the container registry.

~~~bash
gcloud auth configure-docker
~~~

## Build, tag, and push the Docker image

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
Return to the deployment [README](README.md#setup-docker-repository).
