terraform {
  backend "s3" {
    bucket         = "turaf-terraform-state-qa"
    key            = "qa/terraform.tfstate"
    region         = "us-east-1"
    dynamodb_table = "turaf-terraform-locks"
    encrypt        = true
  }
}
