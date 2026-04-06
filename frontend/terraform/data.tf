data "terraform_remote_state" "infra" {
  backend = "s3"
  config = {
    bucket = "turaf-terraform-state-${var.environment}"
    key    = "terraform.tfstate"
    region = "us-east-1"
  }
}

data "aws_caller_identity" "current" {}
data "aws_region" "current" {}
