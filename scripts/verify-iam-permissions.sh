#!/bin/bash

ROLE_NAME="GitHubActionsDeploymentRole"

for ENV in dev qa prod; do
  echo "Verifying permissions in $ENV..."
  
  aws iam get-role-policy \
    --role-name $ROLE_NAME \
    --policy-name GitHubActionsDeploymentPolicy \
    --profile turaf-$ENV \
    --query 'PolicyDocument.Statement[?Sid==`ArchitectureTestReports`]' \
    --output json
    
  aws iam get-role-policy \
    --role-name $ROLE_NAME \
    --policy-name GitHubActionsDeploymentPolicy \
    --profile turaf-$ENV \
    --query 'PolicyDocument.Statement[?Sid==`CloudFrontInvalidation`]' \
    --output json
done
