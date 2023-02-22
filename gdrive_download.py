import gdown

# 24Sep19
# https://drive.google.com/drive/folders/1rLQe2opkCsp7s1K41HtZ6WjU6eavanxU

drive_paths = {
'/benign/20-23Sep19':
'https://drive.google.com/drive/folders/1CIcDZwY6w52SdrIZsbuQxyqLmPMqNRMc', 
}

# Missing from benign/20-23Sep19, 451 (85, 97, last) through 951
# ecar/benign/20-23Sep19/AIA-151-175/AIA-151-175.ecar-2019-12-08T11-48-21.825.json.gz 
    
for file_path, url in drive_paths.items():
    # file_path = '24Sep19/AIA-651-675'
    # url = "https://drive.google.com/drive/folders/1-nm2lE7t4kvAbjhTfdE9dXWNgQ81vZNX"
    output = f"/home/ec2-user/SageMaker/ecar/{file_path}"
    gdown.download_folder(url=url, output=output, quiet=False, use_cookies=False)
