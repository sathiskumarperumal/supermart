variable "project_id" {
  description = "GCP project ID"
  type        = string
}

variable "region" {
  description = "GCP region"
  type        = string
}

variable "suffix" {
  description = "Resource name suffix"
  type        = string
}

variable "environment" {
  description = "Deployment environment"
  type        = string
}

variable "db_name" {
  description = "Database name"
  type        = string
  default     = "supermartdb"
}

variable "db_user" {
  description = "Database application user"
  type        = string
  default     = "supermart"
}

variable "db_password" {
  description = "Database application user password"
  type        = string
  sensitive   = true
}

variable "db_tier" {
  description = "Cloud SQL machine tier"
  type        = string
  default     = "db-f1-micro"
}
