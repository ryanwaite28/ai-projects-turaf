terraform {
  backend "s3" {
    bucket         = "turaf-terraform-state-${var.environment}"
    key            = "services/bff-api/terraform.tfstate"
    region         = "us-east-1"
    encrypt        = true
    dynamodb_table = "turaf-terraform-locks"
  }
}
