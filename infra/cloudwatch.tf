resource "aws_cloudwatch_log_group" "glue-logging" {
  name              = "/aws-glue/jobs/"
  retention_in_days = 3
  kms_key_id        = aws_kms_key.cloudwatch-key.arn
}