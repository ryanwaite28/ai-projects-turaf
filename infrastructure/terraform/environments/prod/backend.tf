terraform {
  backend "s3" {
    bucket         = "turaf-terraform-state-prod"
    key            = "prod/terraform.tfstate"
    region         = "us-east-1"
    dynamodb_table = "turaf-terraform-locks"
    encrypt        = true
  }
}
