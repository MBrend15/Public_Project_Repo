# want to restrict security groups to only internal ip ranges and allow only necessary protocols

locals {
  zones = {
    "a" = 0
    "b" = 16
    "c" = 32
    "d" = 48
  }
}

resource "aws_vpc" "sapient_vpc" {
  cidr_block           = "10.0.0.0/16"
  enable_dns_support   = true
  enable_dns_hostnames = true
  tags = {
    Name = "glue_vpc"
  }
}

resource "aws_route_table" "serverless_integration_igw_rt" {
  vpc_id = aws_vpc.sapient_vpc.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.sapient_igw.id
  }

  tags = {
    Name = "serverless_integration_igw_rt"
  }
}

resource "aws_main_route_table_association" "sapient_rt_assoc" {
  vpc_id         = aws_vpc.sapient_vpc.id
  route_table_id = aws_route_table.serverless_integration_igw_rt.id
}

resource "aws_internet_gateway" "sapient_igw" {
  vpc_id = aws_vpc.sapient_vpc.id

  tags = {
    Name = "serverless_integration_igw"
  }
}

resource "aws_route" "sapient_igw_r" {
  route_table_id         = aws_route_table.serverless_integration_igw_rt.id
  destination_cidr_block = "0.0.0.0/0"
  gateway_id             = aws_internet_gateway.sapient_igw.id
}

resource "aws_security_group" "allow_tls" {
  name        = "allow_tls"
  description = "Allow TLS inbound traffic"
  vpc_id      = aws_vpc.sapient_vpc.id

  ingress {
    description = "TLS from VPC"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = [aws_vpc.sapient_vpc.cidr_block]
  }

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

resource "aws_security_group_rule" "allow_postgres_from_ip" {
  type              = "ingress"
  from_port         = 5432
  to_port           = 5432
  protocol          = "tcp"
  cidr_blocks       = ["23.252.54.0/32"]
  security_group_id = aws_security_group.allow_tls.id
}

resource "aws_security_group_rule" "allow_postgres_to_ip" {
  type              = "egress"
  from_port         = 5432
  to_port           = 5432
  protocol          = "tcp"
  cidr_blocks       = ["0.0.0.0/0"]
  security_group_id = aws_security_group.allow_tls.id
}