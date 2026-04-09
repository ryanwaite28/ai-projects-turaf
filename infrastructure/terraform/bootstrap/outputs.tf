output "s3_bucket_name" {
  description = "Name of the S3 bucket for Terraform state"
  value       = aws_s3_bucket.terraform_state.id
}

output "s3_bucket_arn" {
  description = "ARN of the S3 bucket for Terraform state"
  value       = aws_s3_bucket.terraform_state.arn
}

output "dynamodb_table_name" {
  description = "Name of the DynamoDB table for state locking (created via AWS CLI in bootstrap workflow)"
  value       = "turaf-terraform-locks"
}

output "ecr_repositories" {
  description = "Map of ECR repository names to their URLs"
  value = {
    for k, v in aws_ecr_repository.services : k => v.repository_url
  }
}

output "ecr_repository_arns" {
  description = "Map of ECR repository names to their ARNs"
  value = {
    for k, v in aws_ecr_repository.services : k => v.arn
  }
}
