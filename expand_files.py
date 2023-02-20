import io
import os
import time
import gzip
import boto3
import shutil
from boto3.s3.transfer import TransferConfig


file_pre = "pre_prod/"
file_prod = "prod"
log_type = "bro"
bucket = "sapient-bucket-raw"

config = TransferConfig(multipart_threshold=1024 * 300, 
                        max_concurrency=10,
                        multipart_chunksize=1024 * 300,
                        use_threads=True)


# https://medium.com/towards-data-engineering/get-keys-inside-an-s3-bucket-at-the-subfolder-level-7be42d858372
def get_matching_s3_objects(bucket, prefix="", suffix=""):
    """
    Generate objects in an S3 bucket.
    :param bucket: Name of the S3 bucket.
    :type bucket: str
    :param prefix: Only fetch objects whose key starts with this prefix (optional).
    :type prefix: tuple, list, str
    :param suffix: Only fetch objects whose keys end with this suffix (optional).
    :type suffix: str
    :return: None
    :rtype:
    """

    if isinstance(prefix, str):
        prefixes = (prefix, )
    else:
        prefixes = prefix

    s3 = boto3.resource('s3')
    my_bucket = s3.Bucket(bucket)
    
    count = 0
    files_list = []
    
    for key_prefix in prefixes:
        for object_summary in my_bucket.objects.filter(Prefix=key_prefix):
            key = object_summary.key
            if key.endswith(suffix):
                count += 1
                files_list.append(key)
    return count, files_list


# https://medium.com/analytics-vidhya/aws-s3-multipart-upload-download-using-boto3-python-sdk-2dedb0945f11
# https://stackoverflow.com/questions/48466421/python-how-to-decompress-a-gzip-file-to-an-uncompressed-file-on-disk
def expand_json_gz(bucket, key):
    ''' download gzipped json file from s3, expand, and send to s3 '''
    s3 = boto3.resource('s3')
    client = boto3.client('s3')
    tmp_loc = f'/home/ec2-user/SageMaker/tmp/{key}'
    new_dir = os.path.dirname(tmp_loc)
    start_time = time.time()
    try: 
        os.makedirs(new_dir)
    except:
        pass
    # download gz file
    response = s3.meta.client.download_file(Bucket=bucket, Key=key, Filename=tmp_loc)
    exp_loc = tmp_loc.replace(".gz", "")
    key_exp = key.lstrip(file_pre).replace(".gz", "")
    print(exp_loc)
    with gzip.open(tmp_loc, 'r') as f_in: 
        with open(exp_loc, 'wb') as f_out:
            try:
                shutil.copyfileobj(f_in, f_out)
                response = client.upload_file(
                                Filename = exp_loc,
                                Bucket=bucket,
                                Key= 'prod/'+ key_exp.lstrip(file_pre),
                                Config=config
        )
                print(time.strftime('%l:%M%p %Z on %b %d, %Y') + " -- file upload complete")
            except Exception as e:
                print(e)
                print(time.strftime('%l:%M%p %Z on %b %d, %Y') + " -- failed file: " + file_pre)
                pass
    try: 
        os.remove(tmp_loc)
        os.remove(exp_loc)
    except Exception as e:
        print(e)
    print("--- %s seconds ---" % (time.time() - start_time))



if __name__ == "__main__":
    
    # Single file test expansion
    # key = 'pre_prod/bro/2019-09-18/communication.07_00_00-08_00_00.log.gz'  # input for your key on S3 (means S3 object fullpath)
    # actual = expand_json_gz(bucketname, key)
    
    print(time.strftime('%l:%M%p %Z on %b %d, %Y') + " -- process start time")
    # ignore completed files previously logged
    infiles = [r"/home/ec2-user/SageMaker/sapient/expand_files.log", r"/home/ec2-user/SageMaker/sapient/expand_files.log1" ]
    corpus = []

    for infile in infiles:
        with open(infile) as f:
            f = f.readlines()
        corpus = corpus + f

    corpus = [*set(corpus)]
    completed = [x.replace("/home/ec2-user/SageMaker/tmp/", "").replace("\n", "") for x in corpus if ".json" in x]
    
    start_time = time.time()
    s3_count, s3_files = get_matching_s3_objects(bucket = bucket, prefix = file_pre + "ecar/", suffix="gz")
    s3_count = s3_count - len(completed)
    print(f"count of total objects is {s3_count}.")
    print(f"estimated time is " + str(round(5*s3_count/60/60, 0)) + " hours.")
    for f in s3_files:
        if f.replace(".gz", "") not in completed:
            print(f)
        expand_json_gz(bucket, f)
        s3_count -= 1
        print(time.strftime('%l:%M%p %Z on %b %d, %Y') + " -- There are " + str(s3_count) + " files remaining to convert")
    print(time.strftime('%l:%M%p %Z on %b %d, %Y') + " -- total time was  --- %s seconds ---" % (time.time() - start_time))
