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