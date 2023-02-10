# This provides the infrastructure and the notebooks including eda and models for the sapient project
# which models Advanced Persistent Threats (ATPs)

# For the infrastructure piece, the setup requirements include.
# terraform + aws credentials in ~/.aws/credentials

# Getting terraform: https://developer.hashicorp.com/terraform/tutorials/aws-get-started/install-cli#install-terraform

# For jupyter notebooks, we leverage git
# That setup includes adding git public ssh key in your git profile within the jupyter terminal

# you will also need to update file and directory permissions. This might look like:

```
chmod 400 ~/.ssh/id_rsa.pub
chmod 700 ~/.ssh
chmod 600 ~/.ssh/*
```
