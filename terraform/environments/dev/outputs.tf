output "cloud_run_url" {
  description = "Public URL of the deployed Supermart IoT API (dev)"
  value       = module.compute.service_url
}

output "swagger_ui_url" {
  description = "Swagger UI URL"
  value       = "${module.compute.service_url}/api/swagger-ui.html"
}

output "cloudsql_instance" {
  description = "Cloud SQL instance connection name"
  value       = module.cloudsql.instance_connection_name
}
