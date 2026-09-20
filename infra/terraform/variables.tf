variable "project_id" {
  description = "Identifiant du projet Google Cloud"
  type        = string
}

variable "environment" {
  description = "Environnement cible (dev / staging / production)"
  type        = string
  default     = "dev"
}

variable "region" {
  description = "Région GCP de déploiement"
  type        = string
  default     = "europe-west1"
}

variable "node_count" {
  description = "Nombre initial de nœuds du pool applicatif"
  type        = number
  default     = 3
}

variable "node_machine_type" {
  description = "Type de machine des nœuds GKE"
  type        = string
  default     = "e2-standard-4"
}

variable "min_nodes" {
  description = "Nombre minimal de nœuds (autoscaling)"
  type        = number
  default     = 2
}

variable "max_nodes" {
  description = "Nombre maximal de nœuds (autoscaling)"
  type        = number
  default     = 10
}

variable "node_cidr" {
  description = "CIDR du sous-réseau des nœuds"
  type        = string
  default     = "10.0.0.0/24"
}

variable "pods_cidr" {
  description = "CIDR des Pods (GKE)"
  type        = string
  default     = "10.10.0.0/16"
}

variable "services_cidr" {
  description = "CIDR des Services (GKE)"
  type        = string
  default     = "10.20.0.0/16"
}

variable "master_cidr" {
  description = "CIDR du plan de contrôle GKE privé"
  type        = string
  default     = "172.16.0.0/28"
}

variable "authorized_cidr" {
  description = "CIDR autorisé à joindre le master GKE"
  type        = string
  default     = "0.0.0.0/0"
}

variable "db_password" {
  description = "Mot de passe du compte applicatif Cloud SQL"
  type        = string
  sensitive   = true
}