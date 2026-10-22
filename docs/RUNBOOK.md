# Llama Express — Runbook

Operating guide for the single-machine deployment (Docker Desktop on Windows + WSL2).
Commands are written for **Windows PowerShell 5.1** and run from the repository root unless a folder is given.

---

## 1. What runs where

| Component | Runs on | Local access | Public access |
|---|---|---|---|
| Frontend (React) | Vercel (production) / K8s `frontend` (local) | http://localhost | https://llama.vzkyrielight.my.id |
| Backend (Spring Boot ×2) | K8s `llama/backend` | http://localhost/api | https://llama-api.vzkyrielight.my.id (`/api`, `/ws` only) |
| Oracle, Kafka, Redis | K8s StatefulSets in `llama` | — | — |
| Gateway | NGINX Gateway Fabric, Service `llama-gateway-nginx` (LoadBalancer → localhost:80) | http://localhost | via tunnel |
| Cloudflare Tunnel | K8s `llama/cloudflared` ×2 | — | Zero Trust → Networks → Tunnels → `llama-express` |
| Prometheus / Grafana / Alertmanager | K8s `monitoring` (Helm release `kps`) | port-forward (§4) | — |
| Jenkins | docker compose (`ci` profile), container `llama-jenkins` | http://localhost:8090 | `https://jenkins-hook.vzkyrielight.my.id/github-webhook/` only |
| SonarQube | docker compose (`quality` profile) | http://localhost:9000 | — |

The compose `dev`, `app` and `monitoring` profiles duplicate what Kubernetes runs; keep them **stopped** unless you develop outside Kubernetes.

---

## 2. Start-up after a reboot

1. Start **Docker Desktop** and wait until Kubernetes shows *running*.
2. Jenkins and SonarQube restart automatically (`restart: unless-stopped`). Confirm:
   ```powershell
   docker compose -f Infra\docker-compose.yml ps -a --format "table {{.Name}}\t{{.Status}}"
   curl.exe -s http://localhost:9000/api/system/status      # "status":"UP" (SonarQube needs 1–2 min)
   ```
   If SonarQube is not running: `docker compose -f Infra\docker-compose.yml --profile quality up -d sonarqube`
3. Wait for the cluster to settle (Oracle needs the longest):
   ```powershell
   kubectl -n llama get pods
   kubectl -n monitoring get pods
   ```
4. Run the health check in §3.

---

## 3. Health check

```powershell
"--- pods ---"
kubectl -n llama get pods
"--- local gateway ---"
"/                 -> " + (curl.exe -s -o NUL -w '%{http_code}' http://localhost/)                     # 200
"/api/v1/users/me  -> " + (curl.exe -s -o NUL -w '%{http_code}' http://localhost/api/v1/users/me)     # 401
"--- public API through the tunnel ---"
"api /users/me     -> " + (curl.exe -s -o NUL -w '%{http_code}' https://llama-api.vzkyrielight.my.id/api/v1/users/me)  # 401
"--- tunnel connectors ---"
kubectl -n llama logs deploy/cloudflared --tail 20 | Select-String 'Registered tunnel connection|ERR'
"--- resources ---"
kubectl top nodes
```

A **401** from `/api/v1/users/me` is the healthy answer: the request reached the backend and security rejected the missing token.

---

## 4. Tools

| Tool | Command | URL |
|---|---|---|
| Grafana | `kubectl -n monitoring port-forward svc/kps-grafana 3001:80` | http://localhost:3001 (dashboard **Llama Express — Overview**) |
| Prometheus | `kubectl -n monitoring port-forward svc/prometheus-operated 9090:9090` | http://localhost:9090 |
| Alertmanager | `kubectl -n monitoring port-forward svc/alertmanager-operated 9093:9093` | http://localhost:9093 |

Grafana admin password:
```powershell
kubectl -n monitoring get secret grafana-admin -o jsonpath='{.data.admin-password}' | % { [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($_)) }
```

Kafka consumer groups:
```powershell
kubectl -n llama exec kafka-0 -- /opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --list
```
Expected: `llama-dispatch`, `llama-location-history`, and one `llama-ws-status-*` + one `llama-ws-location-*` **per backend pod**.

`kubectl port-forward` binds to a single pod; it exits when that pod restarts. Re-run it.

---

## 5. Deployments

**Normal flow:** merge `feat/*` → `dev` → `main` through pull requests. Jenkins (`main`) tests, scans, builds and pushes images, sets the new SHA tag on the Deployments, waits for the rollout and runs a smoke test. Vercel builds the frontend from `main`.

**Applying manifest changes** (ConfigMaps, probes, policies) is manual:
```powershell
cd Infra
kubectl diff -k k8s/overlays/local
kubectl apply -k k8s/overlays/local
kubectl apply -k k8s/monitoring            # ServiceMonitor, alert rules, dashboard
```
- `apply -k` sets the images to the `:main` tag (same content as the last `main` build). Jenkins sets the SHA tag again on the next deploy.
- If a manifest change depends on new application code (e.g. a new probe path), merge and deploy the code **first**, then apply the manifest.

**Monitoring stack upgrade / values change:**
```powershell
helm upgrade --install kps prometheus-community/kube-prometheus-stack --version 92.11.0 --namespace monitoring -f k8s/monitoring/values-local.yaml --wait --timeout 10m
```

---

## 6. Rollback

The pipeline rolls back automatically when the rollout or the smoke test fails.

Manual rollback to a known image:
```powershell
kubectl -n llama rollout history deployment/backend
kubectl -n llama set image deployment/backend  backend=farhanrahman27/llama-express-backend:<sha12>
kubectl -n llama set image deployment/frontend frontend=farhanrahman27/llama-express-frontend:<sha12>
kubectl -n llama rollout status deployment/backend --timeout=10m
```
The backend uses `maxSurge: 0, maxUnavailable: 1`: pods are replaced one at a time, the other pod keeps serving, and WebSocket clients reconnect and resync.

Vercel: Deployments → pick a previous production deployment → **Promote to Production** (or *Instant Rollback*).

---

## 7. Secrets

| Secret | Where | Notes |
|---|---|---|
| Compose env | `Infra/.env` | git-ignored |
| Oracle / backend / tunnel | `Infra/k8s/overlays/local/secrets/` (`oracle.env`, `backend.env`, `cloudflared.token`) | git-ignored; turned into K8s Secrets by Kustomize |
| Grafana admin | K8s Secret `monitoring/grafana-admin` | created manually |
| Jenkins credentials | Jenkins → Credentials: `github`, `dockerhub`, `sonar-token`, `midtrans-client-key`, `kubeconfig-llama`, `github-webhook-secret` | never in the Jenkinsfile |
| Vercel | Project → Settings → Environment Variables (`VITE_API_BASE_URL`, `VITE_MIDTRANS_CLIENT_KEY`) | public values baked at build time |

Before every commit: `git status --short` must not list `.env`, `secrets/*` or tokens.

**Rotating the tunnel token:** Zero Trust → Tunnels → `llama-express` → refresh token → write it to `secrets/cloudflared.token` (no trailing newline) → `kubectl apply -k k8s/overlays/local` → `kubectl -n llama rollout restart deploy/cloudflared`.

**Rotating the GitHub webhook secret:** update the Jenkins credential `github-webhook-secret` and the secret in GitHub → Settings → Webhooks, then check *Recent Deliveries*.

---

## 8. Known incidents

| Symptom | Cause | Fix |
|---|---|---|
| Pipeline fails at **SonarQube** | SonarQube container stopped (e.g. after an unclean shutdown) | Start it (§2); restart policy is `unless-stopped` |
| Backend stage: dozens of `ApplicationContext failure threshold exceeded` errors | Testcontainers Oracle timed out (`DATABASE IS READY TO USE!`) because builds ran in parallel on a busy machine | Jenkins runs with **1 executor**; re-run the build. Find the first `Caused by` in the build log |
| Frontend calls `https://llama…/api/...` and gets 404 | `VITE_API_BASE_URL` missing at build time (Vite bakes `VITE_*` into the bundle) | Fix the variable for **Production** in Vercel, then redeploy |
| Refreshing a deep link on Vercel returns 404 | `vercel.json` (SPA rewrite) not deployed | Make sure `Frontend/frontend/vercel.json` is on `main` |
| Merge to `main` but no new Vercel production deployment | Vercel skipped the commit | Check the Vercel status on the commit in GitHub; create a deployment from `main` manually |
| Grafana restarts (`OOMKilled`) | Go heap + file cache over the limit | `grafana.goMemLimit.factor: 0.5`, limit 768Mi (values-local.yaml) |
| Kubernetes resource dashboards are empty | Docker Desktop's cAdvisor has no per-container labels | Use the Llama Express dashboard (reads kubelet `/metrics/resource`) |
| node-exporter `CrashLoopBackOff`: "not a shared or slave mount" | Docker Desktop mount propagation | `hostRootFsMount.mountPropagation: ""` (already set) |
| Public API returns 502/1033 | cloudflared pods down or tunnel disconnected | §3 tunnel check; `kubectl -n llama rollout restart deploy/cloudflared` |
| Oracle `CrashLoopBackOff` with `ORA-01081` | stale shared memory / semaphores | the StatefulSet command clears IPC before start; if it persists, check `/dev/shm` sizing and the PVC |
| A compose backend container keeps restarting | `on-failure` policy restarted it after Docker started | `docker rm -f llama-express-backend-1` (Kubernetes runs the backend) |
| Real-time updates missing for a few seconds after a deploy | pods replaced, WebSockets reconnecting | expected; the client resyncs from REST after reconnecting |

PowerShell 5.1 pitfalls: arguments containing `"` or `:` can be mangled when passed to native tools. Quote JVM flags (`'-XX:+PrintCommandLineFlags'`), pass JSON bodies via files (`--data-binary "@file.json"`), and prefer `--patch-file` for `kubectl patch`.

---

## 9. Capacity

WSL2 has 16 GB. Typical usage is ~75% of node memory with both backend pods, monitoring, Jenkins and SonarQube running. Heavy moments: a Jenkins backend test stage (+1.5–2 GB for Testcontainers Oracle). To free memory temporarily: `docker compose -f Infra\docker-compose.yml stop sonarqube`.

---

## 10. Shutting down

Stopping Docker Desktop is enough; everything restarts with the steps in §2. The public API is unavailable while the machine is off. The Vercel frontend stays up and shows login/API errors.
