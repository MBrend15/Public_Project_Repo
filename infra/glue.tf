resource "aws_glue_job" "json-to-parquet" {
  for_each          = fileset("./glue_jobs/", "*")
  name              = replace(each.key, ".py", "")
  role_arn          = aws_iam_role.sapient-glue-role.arn
  glue_version      = "3.0"
  worker_type       = "G.1X"
  number_of_workers = 20
  timeout           = 240

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

resource "aws_glue_crawler" "sapient-ecar-crawler" {
  database_name = aws_glue_catalog_database.sapient-ecar-db.name
  name          = "sapient-ecar-crawler"
  role          = aws_iam_role.sapient-glue-role.arn

  s3_target {
    path        = "s3://sapient-bucket-refined/prod/ecar/"
    sample_size = 1
  }
}

resource "aws_glue_catalog_database" "sapient-ecar-db" {
  name = "sapient-ecar-db"
}
