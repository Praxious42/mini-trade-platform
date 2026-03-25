Prometheus scrape config examples

Local / Docker Compose:

scrape_configs:
  - job_name: 'order-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['localhost:8081']

  - job_name: 'portfolio-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['localhost:8083']

Kubernetes (service discovery):

scrape_configs:
  - job_name: 'kubernetes-pods'
    kubernetes_sd_configs:
      - role: pod
    relabel_configs:
      - source_labels: [__meta_kubernetes_pod_label_app]
        regex: (order-service|portfolio-service)
        action: keep
      - source_labels: [__address__]
        target_label: __address__
        replacement: $1

Notes:
- The services in this repo expose the actuator under /actuator. Prometheus needs to use /actuator/prometheus as the metrics_path.
- If actuator endpoints are protected by Spring Security, either allow EndpointRequest.to("prometheus") in your security config or run the management server on a separate port and expose it without auth.

