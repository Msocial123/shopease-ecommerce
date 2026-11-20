# ShopEase on Kubernetes (Minikube)

One file per topic. Image: `muralisocial123/shopease:v1.0.0` and `:v2.0.0` (Docker Hub).
The files have **no namespace inside**, so the same files work in `shopease-dev` and `shopease-prod`.

| File | Topic |
|---|---|
| `00-namespaces.yaml` | Namespaces for environment separation |
| `01-configmap.yaml` | ConfigMap – as environment variables and as a volume |
| `02-secret.yaml` | Secret – as environment variables and as a volume |
| `03-nfs-pv-pvc.yaml` | PersistentVolume / PersistentVolumeClaim on NFS (**NFS server steps inside**) |
| `04-postgres-statefulset.yaml` | PostgreSQL with persistent storage – StatefulSet |
| `05-pod.yaml` | Pods |
| `06-replicaset.yaml` | ReplicaSets |
| `07-deployment.yaml` | Deployments, rolling updates and rollbacks (v1) |
| `08-deployment-v2.yaml` | Liveness, readiness, startup probes + resource requests/limits (v2) |
| `09-services.yaml` | Service types: ClusterIP, NodePort, LoadBalancer |
| `10-ingress.yaml` | Ingress, ingress controller, host and path routing |
| `11-hpa.yaml` | Horizontal Pod Autoscaler |

## 1. Prepare (once)

```bash
minikube start --driver=podman --container-runtime=containerd --cpus=2 --memory=4096
minikube addons enable ingress
minikube addons enable metrics-server
# NFS server + firewall: see the comments at the top of 03-nfs-pv-pvc.yaml
```

## 2. Apply in order

```bash
kubectl apply -f 00-namespaces.yaml
kubectl config set-context --current --namespace=shopease-dev

kubectl apply -f 01-configmap.yaml -f 02-secret.yaml
kubectl apply -f 03-nfs-pv-pvc.yaml -f 04-postgres-statefulset.yaml
kubectl get pv,pvc,pods                      # PVCs Bound, postgres-0 Running

kubectl apply -f 05-pod.yaml                 # Pod demo,        then: kubectl delete -f 05-pod.yaml
kubectl apply -f 06-replicaset.yaml          # ReplicaSet demo, then: kubectl delete -f 06-replicaset.yaml

kubectl apply -f 07-deployment.yaml          # v1
kubectl apply -f 08-deployment-v2.yaml       # rolling update to v2
kubectl rollout undo deployment/shopease     # rollback (re-apply 08 to go forward again)

kubectl apply -f 09-services.yaml -f 10-ingress.yaml -f 11-hpa.yaml
```

## 3. Open the shop

```bash
curl http://$(minikube ip):30080/health                          # NodePort
curl -H "Host: shopease.local" http://$(minikube ip)/            # Ingress
minikube tunnel        # (second terminal) -> EXTERNAL-IP for the LoadBalancer service
# From your laptop's browser (EC2/VM): forward a port that is open in the security group
kubectl port-forward --address 0.0.0.0 svc/shopease-svc 8085:80  # http://<public-ip>:8085/
```

Admin login: `admin@shopease.com` / `APP_ADMIN_PASSWORD` from `02-secret.yaml`.

## Notes
* The PVs are cluster-wide and can be bound only once – a second namespace (`shopease-prod`) needs its
  own PVs (copy the PV blocks with another name/NFS folder).
* `/health` returns 503 while PostgreSQL is down: pods become **not ready** (no traffic) but are not
  restarted – the liveness probe only checks the Tomcat port.
* Clean up: `kubectl delete namespace shopease-dev` and `kubectl delete pv shopease-uploads-pv postgres-nfs-pv`
  (data stays on the NFS server – reclaim policy `Retain`).
