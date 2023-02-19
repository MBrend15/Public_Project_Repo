This project provides the infrastructure and the notebooks including eda and models for the sapient project 
which models Advanced Persistent Threats (ATPs)
For the infrastructure piece, the setup requirements include.
terraform + aws credentials in ~/.aws/credentials
Getting terraform: [Terraform installation](https://developer.hashicorp.com/terraform/tutorials/aws-get-started/install-cli#install-terraform)

For jupyter notebooks, we leverage git
That setup includes adding git public ssh key in your git profile within the jupyter terminal.


### Git credentials
You may only need the private key. In case you want to specify it by a different name than id_rsa, you can also add a config file. 
Setting up keys: [Github ssh keys](https://docs.github.com/en/authentication/connecting-to-github-with-ssh/generating-a-new-ssh-key-and-adding-it-to-the-ssh-agent)
```
vi ~/.ssh/config
```

```
vi ~/.ssh/id_rsa
```
You will also need to update file and directory permissions. This might look like:
```
chmod 400 ~/.ssh/*
chmod 700 ~/.ssh
```

### Downloading the data:

This is done in two ways:
1. Manually through the google drive
2. run `gdrive_download.py`
If (when) that fails from limits, update the link list to continue. Bro and Ecar-bro will can be downloaded completely. 
Ecar will need to be split into smaller parts
Once you have the files downloaded, you'll want to upload them to S3. You can using aws cli. 
Notice bro is moving to bro. This maintains the file structure. 
Example:

```
aws s3 cp /home/ec2-user/SageMaker/ecar/evaluation/24Sep19 s3://sapient-bucket-raw/pre_prod/ecar/evaluation --recursive
```

### Expansion of the files
This was done by download each file from S3 and expanding it, then pushing it back to S3. You can run this command to complete this. 
This command allows the expansion to run as a process in the background log to file. You can view the file to see if any files failed and locate which.
```
nohup python -u expand_files.py > expand_files.log &
```

### Processing the data
all events filtered to -> process, flow, file, shell

icmp ping messages (maybe just ICMP protocol)
bi-directional flow messages 
tcp three way -> for filtering, how many services are there in bro connections, might be able to filter out when proto and service are both tcp 

### Labels for the malicious data
A prior project that using the Darpa OpTC dataset contains labeled malicious data: [Darpa project](https://github.com/SparkyAndy/XCS229ii-FinalProject)

(bro) uid + (ecar_bro) bro_uid + (ecar) id


### Proposed methods of analysis
- Provenance Graphs (DAG)
- Event poison distribution
- Model Type - Bayesian Neural Network
- Generative AI for DAG, Generative adverserial network
- Pagerank as a feature
- Few Shot AI to use a little data
- Visualization of Attack in Network in Tableau as Report


