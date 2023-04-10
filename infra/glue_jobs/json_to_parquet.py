# setup based on this: https://t-redactyl.io/blog/2020/08/reading-s3-data-into-a-spark-dataframe-using-sagemaker.html
import boto3
import json
import time
import pandas as pd
from pyspark import SparkConf
from pyspark.context import SparkContext
from pyspark.sql import SparkSession
from pyspark.sql.types import *
from pyspark.sql.functions import *
import matplotlib.pyplot as plt
import botocore.session

sc = SparkContext('local')
spark = SparkSession(sc)

# Set Spark Session Configuration
session = botocore.session.get_session()
credentials = session.get_credentials()
client = boto3.client('secretsmanager')
response = client.get_secret_value(
    SecretId='sapient-s3-access'
)

response = json.loads(response['SecretString'])
access_key = response["aws_access_key_id"]
secret_key = response["aws_secret_access_key"]
# conf = (SparkConf()
#         .set("spark.driver.extraClassPath", ":".join(spark.classpath_jars()))
#       )


# https://spark.apache.org/docs/latest/configuration.html#memory-management
spark = (
    SparkSession
        .builder
        .config('fs.s3a.access.key', access_key)
        .config('fs.s3a.secret.key', secret_key)
        .appName("sapient")
        .getOrCreate()
)
# spark.sparkContext.setLogLevel("ERROR")

# read from raw bucket + write to refined bucket + aggregate final to the trusted bucket
s3_url_raw = "s3a://sapient-bucket-raw/"
s3_url_refined = "s3a://sapient-bucket-refined/"
s3_url_trusted = "s3a://sapient-bucket-trusted/"
bro_cols_conn = ['ts', 'uid', 'id.orig_h', 'id.orig_p', 'id.resp_', 'id.resp_p', 'proto', 'service', 'duration',
                 'orig_bytes', 'resp_bytes', 'conn_state',
                 'local_orig', 'local_resp', 'missed_bytes', 'history', 'orig_pkts', 'orig_ip_bytes', 'resp_pkts',
                 'resp_ip_bytes', 'tunnel_parents']
bro_cols_rep = ['ts', 'level', 'message', 'location']
# schemas to reduce read times for spark
bro_schema = StructType([
    StructField('ts', StringType(), True),
    StructField('uid', StringType(), True),
    StructField('id.orig_h', StringType(), True),
    StructField('id.orig_p', IntegerType(), True),
    StructField('id.resp_', StringType(), True),
    StructField('id.resp_p', IntegerType(), True),
    StructField('proto', StringType(), True),
    StructField('service', StringType(), True),
    StructField('duration', StringType(), True),
    StructField('orig_bytes', IntegerType(), True),
    StructField('resp_bytes', IntegerType(), True),
    StructField('conn_state', StringType(), True),
    StructField('local_orig', StringType(), True),
    StructField('local_resp', StringType(), True),
    StructField('missed_bytes', IntegerType(), True),
    StructField('history', StringType(), True),
    StructField('orig_pkts', IntegerType(), True),
    StructField('orig_ip_bytes', IntegerType(), True),
    StructField('resp_pkts', IntegerType(), True),
    StructField('resp_ip_bytes', IntegerType(), True),
    StructField('tunnel_parents', StringType(), True)
])

ecar_bro_schema = StructType([
    StructField('action', StringType(), True),
    StructField('actorID', StringType(), True),
    StructField('hostname', StringType(), True),
    StructField('id', StringType(), True),
    StructField('object', StringType(), True),
    StructField('objectID', StringType(), True),
    StructField('pid', IntegerType(), True),
    StructField('ppid', IntegerType(), True),
    StructField('principal', StringType(), True),
    StructField('properties', StructType([
        StructField('acuity_level', StringType(), True),
        StructField('bro_uid', StringType(), True),
        StructField('dest_ip', StringType(), True),
        StructField('dest_port', IntegerType(), True),
        StructField('direction', StringType(), True),
        StructField('image_path', StringType(), True),
        StructField('l4protocol', StringType(), True),
        StructField('src_ip', StringType(), True),
        StructField('src_port', IntegerType(), True),
    ])),
    StructField('tid', IntegerType(), True),
    StructField('timestamp', TimestampType(), True)
])

ecar_schema = StructType([
    StructField('action', StringType(), True),
    StructField('actorID', StringType(), True),
    StructField('hostname', StringType(), True),
    StructField('id', StringType(), True),
    StructField('object', StringType(), True),
    StructField('objectID', StringType(), True),
    StructField('pid', IntegerType(), True),
    StructField('ppid', IntegerType(), True),
    StructField('principal', StringType(), True),
    StructField('properties', StructType([
        StructField('acuity_level', StringType(), True),
        StructField('base_address', StringType(), True),
        StructField('command_line', StringType(), True),
        StructField('context_info', StringType(), True),
        StructField('data', StringType(), True),
        StructField('dest_port', IntegerType(), True),
        StructField('direction', StringType(), True),
        StructField('end_time', TimestampType(), True),
        StructField('file_path', StringType(), True),
        StructField('image_path', StringType(), True),
        StructField('info_class', StringType(), True),
        StructField('key', StringType(), True),
        StructField('l4protocol', StringType(), True),
        StructField('logon_id', StringType(), True),
        StructField('module_path', StringType(), True),
        StructField('new_path', StringType(), True),
        StructField('parent_image_path', StringType(), True),
        StructField('path', StringType(), True),
        StructField('payload', StringType(), True),
        StructField('privileges', StringType(), True),
        StructField('requesting_domain', StringType(), True),
        StructField('requesting_logon_id', StringType(), True),
        StructField('requesting_user', StringType(), True),
        StructField('sid', StringType(), True),
        StructField('size', StringType(), True),
        StructField('src_ip', StringType(), True),
        StructField('src_pid', IntegerType(), True),
        StructField('src_port', IntegerType(), True),
        StructField('src_tid', StringType(), True),
        StructField('stack_base', StringType(), True),
        StructField('stack_limit', StringType(), True),
        StructField('start_address', StringType(), True),
        StructField('start_time', TimestampType(), True),
        StructField('subprocess_tag', StringType(), True),
        StructField('task_name', StringType(), True),
        StructField('task_pid', IntegerType(), True),
        StructField('task_process_uuid', StringType(), True),
        StructField('tgt_pid', IntegerType(), True),
        StructField('tgt_pid_uuid', StringType(), True),
        StructField('tgt_tid', IntegerType(), True),
        StructField('type', StringType(), True),
        StructField('user', StringType(), True),
        StructField('user_name', StringType(), True),
        StructField('user_stack_base', StringType(), True),
        StructField('user_stack_limit', StringType(), True),
        StructField('value', StringType(), True)
    ])),
    StructField('tid', IntegerType(), True),
    StructField('timestamp', TimestampType(), True)
])


def get_ecar_files():
    env = 'prod'
    type = 'car'
    paths = ['23Sep19-red',
             '24Sep19',
             '25Sep'
             ]
    s3 = boto3.client('s3')
    response = s3.list_objects_v2(
        Bucket='sapient-bucket-raw',
        Prefix=f'{env}/')

    all_files = []
    for p in paths:
        files = []
        for content in response.get('Contents', []):
            files.append(content['Key'])
        files = [f"{s3_url_raw}/" + f for f in files if p in f]
        all_files = all_files + files
    return all_files


def loadAndCheckpoint(type='ecar-bro', env='prod', size='small'):
    """
    type: ecar, ecar-bro, bro, labels
    This function reads a file from json or log text and writes it as a parquet.
    """
    ecar_fil = [("FLOW"), ("PROCESS"), ("FILE"), ("SHELL")]
    if size == 'small':
        # 1 million
        read_lim = 1000000
    elif size == 'medium':
        # 100 million
        read_lim = 100000000
    elif size == 'large':
        # 1 billion
        read_lim = 1000000000
    elif size == 'all':
        # 1 billion
        read_lim = 'all'
    start_time = time.time()
    if type in ('ecar', 'car'):
        if env == 'prod':
            ecar_files = get_ecar_files()
            df = spark.read.schema(ecar_schema).json(ecar_files).filter(col("object").isin(ecar_fil))
        else:
            df = spark.read.schema(ecar_schema).json(f"{s3_url_raw}/{env}/{type}/**/**/**/*.json").filter(
                col("object").isin(ecar_fil))
        if size == 'all':
            pass
        else:
            df = df.limit(read_lim)
        print(
            time.strftime('%l:%M%p %Z on %b %d, %Y') + " -- read time: --- %s seconds ---" % (time.time() - start_time))
        print(time.strftime('%l:%M%p %Z on %b %d, %Y') + f" -- Your new dataframe has {df.count():,} rows.")
        start_time = time.time()
        df = df.select(*df.columns, "properties.*").drop('properties')
        df = df.withColumn('event_minute', minute(col('timestamp'))) \
            .withColumn('event_day', dayofmonth(col('timestamp'))) \
            .withColumn('event_hour', hour(col('timestamp')))
        print(time.strftime('%l:%M%p %Z on %b %d, %Y') + " -- schema expansion time: --- %s seconds ---" % (
                time.time() - start_time))
        start_time = time.time()
        # numPartitions = 16000
        # df = df.repartition(numPartitions)
        df.write.option("maxRecordsPerFile", 300000).mode("overwrite").partitionBy("event_day", "event_hour",
                                                                                   "event_minute").parquet(
            f"{s3_url_refined}/{env}/ecar/{size}")
    elif type in ('ecar-bro', 'car-bro'):
        df = spark.read.schema(ecar_bro_schema).json(f"{s3_url_raw}/{env}/{type}/**/**/**/*.json")
        # this will extract and flatten nested properties column
        if size == 'all':
            pass
        else:
            df = df.limit(read_lim)
        print(
            time.strftime('%l:%M%p %Z on %b %d, %Y') + " -- read time: --- %s seconds ---" % (time.time() - start_time))
        print(time.strftime('%l:%M%p %Z on %b %d, %Y') + f" -- Your new dataframe has {df.count():,} rows.")
        start_time = time.time()
        df = df.select(*df.columns, "properties.*").drop('properties')
        df = df.withColumn('event_minute', minute(col('timestamp'))) \
            .withColumn('event_day', dayofmonth(col('timestamp'))) \
            .withColumn('event_hour', hour(col('timestamp')))
        df.write.option("maxRecordsPerFile", 300000).mode("overwrite").partitionBy("event_day", "event_hour").parquet(
            f"{s3_url_refined}/{env}/ecar-bro/{size}")
    elif type == 'bro':
        df = spark.read.schema(bro_schema).csv(f"{s3_url_raw}/{env}/**/**/conn*.log", sep="\t", comment="#",
                                               header=False)
        if size == 'all':
            pass
        else:
            df = df.limit(read_lim)
        df = df.toDF(*bro_cols_conn)
        print(
            time.strftime('%l:%M%p %Z on %b %d, %Y') + " -- read time: --- %s seconds ---" % (time.time() - start_time))
        print(time.strftime('%l:%M%p %Z on %b %d, %Y') + f" -- Your new dataframe has {df.count():,} rows.")
        start_time = time.time()
        df.write.option("maxRecordsPerFile", 300000).mode("overwrite").parquet(f"{s3_url_refined}/{env}/bro/{size}")
    elif type == 'labels':
        df = spark.read.csv(f"{s3_url_raw}/{type}/*.csv", sep=",", header=True)
        df.write.option("maxRecordsPerFile", 300000).mode("overwrite").parquet(f"{s3_url_refined}/{env}/labels")
    print(time.strftime('%l:%M%p %Z on %b %d, %Y') + " -- write time: --- %s seconds ---" % (time.time() - start_time))
    df.unpersist()


env = 'prod'
size = 'small'
# this is here because the prod ecar/ecar-bro are missing the "e"s and is a short term fix
if env == 'prod':
    ftype = 'car'
else:
    ftype = 'ecar'

loadAndCheckpoint('labels', env=env)
loadAndCheckpoint(f'{ftype}-bro', env=env, size=size)

# if __name__ == "__main__":

#     env='prod'
#     size='small'
#     # this is here because the prod ecar/ecar-bro are missing the "e"s and is a short term fix
#     if env == 'prod':
#         ftype = 'car'
#     else:
#         ftype = 'ecar'

#     loadAndCheckpoint('labels', env=env)
# loadAndCheckpoint(f'{ftype}-bro', env=env, size=size)
# loadAndCheckpoint(f'{ftype}', env=env, size=size)

# size='medium'
# loadAndCheckpoint(f'{ftype}-bro', env=env, size=size)
# loadAndCheckpoint(f'{ftype}', env=env, size=size)

