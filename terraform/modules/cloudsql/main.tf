locals {
  instance_name = "mysql-${var.suffix}-${var.environment}"
}

resource "google_sql_database_instance" "main" {
  project             = var.project_id
  name                = local.instance_name
  region              = var.region
  database_version    = "MYSQL_8_0"
  deletion_protection = false

  settings {
    tier              = var.db_tier
    availability_type = "ZONAL"

    backup_configuration {
      enabled            = true
      binary_log_enabled = true
    }

    ip_configuration {
      ipv4_enabled = true
      # Public IP — Cloud Run sidecar connects via Cloud SQL Auth Proxy over TLS
      authorized_networks {
        value = "0.0.0.0/0"
        name  = "allow-all-for-proxy"
      }
    }

    database_flags {
      name  = "character_set_server"
      value = "utf8mb4"
    }
  }
}

resource "google_sql_database" "app_db" {
  project  = var.project_id
  instance = google_sql_database_instance.main.name
  name     = var.db_name
  charset  = "utf8mb4"
}

resource "google_sql_user" "app_user" {
  project  = var.project_id
  instance = google_sql_database_instance.main.name
  name     = var.db_user
  password = var.db_password
  host     = "%"
}
