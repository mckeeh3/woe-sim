
# Create a load balancer in a kind environment

Create a load balancer to enable access to the woe-sim microservice HTTP endpoint.

The [Kind Load Balancer](https://kind.sigs.k8s.io/docs/user/loadbalancer/) documentation describes how to
get service of type LoadBalancer working in a kind cluster using Metallb.

The following are the steps provided in the Kind LoadBalancer documentation.

## Installing metallb using default manifests

Create the metallb namespace

~~~bash
kubectl apply -f https://raw.githubusercontent.com/metallb/metallb/master/manifests/namespace.yaml
~~~

### Create the memberlist secrets

~~~bash
kubectl create secret generic -n metallb-system memberlist --from-literal=secretkey="$(openssl rand -base64 128)" 
~~~

### Apply metallb manifest

~~~bash
kubectl apply -f https://raw.githubusercontent.com/metallb/metallb/master/manifests/metallb.yaml
~~~

Wait for metallb pods to have a status of Running

~~~bash
kubectl get pods -n metallb-system --watch
~~~

### Setup address pool used by loadbalancers

To complete layer2 configuration, we need to provide metallb a range of IP addresses it controls. We want this range to be on the docker kind network.

~~~bash
docker network inspect -f '{{.IPAM.Config}}' kind
~~~

Here is an example of the output from the above command.

~~~text
[{172.18.0.0/16  172.18.0.1 map[]} {fc00:f853:ccd:e793::/64  fc00:f853:ccd:e793::1 map[]}]
~~~

Create a file, such as `metallb-configmap.yaml`, that contains the following contents.

~~~text
apiVersion: v1
kind: ConfigMap
metadata:
  namespace: metallb-system
  name: config
data:
  config: |
    address-pools:
    - name: default
      protocol: layer2
      addresses:
      - 172.19.255.200-172.19.255.250
~~~

Edit the last line in the file to match the IP range that fits within the Docker network.
For example, based on the above `docker network inspect` command this IP range works.

~~~text
      - 172.18.255.200-172.18.255.250
~~~

The complete modified file looks like this.

~~~text
apiVersion: v1
kind: ConfigMap
metadata:
  namespace: metallb-system
  name: config
data:
  config: |
    address-pools:
    - name: default
      protocol: layer2
      addresses:
      - 172.18.255.200-172.18.255.250
~~~

Apply this modified file.

~~~bash
kc apply -f /tmp/metallb.yaml
~~~

### Create a Load Balancer to enable external access

Create a load balancer to enable access to the woe-sim microservice HTTP endpoint.

~~~bash
kubectl expose deployment woe-sim --type=LoadBalancer --name=woe-sim-service
~~~

~~~text
service/woe-sim-service exposed
~~~

Next, view to external port assignments.

~~~bash
kubectl get services woe-sim-service
~~~

~~~text
NAME              TYPE           CLUSTER-IP      EXTERNAL-IP      PORT(S)                                        AGE
woe-sim-service   LoadBalancer   10.96.130.159   172.18.255.200   8080:30924/TCP,8558:30721/TCP,2552:31361/TCP   15h
~~~

### Verify that the load balancer is working

Run a `curl` command using the load balancer IP and port 8558, that Akka Management port.

~~~bash
curl -v http://172.18.255.200:8558/cluster/members | python -m json.tool
~~~

~~~text
*   Trying 172.18.255.200:8558...
  % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
  0     0    0     0    0     0      0      0 --:--:-- --:--:-- --:--:--     0* Connected to 172.18.255.200 (172.18.255.200) port 8558 (#0)
> GET /cluster/members HTTP/1.1
> Host: 172.18.255.200:8558
> User-Agent: curl/7.76.1
> Accept: */*
>
* Mark bundle as not supporting multiuse
< HTTP/1.1 200 OK
< Server: akka-http/10.2.3
< Date: Mon, 24 May 2021 16:38:23 GMT
< Content-Type: application/json
< Content-Length: 568
<
{ [568 bytes data]
100   568  100   568    0     0   277k      0 --:--:-- --:--:-- --:--:--  277k
* Connection #0 to host 172.18.255.200 left intact
{
    "leader": "akka://woe-sim@10.244.0.26:25520",
    "members": [
        {
            "node": "akka://woe-sim@10.244.0.26:25520",
            "nodeUid": "925406639243941266",
            "roles": [
                "dc-default"
            ],
            "status": "Up"
        },
        {
            "node": "akka://woe-sim@10.244.0.27:25520",
            "nodeUid": "1608401123146447922",
            "roles": [
                "dc-default"
            ],
            "status": "Up"
        },
        {
            "node": "akka://woe-sim@10.244.0.28:25520",
            "nodeUid": "4831089820306204355",
            "roles": [
                "dc-default"
            ],
            "status": "Up"
        }
    ],
    "oldest": "akka://woe-sim@10.244.0.26:25520",
    "oldestPerRole": {
        "dc-default": "akka://woe-sim@10.244.0.26:25520"
    },
    "selfNode": "akka://woe-sim@10.244.0.28:25520",
    "unreachable": []
}
~~~
