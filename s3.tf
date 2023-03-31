locals {
  bucket_prefix = "sapient-bucket"
  bucket_stages = {
    "raw"     = { environment = "Prod" }
    "trusted" = { environment = "Prod" }
    "refined" = { environment = "Prod" }
    "studio" = { environment = "Prod" }
  }
}


resource "aws_s3_bucket" "sapient-buckets" {
  for_each = local.bucket_stages

  bucket = "${local.bucket_prefix}-${each.key}"

  tags = {
    Name = local.bucket_prefix
  }
}

resource "aws_s3_bucket_acl" "sapient-bucket-acl" {
  for_each = local.bucket_stages

  bucket = aws_s3_bucket.sapient-buckets[each.key].id
  acl    = "private"
}

#block public access
resource "aws_s3_bucket_public_access_block" "aws-public-access-config" {
  for_each = local.bucket_stages

  bucket = aws_s3_bucket.sapient-buckets[each.key].id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}


resource "aws_s3_bucket_cors_configuration" "aws-cors-config" {
  for_each = local.bucket_stages

  bucket = aws_s3_bucket.sapient-buckets[each.key].bucket

  cors_rule {
    allowed_headers = ["*"]
    allowed_methods = ["PUT", "POST"]
    allowed_origins = ["*"]
    max_age_seconds = 3000
  }

  cors_rule {
    allowed_methods = ["GET"]
    allowed_origins = ["*"]
  }
}

# Encryption
resource "aws_s3_bucket_server_side_encryption_configuration" "aws-s3-bucket-config" {
  for_each = local.bucket_stages

  bucket = aws_s3_bucket.sapient-buckets[each.key].bucket

  rule {
    apply_server_side_encryption_by_default {
      kms_master_key_id = aws_kms_key.sapient-bucket-key.arn
      sse_algorithm     = "aws:kms"
    }
  }
}

