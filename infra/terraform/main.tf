locals {
  cluster_name   = "telemetryhub-${var.environment}"
  common_labels  = { app = "telemetryhub", environment = var.environment }
  node_networks  = ["10.0.0.0/24", "10.0.1.0/24", "10.0.2.0/24"]
  postgres_tier  = "db-custom-2-7680"
  postgres_ha    = var.environment == "production" ? true : false
  db_credentials = google_sql_user.app_user
}

resource "google_project_service" "services" {
  for_each = toset([
    "compute.googleapis.com",
    "container.googleapis.com",
    "sqladmin.googleapis.com",
    "redis.googleapis.com",
    "logging.googleapis.com",
    "monitoring.googleapis.com",
  ])
  project            = var.project_id
  service            = each.value
  disable_on_destroy = false
}

resource "google_compute_network" "main" {
  name                    = "telemetryhub-vpc"
  auto_create_subnetworks = false
  project                 = var.project_id
}

resource "google_compute_subnetwork" "nodes" {
  name          = "telemetryhub-subnet"
  ip_cidr_range = var.node_cidr
  region        = var.region
  network       = google_compute_network.main.id
}

resource "google_container_cluster" "main" {
  name                     = local.cluster_name
  location                 = var.region
  remove_default_node_pool = true
  initial_node_count       = 1
  deletion_protection      = false

  network    = google_compute_network.main.id
  subnetwork = google_compute_subnetwork.nodes.id

  private_cluster_config {
    enable_private_nodes    = true
    enable_private_endpoint = false
    master_ipv4_cidr_block  = var.master_cidr
  }

  node_pool {
    name       = "default-pool"
    node_count = 1
  }

  ip_allocation_policy {
    cluster_ipv4_cidr_block  = var.pods_cidr
    services_ipv4_cidr_block = var.services_cidr
  }

  workload_identity_config {
    workload_pool = "${var.project_id}.svc.id.goog"
  }

  master_authorized_networks_config {
    cidr_blocks {
      cidr_block   = var.authorized_cidr
      display_name = "ops"
    }
  }
}

resource "google_container_node_pool" "apps" {
  name       = "apps-pool"
  location   = var.region
  cluster    = google_container_cluster.main.name
  node_count = var.node_count

  node_config {
    machine_type = var.node_machine_type
    disk_size_gb = 40
    disk_type    = "pd-standard"

    labels = local.common_labels

    oauth_scopes = [
      "https://www.googleapis.com/auth/cloud-platform",
    ]

    workload_metadata_config {
      mode = "GKE_METADATA"
    }
  }

  autoscaling {
    min_node_count = var.min_nodes
    max_node_count = var.max_nodes
  }
}

resource "google_sql_database_instance" "postgres" {
  name                = "telemetryhub-postgres"
  database_version    = "POSTGRES_15"
  region              = var.region
  deletion_protection = false

  settings {
    tier              = local.postgres_tier
    disk_type         = "PD_SSD"
    disk_size         = 40
    availability_type = local.postgres_ha ? "REGIONAL" : "ZONAL"

    ip_configuration {
      private_network = google_compute_network.main.id
    }

    backup_configuration {
      enabled                        = true
      start_time                     = "03:00"
      point_in_time_recovery_enabled = true
    }

    database_flags {
      name  = "timescaledb.telemetry"
      value = "on"
    }
  }
}

resource "google_sql_database" "telemetryhub" {
  name     = "telemetryhub"
  instance = google_sql_database_instance.postgres.name
}

resource "google_sql_user" "app_user" {
  name     = "telemetryhub_app"
  instance = google_sql_database_instance.postgres.name
  password = var.db_password
}

resource "google_redis_instance" "cache" {
  name           = "telemetryhub-redis"
  tier           = "STANDARD_HA"
  memory_size_gb = 1
  region         = var.region
  redis_version  = "REDIS_7_0"

  authorized_network = google_compute_network.main.id
}