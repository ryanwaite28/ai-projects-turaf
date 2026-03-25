terraform {
  backend "s3" {
    bucket         = "turaf-terraform-state-ops"
    key            = "ops/terraform.tfstate"
    region         = "us-east-1"
    dynamodb_table = "turaf-terraform-locks"
    encrypt        = true
  }
}
