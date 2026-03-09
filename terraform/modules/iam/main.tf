locals {
  sa_name = "cr-sa-${var.suffix}-${var.environment}"
}

resource "google_service_account" "cloud_run_sa" {
  project      = var.project_id
  account_id   = local.sa_name
  display_name = "Cloud Run SA — ${var.suffix} ${var.environment}"
}

# Pull images from GCR / Artifact Registry
resource "google_project_iam_member" "ar_reader" {
  project = var.project_id
  role    = "roles/artifactregistry.reader"
  member  = "serviceAccount:${google_service_account.cloud_run_sa.email}"
}

# Connect to Cloud SQL instances
resource "google_project_iam_member" "cloudsql_client" {
  project = var.project_id
  role    = "roles/cloudsql.client"
  member  = "serviceAccount:${google_service_account.cloud_run_sa.email}"
}
