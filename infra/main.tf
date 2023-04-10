provider "aws" {
  profile = var.profile
  region  = var.region

  default_tags {
    tags = {
      Environment = "Production"
      Product     = "sapient"
    }
  }
}

terraform {
  backend "s3" {
    bucket = "sapient-infra-terraform"
    key    = "ce-terraform"
    region = "us-west-2"
  }
}