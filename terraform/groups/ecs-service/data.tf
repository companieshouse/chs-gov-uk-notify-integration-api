data "vault_generic_secret" "stack_secrets" {
  path = "applications/${var.aws_profile}/${var.environment}/${local.stack_name}-stack"
}

data "aws_kms_key" "kms_key" {
  key_id = local.kms_alias
}

data "vault_generic_secret" "service_secrets" {
  path = "applications/${var.aws_profile}/${var.environment}/${local.stack_name}-stack/${local.service_name}"
}

data "aws_vpc" "vpc" {
  filter {
    name   = "tag:Name"
    values = [local.vpc_name]
  }
}

data "aws_subnets" "application" {
  filter {
    name   = "tag:Name"
    values = [local.application_subnet_pattern]
  }
}

data "aws_ecs_cluster" "ecs_cluster" {
  cluster_name = "${local.name_prefix}-cluster"
}

data "aws_iam_role" "ecs_cluster_iam_role" {
  name = "${local.name_prefix}-ecs-task-execution-role"
}

data "aws_lb" "service_lb" {
  name = "${var.environment}-chs-internalapi"
}

data "aws_lb_listener" "service_lb_listener" {
  load_balancer_arn = data.aws_lb.service_lb.arn
  port              = 443
}

# retrieve all global secrets for this env using global path
data "aws_ssm_parameters_by_path" "global_secrets" {
  path = "/${local.global_prefix}"
}

# create a list of secrets names to retrieve them in a nicer format and lookup each secret by name
data "aws_ssm_parameter" "global_secret" {
  for_each = toset(data.aws_ssm_parameters_by_path.global_secrets.names)
  name     = each.key
}

// --- s3 bucket for shared services config ---
data "vault_generic_secret" "shared_s3" {
  path = "aws-accounts/shared-services/s3"
}

data "aws_s3_bucket" "notification_attachments" {
  bucket = "notification-attachments-${var.environment}"
}

# policy on the role allowing ecs to read from s3 bucket
data "aws_iam_policy_document" "read_trust" {
  statement {
    effect  = "Allow"
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["ecs-tasks.amazonaws.com"]
    }
  }
}

data "aws_iam_policy_document" "read_from_s3" {
  statement {
    sid       = "ReadFromBucket"
    effect    = "Allow"
    actions   = ["s3:GetObject"]
    resources = ["${data.aws_s3_bucket.notification_attachments.arn}/*"]
  }
}
