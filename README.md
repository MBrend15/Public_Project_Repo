### This provides the infrastructure and the notebooks including eda and models for the sapient project
###  which models Advanced Persistent Threats (ATPs)

###  For the infrastructure piece, the setup requirements include.
###  terraform + aws credentials in ~/.aws/credentials

###  Getting terraform: https://developer.hashicorp.com/terraform/tutorials/aws-get-started/install-cli#install-terraform

###  For jupyter notebooks, we leverage git
###  That setup includes adding git public ssh key in your git profile within the jupyter terminal

```
vi ~/.ssh/config
```

```
vi ~/.ssh/id_rsa
```

###  you will also need to update file and directory permissions. This might look like:

```
chmod 400 ~/.ssh/*
chmod 700 ~/.ssh
```

### Other code on Darpa dataset:
### https://github.com/SparkyAndy/XCS229ii-FinalProject/blob/main/ClassifyingComputerProcessesDarpaOpTCBaselineModel.ipynb

### Transferring large files to S3. 
#### Downloading the data:
#### This is done in two ways:
#### 1. Manually through the google drive
#### run update gdrive_download.py file with sharing link for url's from folders in google drive
#### 2. If that fails from limits, you can manually download through the google drive
#### This can fail multiple times even for a single file. Take note. Those that fail can be manually downloaded after. 

### Once you have the files downloaded, you'll want to upload them to S3. You can using aws cli. 
#### 1. Notice bro is moving to bro. This maintains the file structure. 
#### Example:
```
aws s3 cp /home/ec2-user/SageMaker/ecar/evaluation s3://sapient-bucket-raw/prod/ecar/evaluation --recursive
```

### Reading the data
#### bro/date/file
#### ecar/benign/date/file
#### ecar-bro/benign/date/file
#### Reading log data
### Filtering the data
all events filtered to -> process, flow, file, shell

icmp ping messages (maybe just ICMP protocol)
bi-directional flow messages 
tcp three way -> for filtering, how many services are there in bro connections, might be able to filter out when proto and service are both tcp 


### Labels for the malicious data

#### 29 computers were attacked
#### attacked occurred on 3 days
### Joining the data
(bro) uid + (ecar_bro) bro_uid + (ecar) id


Provenance Graphs (DAG)
Event poison distribution
Model Type - Bayesian Neural Network
Generative AI for DAG - Generative adverserial network
Pagerank?


