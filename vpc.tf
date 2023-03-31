# want to restrict security groups to only internal ip ranges and allow only necessary protocols

locals {
  zones = {
    "a" = 0
    "b" = 16
    "c" = 32
    "d" = 48
  }
}

resource "aws_subnet" "sapient-main" {
  vpc_id     = aws_vpc.sapient_vpc.id
  cidr_block = "168.31.0.0/20"

  tags = {
    name = "sapient-main"
  }
}

resource "aws_vpc" "sapient_vpc" {
  cidr_block           = "168.31.0.0/16"
  enable_dns_support   = true
  enable_dns_hostnames = true
  tags = {
    Name = "sapient_vpc"
  }
}

resource "aws_route_table" "sapient-igw-rt" {
  vpc_id = aws_vpc.sapient_vpc.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.sapient_igw.id
  }

  tags = {
    Name = "sapient-igw-rt"
  }
}

resource "aws_main_route_table_association" "sapient_rt_assoc" {
  vpc_id         = aws_vpc.sapient_vpc.id
  route_table_id = aws_route_table.sapient-igw-rt.id
}

resource "aws_internet_gateway" "sapient_igw" {
  vpc_id = aws_vpc.sapient_vpc.id

  tags = {
    Name = "sapient_igw"
  }
}

resource "aws_route" "sapient_igw_r" {
  route_table_id         = aws_route_table.sapient-igw-rt.id
  destination_cidr_block = "0.0.0.0/0"
  gateway_id             = aws_internet_gateway.sapient_igw.id
}

resource "aws_security_group" "allow_tls" {
  name        = "allow_tls"
  description = "Allow TLS inbound traffic"
  vpc_id      = aws_vpc.sapient_vpc.id

  egress {
    from_port        = 0
    to_port          = 0
    protocol         = "-1"
    cidr_blocks      = ["0.0.0.0/0"]
    ipv6_cidr_blocks = ["::/0"]
  }

  tags = {
    Name = "allow_tls"
  }
}