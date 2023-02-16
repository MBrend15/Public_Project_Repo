import gdown

# 24Sep19
# https://drive.google.com/drive/folders/1rLQe2opkCsp7s1K41HtZ6WjU6eavanxU

# 25Sep
# https://drive.google.com/drive/folders/1SltTXt8r9jkuTQEW725Tcp9xUUJVbCTn?usp=share_link

file_path = '24Sep19'
url = "https://drive.google.com/drive/folders/1rLQe2opkCsp7s1K41HtZ6WjU6eavanxU"
output = f"/home/ec2-user/SageMaker/ecar/evaluation/{file_path}"
gdown.download_folder(url=url, quiet=False, use_cookies=False)