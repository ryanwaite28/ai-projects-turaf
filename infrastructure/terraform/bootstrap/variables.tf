variable "environment" {
  description = "Environment name (dev, qa, prod)"
  type        = string
}

variable "region" {
  description = "AWS region"
  type        = string
  default     = "us-east-1"
}
