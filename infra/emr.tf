//resource "aws_emr_studio" "sapient-emr-studio" {
//  auth_mode                   = "IAM"
//  default_s3_location         = "s3://sapient-bucket-studio"
//  engine_security_group_id    = aws_security_group.allow_tls.id
//  name                        = "sapient-emr-studio"
//  service_role                = aws_iam_role.sapient_emr_role.arn
//  subnet_ids                  = [aws_subnet.sapient-main.id]
//  vpc_id                      = aws_vpc.sapient_vpc.id
//  workspace_security_group_id = aws_security_group.allow_tls.id
//}

