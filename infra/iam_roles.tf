resource "aws_iam_role" "sapient-sagemaker-role" {
  name               = "sapient-sagemaker-role"
  assume_role_policy = data.aws_iam_policy_document.sagemaker-assume-role-policy-doc.json

  inline_policy {
    name   = "sapient-inline-policy"
    policy = data.aws_iam_policy_document.sapient-sagemaker-inline-policy-doc.json
  }
}

resource "aws_iam_role" "sapient-glue-role" {
  name               = "sapient-glue-role"
  assume_role_policy = data.aws_iam_policy_document.glue-assume-role-policy-doc.json

}

resource "aws_iam_role_policy" "sapient-glue-role-policy" {
  name = "sapient-glue-role-policy"
  role = aws_iam_role.sapient-glue-role.id

  policy = data.aws_iam_policy_document.sapient-glue-role-policy-doc.json
}

resource "aws_iam_role" "sapient_emr_role" {
  name               = "sapient-emr-service-role"
  assume_role_policy = data.aws_iam_policy_document.emr-service-role-policy-doc.json

  inline_policy {
    name   = "sapient-inline-policy"
    policy = data.aws_iam_policy_document.emr-service-role-inline-policy-doc.json
  }
}

# IAM Role for EC2 Instance Profile
resource "aws_iam_role" "sapient_emr_profile_role" {
  name = "sapient_emr_profile_role"

  assume_role_policy = <<EOF
{
  "Version": "2008-10-17",
  "Statement": [
    {
      "Sid": "",
      "Effect": "Allow",
      "Principal": {
        "Service": "ec2.amazonaws.com"
      },
      "Action": "sts:AssumeRole"
    }
  ]
}
EOF
}