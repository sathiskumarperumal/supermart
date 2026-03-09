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

variable "image" {
  description = "Full container image reference for the Supermart IoT API"
  type        = string
}

variable "service_account_email" {
  description = "Email of the Cloud Run service account"
  type        = string
}

variable "cloudsql_connection_name" {
  description = "Cloud SQL instance connection name (PROJECT:REGION:INSTANCE)"
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

variable "jwt_secret" {
  description = "JWT signing secret"
  type        = string
  sensitive   = true
}
