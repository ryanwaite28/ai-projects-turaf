terraform {
  backend "s3" {
    bucket         = "turaf-terraform-state-${var.environment}"
    key            = "services/ws-gateway/terraform.tfstate"
    region         = "us-east-1"
    encrypt        = true
    dynamodb_table = "turaf-terraform-locks"
  }
}
