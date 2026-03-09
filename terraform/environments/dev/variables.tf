variable "project_id" {
  description = "GCP project ID"
  type        = string
}

variable "region" {
  description = "GCP region"
  type        = string
  default     = "us-central1"
}

variable "suffix" {
  description = "Resource name suffix used in all GCP resource names"
  type        = string
  default     = "supermart"
}

variable "environment" {
  description = "Deployment environment"
  type        = string
  default     = "dev"
}

variable "image" {
  description = "Full container image reference (set dynamically by CI)"
  type        = string
}

variable "db_name" {
  description = "MySQL database name"
  type        = string
  default     = "supermartdb"
}

variable "db_user" {
  description = "MySQL application user"
  type        = string
  default     = "supermart"
}

variable "db_password" {
  description = "MySQL application user password"
  type        = string
  sensitive   = true
}

variable "jwt_secret" {
  description = "JWT signing secret (hex string, min 32 bytes)"
  type        = string
  sensitive   = true
}
