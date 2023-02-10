data "aws_iam_policy_document" "kms-key-policy-doc" {

  statement {
    sid = "Enable IAM User Permissions"
    actions = [
      "kms:*"
    ]

    effect = "Allow"

    resources = [
      "*",
    ]

    principals {
      type        = "AWS"
      identifiers = ["arn:aws:iam::756727001009:root"]
    }

  }
}

data "aws_iam_policy_document" "sagemaker-assume-role-policy-doc" {

  statement {
    actions = ["sts:AssumeRole"]

    effect = "Allow"
    principals {
      type        = "Service"
      identifiers = ["sagemaker.amazonaws.com"]
    }

  }
}


resource "aws_iam_role" "sapient-sagemaker-role" {
  name               = "sapient-sagemaker-role"
  assume_role_policy = data.aws_iam_policy_document.sagemaker-assume-role-policy-doc.json

  inline_policy {
    name   = "sapient-inline-policy"
    policy = data.aws_iam_policy_document.sapient-sagemaker-inline-policy-doc.json
  }
}

data "aws_iam_policy_document" "sapient-sagemaker-inline-policy-doc" {

  statement {
    actions = [
      "glue:*",
      "ec2:DescribeVpcEndpoints",
      "ec2:DescribeRouteTables",
      "ec2:CreateNetworkInterface",
      "ec2:DeleteNetworkInterface",
      "ec2:DescribeNetworkInterfaces",
      "ec2:DescribeSecurityGroups",
      "ec2:DescribeSubnets",
      "ec2:DescribeVpcAttribute",
      "cloudwatch:PutMetricData"
    ]
    effect = "Allow"
    resources = [
      "*",
    ]
  }

  statement {
    actions = [
      "iam:ListRolePolicies",
      "iam:GetRole",
      "iam:GetRolePolicy"
    ]
    effect = "Allow"
    resources = [
      "*",
    ]
  }

  statement {
    actions = [
      "s3:CreateBucket",
      "s3:PutBucketPublicAccessBlock"
    ]
    effect = "Allow"
    resources = [
      "arn:aws:s3:::aws-glue-*",
    ]
  }

  statement {
    actions = [
      "s3:ListBucket"
    ]
    effect = "Allow"
    resources = [
      "arn:aws:s3:::${local.bucket_prefix}-*"
    ]
  }

  statement {
    actions = [
      "s3:GetObject",
      "s3:PutObject",
      "s3:DeleteObject"
    ]
    effect = "Allow"
    resources = [
      "arn:aws:s3:::aws-glue-*/*",
      "arn:aws:s3:::*/*aws-glue-*/*",
      "arn:aws:s3:::${local.bucket_prefix}-*/*"
    ]
  }

  statement {
    actions = [
      "s3:GetObject"
    ]
    effect = "Allow"
    resources = [
      "arn:aws:s3:::crawler-public*",
      "arn:aws:s3:::aws-glue-*",
    ]
  }

  statement {
    actions = [
      "logs:CreateLogGroup",
      "logs:CreateLogStream",
      "logs:PutLogEvents",
      "logs:AssociateKmsKey"
    ]
    effect = "Allow"
    resources = [
      "arn:aws:logs:*:*:/aws-glue/*"
    ]
  }

  statement {
    effect = "Allow"
    actions = [
      "ec2:CreateTags",
      "ec2:DeleteTags"
    ]
    condition {
      test     = "ForAllValues:StringLike"
      variable = "aws:TagKeys"

      values = [
        "aws-glue-service-resource"
      ]
    }
    resources = [
      "arn:aws:ec2:*:*:network-interface/*",
      "arn:aws:ec2:*:*:security-group/*",
      "arn:aws:ec2:*:*:instance/*"
    ]
  }
}