resource "aws_iam_role" "notifications_read" {
  name               = "${local.name_prefix}-read"
  assume_role_policy = data.aws_iam_policy_document.read_trust.json
}

resource "aws_iam_role_policy" "read_from_s3" {
  name   = "${local.name_prefix}-read-s3"
  role   = aws_iam_role.notifications_read.id
  policy = data.aws_iam_policy_document.read_from_s3.json
}
