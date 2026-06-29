# Kubernetes Deploy Runbook

## Prerequisites

- Kubernetes cluster available
- `kubectl` configured
- Container images pushed for the services referenced in `k8s/base/*.yaml`
- Backing infrastructure provisioned or replaced with managed services

## Apply base manifests

```shell
kubectl apply -k k8s/base
```

## Verify resources

```shell
kubectl get ns
kubectl get all -n shopping
kubectl get hpa -n shopping
```

## Update images

Example:

```shell
kubectl set image deployment/order-service order-service=shopping/order-service:latest -n shopping
kubectl set image deployment/payment-service payment-service=shopping/payment-service:latest -n shopping
kubectl set image deployment/inventory-service inventory-service=shopping/inventory-service:latest -n shopping
```

## Inspect rollout

```shell
kubectl rollout status deployment/order-service -n shopping
kubectl rollout status deployment/payment-service -n shopping
kubectl rollout status deployment/inventory-service -n shopping
```

## Check logs

```shell
kubectl logs deployment/order-service -n shopping
kubectl logs deployment/payment-service -n shopping
kubectl logs deployment/inventory-service -n shopping
```

## Delete resources

```shell
kubectl delete -k k8s/base
```

## Notes

- `secret.example.yaml` is only an example. Replace with real secrets before production.
- Current manifests cover the core platform path first: discovery, gateway, order, payment, inventory.
- Add managed PostgreSQL, MongoDB, Redis, Kafka, and ingress before production rollout.
