import gdown

# 24Sep19
# https://drive.google.com/drive/folders/1rLQe2opkCsp7s1K41HtZ6WjU6eavanxU

# 24Sep19/AIA-601-625
# https://drive.google.com/drive/folders/16jlNFJk8tWIyjGhd9aDV6hsv5qEafwXe?usp=share_link

drive_paths = {
'24Sep19/AIA-651-675':
'https://drive.google.com/drive/folders/1-nm2lE7t4kvAbjhTfdE9dXWNgQ81vZNX',
 
'24Sep19/AIA-701-725':
'https://drive.google.com/drive/folders/1bmUaBKR_Bz7ph_5-PfLD4shYGo_oW2dz',

'24Sep19/AIA-751-775':
'https://drive.google.com/drive/folders/1oDq-eNoxkm8jj2kPHD_GHZymu28YrAzz',

'24Sep19/AIA-801-825':
'https://drive.google.com/drive/folders/1TfvCuGNCKDQRSCir3qKvZxTFnzGo58ht',

'24Sep19/AIA-851-875':
'https://drive.google.com/drive/folders/1Jm3yAGsQOgMji1FrY3mvTKh-7IgSyyxF',

'24Sep19/AIA-901-925':
'https://drive.google.com/drive/folders/1RTTJmO7Vg1-lyZ0ok5QGfVymKS8kqCyW',

'24Sep19/AIA-951-975':
'https://drive.google.com/drive/folders/1WgBWH6QWYXGU0OgmPxnOzSaZ5wZ5sYiJ'
    
}

# 25Sep
# https://drive.google.com/drive/folders/1SltTXt8r9jkuTQEW725Tcp9xUUJVbCTn

for file_path,url in drive_paths.items():
    # file_path = '24Sep19/AIA-651-675'
    # url = "https://drive.google.com/drive/folders/1-nm2lE7t4kvAbjhTfdE9dXWNgQ81vZNX"
    output = f"/home/ec2-user/SageMaker/ecar/evaluation/{file_path}"
    gdown.download_folder(url=url, quiet=False, use_cookies=False)