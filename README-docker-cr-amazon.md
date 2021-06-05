
# Setup an Amazon Elastic Container Registry (ECR)

You need one repository for each application that you will deploy, with the same name as the application.

See [Using Amazon ECR with the AWS CLI](https://docs.aws.amazon.com/AmazonECR/latest/userguide/getting-started-cli.html) for detailed instructions.

## Create a repository

Create a repository using either the [ECR Console](https://console.aws.amazon.com/ecr/repositories?region=us-east-1) or the `aws ecr` CLI.

~~~bash
aws ecr create-repository --repository-name <my-app-name>
~~~

~~~bash
aws ecr describe-repositories --repository-names woe-sim
~~~

## Authenticate to your default registry

~~~bash
aws ecr get-login-password --region region | \
docker login --username AWS --password-stdin aws_account_id.dkr.ecr.region.amazonaws.com
~~~

~~~bash
aws ecr get-login-password --region us-east-1 | \
docker login --username AWS --password-stdin 405074255555.dkr.ecr.us-east-1.amazonaws.com
~~~

## Build and push a Docker image to the AWS ECR repository

~~~bash
mvn clean package -Ddocker.registry=<aws-account-id>.dkr.ecr.<region>.amazonaws.com
~~~

~~~bash
mvn clean package -Ddocker.registry=405074255555.dkr.ecr.us-east-1.amazonaws.com
~~~

---
Return to the deployment [README](README.md#setup-docker-repository).
