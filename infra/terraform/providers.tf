terraform {
  required_version = ">= 1.5"

  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 5.0"
    }
  }

  backend "gcs" {
    bucket = "telemetryhub-tfstate"
    prefix = "production"
  }
}

provider "google" {
  project = var.project_id
  region  = var.region
}