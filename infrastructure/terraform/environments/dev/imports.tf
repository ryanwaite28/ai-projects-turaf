# Terraform Import Blocks for DEV environment state drift
#
# These resources were created by a previous Terraform run whose state was lost
# (the DynamoDB lock table didn't exist yet, so state couldn't be saved).
# Import blocks bring them into Terraform state so subsequent applies don't
# try to re-create them. Terraform 1.5+ silently no-ops if already in state.

import {
  to = module.security.aws_iam_role.ecs_execution_role
  id = "turaf-ecs-execution-role-dev"
}

import {
  to = module.security.aws_iam_role.ecs_task_role
  id = "turaf-ecs-task-role-dev"
}

import {
  to = module.messaging.aws_cloudwatch_event_bus.main
  id = "turaf-event-bus-dev"
}

import {
  to = module.messaging.aws_dynamodb_table.idempotency
  id = "turaf-event-idempotency-dev"
}

import {
  to = module.messaging.aws_sqs_queue.dlq
  id = "https://sqs.us-east-1.amazonaws.com/801651112319/turaf-dlq-dev"
}

import {
  to = module.messaging.aws_cloudwatch_event_archive.main
  id = "turaf-event-archive-dev"
}

import {
  to = module.messaging.aws_sqs_queue.events
  id = "https://sqs.us-east-1.amazonaws.com/801651112319/turaf-events-dev"
}
