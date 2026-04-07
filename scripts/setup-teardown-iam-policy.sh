#!/usr/bin/env bash
###############################################################################
# Setup Teardown IAM Policy
#
# Attaches the teardown permissions policy to the GitHubActionsDeploymentRole
# in each AWS account so the teardown.yml workflow can delete resources.
#
# Run this ONCE per account before using the teardown workflow.
#
# USAGE:
#   ./scripts/setup-teardown-iam-policy.sh [--account <account_id>] [--all]
#
# PREREQUISITES:
#   - AWS CLI v2 configured with credentials for the target account(s)
#   - For cross-account: root account credentials that can assume roles
###############################################################################
set -euo pipefail

ROLE_NAME="GitHubActionsDeploymentRole"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../" && pwd)"

# Two policies (IAM has a 6144-byte limit per managed policy)
POLICY_NAMES=("TurafTeardownPolicy" "TurafTeardownPolicy2")
POLICY_FILES=(
  "infrastructure/iam-policies/github-actions-teardown-policy.json"
  "infrastructure/iam-policies/github-actions-teardown-policy-2.json"
)
POLICY_DESCS=(
  "Teardown permissions Part 1: compute, networking, ELB, Lambda, RDS"
  "Teardown permissions Part 2: S3, messaging, monitoring, IAM, data"
)

# Account IDs
DEV_ACCOUNT="801651112319"
QA_ACCOUNT="965932217544"
PROD_ACCOUNT="811783768245"

ALL_ACCOUNTS=("$DEV_ACCOUNT" "$QA_ACCOUNT" "$PROD_ACCOUNT")
ALL_NAMES=("dev" "qa" "prod")
ALL_PROFILES=("turaf-dev" "turaf-qa" "turaf-prod")

log()  { echo -e "\033[1;34m[INFO]\033[0m  $*"; }
ok()   { echo -e "\033[1;32m[OK]\033[0m    $*"; }
err()  { echo -e "\033[1;31m[ERROR]\033[0m $*"; }

create_and_attach_policy() {
  local policy_name=$1
  local policy_file=$2
  local policy_desc=$3
  local account_id=$4

  local policy_doc
  policy_doc=$(cat "${SCRIPT_DIR}/${policy_file}")

  local policy_arn="arn:aws:iam::${account_id}:policy/${policy_name}"
  if aws iam get-policy --policy-arn "$policy_arn" &>/dev/null; then
    log "Policy ${policy_name} already exists — creating new version..."
    local versions
    versions=$(aws iam list-policy-versions --policy-arn "$policy_arn" \
      --query 'Versions[?!IsDefaultVersion].VersionId' --output text)
    local count
    count=$(echo "$versions" | wc -w | tr -d ' ')
    if [[ "$count" -ge 4 ]]; then
      local oldest
      oldest=$(echo "$versions" | awk '{print $NF}')
      aws iam delete-policy-version --policy-arn "$policy_arn" --version-id "$oldest"
      log "  Deleted old policy version: ${oldest}"
    fi
    aws iam create-policy-version \
      --policy-arn "$policy_arn" \
      --policy-document "$policy_doc" \
      --set-as-default
    ok "Updated policy ${policy_name}"
  else
    aws iam create-policy \
      --policy-name "$policy_name" \
      --policy-document "$policy_doc" \
      --description "$policy_desc"
    ok "Created policy ${policy_name}"
  fi

  if aws iam list-attached-role-policies --role-name "$ROLE_NAME" \
    --query "AttachedPolicies[?PolicyName=='${policy_name}']" --output text | grep -q "$policy_name"; then
    log "Policy ${policy_name} already attached to ${ROLE_NAME}"
  else
    aws iam attach-role-policy \
      --role-name "$ROLE_NAME" \
      --policy-arn "$policy_arn"
    ok "Attached ${policy_name} to ${ROLE_NAME}"
  fi
}

attach_policy_to_current_account() {
  local account_id
  account_id=$(aws sts get-caller-identity --query 'Account' --output text)
  log "Working in account: ${account_id}"

  for i in "${!POLICY_NAMES[@]}"; do
    create_and_attach_policy "${POLICY_NAMES[$i]}" "${POLICY_FILES[$i]}" "${POLICY_DESCS[$i]}" "$account_id"
  done
}

usage() {
  echo "Usage: $0 [--account <account_id>] [--all]"
  echo ""
  echo "Options:"
  echo "  --account <id>   Attach policy in a specific account (must have active credentials)"
  echo "  --all            Attach policy in all environment accounts (dev, qa, prod)"
  echo "  (no args)        Attach policy in the currently-authenticated account"
}

# ─── Parse arguments ──────────────────────────────────────────────────
TARGET_ACCOUNT=""
ALL=false

while [[ $# -gt 0 ]]; do
  case $1 in
    --account)  TARGET_ACCOUNT="$2"; shift 2 ;;
    --all)      ALL=true; shift ;;
    --help|-h)  usage; exit 0 ;;
    *)          err "Unknown argument: $1"; usage; exit 1 ;;
  esac
done

# ─── Execute ──────────────────────────────────────────────────────────
if $ALL; then
  for i in "${!ALL_ACCOUNTS[@]}"; do
    ACCT="${ALL_ACCOUNTS[$i]}"
    NAME="${ALL_NAMES[$i]}"
    PROFILE="${ALL_PROFILES[$i]}"
    echo ""
    log "═══ Processing ${NAME} (${ACCT}) — profile: ${PROFILE} ═══"

    # Try to assume role in the target account
    CREDS=$(aws sts assume-role \
      --role-arn "arn:aws:iam::${ACCT}:role/${ROLE_NAME}" \
      --role-session-name "setup-teardown-${NAME}" \
      --duration-seconds 900 \
      --output json 2>/dev/null) || {
        err "Cannot assume ${ROLE_NAME} in ${ACCT}. Trying AWS profile ${PROFILE}..."

        if aws sts get-caller-identity --profile "$PROFILE" &>/dev/null; then
          AWS_PROFILE="$PROFILE" attach_policy_to_current_account
        else
          err "Profile ${PROFILE} not available either. Configure credentials manually:"
          err "  AWS_PROFILE=${PROFILE} $0"
        fi
        continue
      }

    export AWS_ACCESS_KEY_ID=$(echo "$CREDS" | jq -r '.Credentials.AccessKeyId')
    export AWS_SECRET_ACCESS_KEY=$(echo "$CREDS" | jq -r '.Credentials.SecretAccessKey')
    export AWS_SESSION_TOKEN=$(echo "$CREDS" | jq -r '.Credentials.SessionToken')

    attach_policy_to_current_account

    unset AWS_ACCESS_KEY_ID AWS_SECRET_ACCESS_KEY AWS_SESSION_TOKEN
  done
else
  attach_policy_to_current_account
fi

echo ""
ok "Done. The teardown workflow can now delete resources using ${ROLE_NAME}."
echo ""
echo "To run the teardown:"
echo "  1. Go to GitHub → Actions → 'Teardown Environment'"
echo "  2. Click 'Run workflow'"
echo "  3. Select the environment (dev/qa/prod)"
echo "  4. Type DESTROY to confirm"
echo "  5. Only the repository owner (ryanwaite28) can trigger it"
