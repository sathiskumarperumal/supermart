terraform {
  required_version = ">= 1.7.0"

  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 6.0"
    }
  }
}

provider "google" {
  project = var.project_id
  region  = var.region
}

# Note: GCS state bucket (tfstate-supermart-dev, location=US) is created
# by a gcloud pre-step in the GitHub Actions workflow to avoid bootstrap circular dependency.

module "iam" {
  source      = "../../modules/iam"
  project_id  = var.project_id
  suffix      = var.suffix
  environment = var.environment
}

module "cloudsql" {
  source      = "../../modules/cloudsql"
  project_id  = var.project_id
  region      = var.region
  suffix      = var.suffix
  environment = var.environment
  db_name     = var.db_name
  db_user     = var.db_user
  db_password = var.db_password
}

module "compute" {
  source                   = "../../modules/compute"
  project_id               = var.project_id
  region                   = var.region
  suffix                   = var.suffix
  environment              = var.environment
  image                    = var.image
  service_account_email    = module.iam.service_account_email
  cloudsql_connection_name = module.cloudsql.instance_connection_name
  db_name                  = var.db_name
  db_user                  = var.db_user
  db_password              = var.db_password
  jwt_secret               = var.jwt_secret
}
