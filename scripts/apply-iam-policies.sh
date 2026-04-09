#!/usr/bin/env bash
###############################################################################
# apply-iam-policies.sh
#
# Applies all GitHub Actions IAM policies to GitHubActionsDeploymentRole in
# each AWS account (dev, qa, prod) as MANAGED policies (attached to the role).
#
# Managed policies have no combined size limit on the role — inline policies
# share a 10,240-char combined quota per role, which the full policy set
# exceeds. Each managed policy is limited to 6,144 non-whitespace characters.
#
# Policy files in infrastructure/iam-policies/ are listed in the POLICIES map
# below. Adding a new policy means adding one entry there — nothing else.
# The trust policy (github-actions-trust-policy.json) is excluded; it is the
# role's assume-role policy and is set at role creation time.
#
# On first run the script migrates any previously-applied inline policies
# (GitHubActionsDeploymentPolicy, GitHubActionsTerraformPolicy) to managed
# so the role is not left with a mix.
#
# USAGE:
#   ./scripts/apply-iam-policies.sh              # current account
#   ./scripts/apply-iam-policies.sh --env dev    # one environment
#   ./scripts/apply-iam-policies.sh --all        # all environments
#   ./scripts/apply-iam-policies.sh --verify     # verify only, no changes
#   ./scripts/apply-iam-policies.sh --list       # list policies and exit
#
# PREREQUISITES:
#   - AWS CLI v2
#   - jq
#   - AWS profiles configured: turaf-dev, turaf-qa, turaf-prod
#     OR active credentials for the target account
###############################################################################
set -euo pipefail

# ─── Configuration ────────────────────────────────────────────────────────────

ROLE_NAME="GitHubActionsDeploymentRole"
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
POLICY_DIR="${REPO_ROOT}/infrastructure/iam-policies"

# "filename|ManagedPolicyName" — order matters for readability only.
# Each file must be <= 6,144 non-whitespace characters.
POLICIES=(
  "github-actions-deployment-policy.json|GitHubActionsDeploymentPolicy"
  "github-actions-terraform-policy.json|GitHubActionsTerraformPolicy"
  "github-actions-teardown-policy.json|TurafTeardownPolicy"
  "github-actions-teardown-policy-2.json|TurafTeardownPolicy2"
)

# Inline policies to remove during migration (put-role-policy remnants).
# These were applied before the switch to managed policies.
LEGACY_INLINE_POLICIES=(
  "GitHubActionsDeploymentPolicy"
  "GitHubActionsTerraformPolicy"
)

# Account registry
ALL_NAMES=("dev"           "qa"           "prod")
ALL_ACCOUNTS=("801651112319" "965932217544" "811783768245")
ALL_PROFILES=("turaf-dev"  "turaf-qa"     "turaf-prod")

# ─── Logging ──────────────────────────────────────────────────────────────────

log()  { echo -e "\033[1;34m[INFO]\033[0m  $*"; }
ok()   { echo -e "\033[1;32m[OK]\033[0m    $*"; }
warn() { echo -e "\033[1;33m[WARN]\033[0m  $*"; }
err()  { echo -e "\033[1;31m[ERROR]\033[0m $*" >&2; }

# ─── Operations ───────────────────────────────────────────────────────────────

list_policies() {
  echo "Policies applied to ${ROLE_NAME} as managed policies:"
  echo ""
  printf "  %-48s %s\n" "File" "Managed Policy Name"
  printf "  %-48s %s\n" "----" "-------------------"
  for entry in "${POLICIES[@]}"; do
    local file="${entry%%|*}"
    local name="${entry##*|}"
    local path="${POLICY_DIR}/${file}"
    local exists="✓"
    [[ -f "$path" ]] || exists="❌ MISSING"
    printf "  %-48s %s  %s\n" "$file" "$name" "$exists"
  done
  echo ""
  echo "Excluded: github-actions-trust-policy.json  (role trust/assume policy)"
}

# Creates or updates a managed policy, then attaches it to the role.
upsert_managed_policy() {
  local policy_name="$1"
  local policy_doc="$2"
  local account_id="$3"

  local policy_arn="arn:aws:iam::${account_id}:policy/${policy_name}"

  if aws iam get-policy --policy-arn "$policy_arn" &>/dev/null 2>&1; then
    # Policy exists — create a new version (AWS keeps up to 5; prune oldest if needed)
    local old_versions
    old_versions=$(aws iam list-policy-versions \
      --policy-arn "$policy_arn" \
      --query 'Versions[?!IsDefaultVersion].VersionId' \
      --output text 2>/dev/null || true)
    local count
    count=$(echo "$old_versions" | wc -w | tr -d ' ')
    if [[ "$count" -ge 4 ]]; then
      local oldest
      oldest=$(echo "$old_versions" | awk '{print $NF}')
      aws iam delete-policy-version --policy-arn "$policy_arn" --version-id "$oldest"
    fi
    aws iam create-policy-version \
      --policy-arn "$policy_arn" \
      --policy-document "$policy_doc" \
      --set-as-default
  else
    aws iam create-policy \
      --policy-name "$policy_name" \
      --policy-document "$policy_doc"
  fi

  # Attach to role if not already attached
  if ! aws iam list-attached-role-policies --role-name "$ROLE_NAME" \
      --query "AttachedPolicies[?PolicyName=='${policy_name}'].PolicyName" \
      --output text 2>/dev/null | grep -q "^${policy_name}$"; then
    aws iam attach-role-policy \
      --role-name "$ROLE_NAME" \
      --policy-arn "$policy_arn"
  fi
}

migrate_inline_to_managed() {
  # Remove any inline policies left from a previous put-role-policy run.
  for policy_name in "${LEGACY_INLINE_POLICIES[@]}"; do
    if aws iam get-role-policy \
        --role-name "$ROLE_NAME" \
        --policy-name "$policy_name" \
        --query 'PolicyName' \
        --output text 2>/dev/null | grep -q "^${policy_name}$"; then
      aws iam delete-role-policy \
        --role-name "$ROLE_NAME" \
        --policy-name "$policy_name"
      log "Removed legacy inline policy: ${policy_name}"
    fi
  done
}

apply_policies() {
  local env_label="${1:-}"
  local account_id
  account_id=$(aws sts get-caller-identity --query 'Account' --output text)
  local label="${env_label:-account ${account_id}}"

  log "Applying ${#POLICIES[@]} managed policies to ${ROLE_NAME} in ${label} (${account_id})..."

  migrate_inline_to_managed

  for entry in "${POLICIES[@]}"; do
    local file="${entry%%|*}"
    local policy_name="${entry##*|}"
    local policy_path="${POLICY_DIR}/${file}"

    if [[ ! -f "$policy_path" ]]; then
      err "Policy file not found: ${policy_path}"
      exit 1
    fi

    local policy_doc
    policy_doc=$(sed "s/\${ACCOUNT_ID}/${account_id}/g" "$policy_path")

    upsert_managed_policy "$policy_name" "$policy_doc" "$account_id"
    ok "${policy_name}  ←  ${file}"
  done
}

verify_policies() {
  local env_label="${1:-}"
  local account_id
  account_id=$(aws sts get-caller-identity --query 'Account' --output text)
  local label="${env_label:-account ${account_id}}"

  log "Verifying ${#POLICIES[@]} managed policies on ${ROLE_NAME} in ${label} (${account_id})..."

  local all_ok=true
  for entry in "${POLICIES[@]}"; do
    local policy_name="${entry##*|}"
    local policy_arn="arn:aws:iam::${account_id}:policy/${policy_name}"
    local attached
    attached=$(aws iam list-attached-role-policies \
      --role-name "$ROLE_NAME" \
      --query "AttachedPolicies[?PolicyName=='${policy_name}'].PolicyName" \
      --output text 2>/dev/null || true)
    if echo "$attached" | grep -q "^${policy_name}$"; then
      ok "${policy_name} ✓"
    else
      warn "${policy_name} — NOT ATTACHED"
      all_ok=false
    fi
  done

  if $all_ok; then
    ok "All policies attached in ${label}"
  else
    warn "One or more policies missing in ${label} — run without --verify to apply"
    return 1
  fi
}

# ─── Account dispatch ─────────────────────────────────────────────────────────

run_in_account() {
  local account_id="$1"
  local env_name="$2"
  local profile="$3"
  local operation="$4"

  echo ""
  log "═══ ${env_name} (${account_id}) ═══"

  if aws sts get-caller-identity --profile "$profile" &>/dev/null 2>&1; then
    AWS_PROFILE="$profile" "${operation}_policies" "$env_name"
    return
  fi

  warn "Profile '${profile}' unavailable — attempting assume-role..."

  local creds
  creds=$(aws sts assume-role \
    --role-arn "arn:aws:iam::${account_id}:role/${ROLE_NAME}" \
    --role-session-name "apply-iam-policies-${env_name}" \
    --duration-seconds 900 \
    --output json 2>/dev/null) || {
    err "Cannot access ${env_name} (${account_id}) via profile '${profile}' or assume-role."
    err "Configure credentials and retry:  AWS_PROFILE=${profile} $0 --env ${env_name}"
    return 1
  }

  export AWS_ACCESS_KEY_ID=$(echo "$creds"    | jq -r '.Credentials.AccessKeyId')
  export AWS_SECRET_ACCESS_KEY=$(echo "$creds" | jq -r '.Credentials.SecretAccessKey')
  export AWS_SESSION_TOKEN=$(echo "$creds"    | jq -r '.Credentials.SessionToken')

  "${operation}_policies" "$env_name"

  unset AWS_ACCESS_KEY_ID AWS_SECRET_ACCESS_KEY AWS_SESSION_TOKEN
}

# ─── Argument parsing ─────────────────────────────────────────────────────────

usage() {
  cat <<EOF
Usage: $0 [options]

Options:
  (no args)       Apply all policies in the currently-authenticated account
  --env <env>     Apply in a specific environment (dev | qa | prod)
  --all           Apply in all environments (dev, qa, prod)
  --verify        Check policies are attached — no changes made
  --list          List policy files and their managed policy names, then exit
  --help, -h      Show this help

Policies are created as customer-managed policies and attached to
${ROLE_NAME}. The \${ACCOUNT_ID} placeholder in each policy file is
substituted with the actual account ID at runtime.
EOF
}

TARGET_ENV=""
ALL=false
OPERATION="apply"

while [[ $# -gt 0 ]]; do
  case $1 in
    --env)     TARGET_ENV="$2"; shift 2 ;;
    --all)     ALL=true; shift ;;
    --verify)  OPERATION="verify"; shift ;;
    --list)    list_policies; exit 0 ;;
    --help|-h) usage; exit 0 ;;
    *)         err "Unknown argument: $1"; usage; exit 1 ;;
  esac
done

# ─── Execute ──────────────────────────────────────────────────────────────────

if $ALL; then
  for i in "${!ALL_ACCOUNTS[@]}"; do
    run_in_account "${ALL_ACCOUNTS[$i]}" "${ALL_NAMES[$i]}" "${ALL_PROFILES[$i]}" "$OPERATION"
  done

elif [[ -n "$TARGET_ENV" ]]; then
  matched=false
  for i in "${!ALL_NAMES[@]}"; do
    if [[ "${ALL_NAMES[$i]}" == "$TARGET_ENV" ]]; then
      run_in_account "${ALL_ACCOUNTS[$i]}" "${ALL_NAMES[$i]}" "${ALL_PROFILES[$i]}" "$OPERATION"
      matched=true
      break
    fi
  done
  if ! $matched; then
    err "Unknown environment '${TARGET_ENV}'. Valid values: dev, qa, prod"
    exit 1
  fi

else
  "${OPERATION}_policies"
fi

echo ""
ok "Done."
[[ "$OPERATION" == "apply" ]] && echo -e "\nVerify with:\n  $0 --verify --all"
