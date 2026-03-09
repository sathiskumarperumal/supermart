project_id = "proven-country-485607-p6"

# ─── Optional (defaults shown) ────────────────────────────────────────────────
region      = "us-central1"
suffix      = "supermart"
environment = "dev"
db_name     = "supermartdb"
db_user     = "supermart"

# Sensitive vars — set via GitHub Actions secrets (-var flags), NOT here:
#   db_password = "..."
#   jwt_secret  = "..."
#   image       = "..."   # set dynamically by CI from the built image tag
