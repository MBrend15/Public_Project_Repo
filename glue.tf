resource "aws_glue_job" "json-to-parquet" {
  for_each = fileset("./glue_jobs/", "*")
  name     = replace(each.key, ".py", "")
  role_arn = aws_iam_role.sapient-glue-role.arn
  glue_version = "3.0"

  command {
    script_location = "s3://sapient-bucket-scripts/${each.key}"
  }

  default_arguments = {
    # ... potentially other arguments ...
    "--continuous-log-logGroup"          = aws_cloudwatch_log_group.glue-logging.name
    "--enable-continuous-cloudwatch-log" = "true"
    "--enable-continuous-log-filter"     = "true"
    "--enable-metrics"                   = ""
  }
}
