# Task: Configure AWS OIDC

**Service**: CI/CD  
**Phase**: 11  
**Estimated Time**: 2 hours  

## Objective

Configure AWS OIDC federation for GitHub Actions to authenticate without long-lived credentials.

## Prerequisites

- [ ] AWS account access
- [ ] GitHub repository created

## Scope

**Files to Create**:
- `infrastructure/terraform/modules/cicd/github-oidc.tf`

## Implementation Details

### OIDC Provider

```hcl
resource "aws_iam_openid_connect_provider" "github" {
  url = "https://token.actions.githubusercontent.com"
  
  client_id_list = ["sts.amazonaws.com"]
  
  thumbprint_list = [
    "6938fd4d98bab03faadb97b34396831e3780aea1"
  ]
}

resource "aws_iam_role" "github_actions" {
  name = "GitHubActionsRole"
  
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Principal = {
        Federated = aws_iam_openid_connect_provider.github.arn
      }
      Action = "sts:AssumeRoleWithWebIdentity"
      Condition = {
        StringEquals = {
          "token.actions.githubusercontent.com:aud" = "sts.amazonaws.com"
        }
        StringLike = {
          "token.actions.githubusercontent.com:sub" = "repo:your-org/turaf:*"
        }
      }
    }]
  })
}

resource "aws_iam_role_policy_attachment" "github_actions_ecr" {
  role       = aws_iam_role.github_actions.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryPowerUser"
}

resource "aws_iam_role_policy_attachment" "github_actions_ecs" {
  role       = aws_iam_role.github_actions.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonECS_FullAccess"
}
```

## Acceptance Criteria

- [ ] OIDC provider created
- [ ] IAM role configured
- [ ] Trust policy set correctly
- [ ] Permissions granted
- [ ] GitHub Actions can assume role
- [ ] No long-lived credentials needed

## Testing Requirements

**Validation**:
- Run GitHub Actions workflow
- Verify AWS authentication
- Check CloudTrail logs

## References

- Specification: `specs/ci-cd-pipelines.md` (AWS OIDC section)
- Related Tasks: All CI/CD tasks
