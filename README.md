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
#### Honestly think we just need to download them individually
#### Loading the data to S3
#### Using the UI - this can fail multiple times even for a single file 
#### Using aws cli
#### Example:
```
aws s3 cp ~/Downloads/bro s3://sapient-bucket-raw/prod/bro --recursive
```


### Labels for the malicious data
### Reading the data
#### bro/date/file
#### ecar/benign/date/file
#### ecar-bro/benign/date/file
#### Reading log data
### Filtering the data
#### 29 computers were attacked
#### attacked occurred on 3 days
### Joining the data
(bro) uid + (ecar_bro) bro_uid + (ecar) id