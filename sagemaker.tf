resource "aws_sagemaker_notebook_instance" "small-instance-eda" {
  name                = "sapient-instance-small-eda"
  instance_type       = "ml.m5.xlarge"
  platform_identifier = "notebook-al2-v2"
  role_arn            = aws_iam_role.sapient-sagemaker-role.arn
  kms_key_id          = aws_kms_key.sapient-sagemaker-key.id
  volume_size         = 60

}

resource "aws_sagemaker_notebook_instance" "medium-instance-data" {
  name                = "sapient-instance-medium-data-processing"
  instance_type       = "ml.c5.9xlarge"
  platform_identifier = "notebook-al2-v2"
  role_arn            = aws_iam_role.sapient-sagemaker-role.arn
  kms_key_id          = aws_kms_key.sapient-sagemaker-key.id
  volume_size         = 100

}
