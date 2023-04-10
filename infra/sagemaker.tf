resource "aws_sagemaker_domain" "sapient-domain" {
  domain_name = "sapient-domain"
  auth_mode   = "IAM"
  vpc_id      = aws_vpc.sapient_vpc.id
  subnet_ids  = [aws_subnet.sapient-main.id]

  default_user_settings {
    execution_role = aws_iam_role.sapient-sagemaker-role.arn
  }
}


resource "aws_sagemaker_notebook_instance" "small-instance-eda" {
  name                = "sapient-16cpu-64Gib"
  instance_type       = "ml.m5.4xlarge"
  platform_identifier = "notebook-al2-v2"
  role_arn            = aws_iam_role.sapient-sagemaker-role.arn
  kms_key_id          = aws_kms_key.sapient-sagemaker-key.id
  volume_size         = 600

}

resource "aws_sagemaker_notebook_instance" "medium-instance-processing" {
  name                = "sapient-32cpu-128Gib"
  instance_type       = "ml.m5d.8xlarge"
  platform_identifier = "notebook-al2-v2"
  role_arn            = aws_iam_role.sapient-sagemaker-role.arn
  kms_key_id          = aws_kms_key.sapient-sagemaker-key.id
  volume_size         = 600

}

resource "aws_sagemaker_notebook_instance" "gpu-instance" {
  name                = "sapient-GPU-8cpu-61Gib"
  instance_type       = "ml.p3.2xlarge"
  role_arn            = aws_iam_role.sapient-sagemaker-role.arn
  kms_key_id          = aws_kms_key.sapient-sagemaker-key.id
  volume_size         = 300

}
