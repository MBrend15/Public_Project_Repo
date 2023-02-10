# Encryption
resource "aws_kms_key" "sapient-bucket-key" {
  description             = "This key is used to encrypt sapient bucket objects"
  deletion_window_in_days = 10
  policy                  = data.aws_iam_policy_document.kms-key-policy-doc.json
}

resource "aws_kms_alias" "sapient-bucket-key-alias" {
  name          = "alias/sapient-bucket-key"
  target_key_id = aws_kms_key.sapient-bucket-key.key_id
}

resource "aws_kms_key" "sapient-sagemaker-key" {
  description             = "This key is used to encrypt sapient sagemaker objects"
  deletion_window_in_days = 10
  policy                  = data.aws_iam_policy_document.kms-key-policy-doc.json
}

resource "aws_kms_alias" "sapient-sagemaker-key-alias" {
  name          = "alias/sapient-sagemaker-key"
  target_key_id = aws_kms_key.sapient-sagemaker-key.key_id
}