resource "aws_sagemaker_notebook_instance" "small-instance-eda" {
  name                = "sapient-instance"
  instance_type       = "ml.t2.medium"
  platform_identifier = "notebook-al2-v2"
  role_arn            = aws_iam_role.sapient-sagemaker-role.arn
  kms_key_id          = aws_kms_key.sapient-sagemaker-key.id

}