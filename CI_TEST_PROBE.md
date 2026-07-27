# Quality Gate Workflow - Manual Step Required

The workflow YAML for this PR lives at `ci-workflow-draft/quality-gate.yml` instead of
`.github/workflows/quality-gate.yml` because the GitHub API rejected writes to
`.github/workflows/` with the PAT currently configured for this automation
(GitHub requires the `workflow` OAuth scope on the token for that path specifically).

To activate CI:
1. Either add the `workflow` scope to the PAT and re-run this push, or
2. Manually copy `ci-workflow-draft/quality-gate.yml` to `.github/workflows/quality-gate.yml`
   (e.g. via `git mv` locally, or the GitHub web UI), then delete this draft directory.

Also required before the Sonar step will succeed: add a `SONAR_TOKEN` repository secret
(Settings -> Secrets and variables -> Actions) - this cannot be created via the API either.
