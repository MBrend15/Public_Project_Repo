# Encryption
resource "aws_kms_key" "sapient-bucket-key" {
  description             = "This key is used to encrypt bucket objects"
  deletion_window_in_days = 10
  policy                  = data.aws_iam_policy_document.kms-key-policy-doc.json
}

resource "aws_kms_alias" "analytics-bucket-key-alias" {
  name          = "alias/analytics-bucket-key"
  target_key_id = aws_kms_key.sapient-bucket-key.key_id
}