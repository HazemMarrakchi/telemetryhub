output "cluster_name" {
  description = "Nom du cluster GKE"
  value       = google_container_cluster.main.name
}

output "cluster_endpoint" {
  description = "Endpoint du cluster GKE"
  value       = google_container_cluster.main.endpoint
  sensitive   = true
}

output "postgres_instance" {
  description = "Instance Cloud SQL (PostgreSQL + TimescaleDB)"
  value       = google_sql_database_instance.postgres.name
}

output "redis_instance" {
  description = "Instance Memorystore (Redis)"
  value       = google_redis_instance.cache.id
}

output "vpc_name" {
  description = "Nom du VPC"
  value       = google_compute_network.main.name
}