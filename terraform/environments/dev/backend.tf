terraform {
  backend "gcs" {
    # Bucket is created by a gcloud pre-step in the GitHub Actions workflow
    bucket = "tfstate-supermart-dev"
    prefix = "terraform/state"
  }
}
