#!/bin/bash

set -e

echo "========================================="
echo "Turaf Database Migration Script"
echo "========================================="
echo ""

# Validate required environment variables
if [ -z "$DB_HOST" ]; then
  echo "❌ Error: DB_HOST environment variable is not set"
  exit 1
fi

if [ -z "$DB_NAME" ]; then
  echo "❌ Error: DB_NAME environment variable is not set"
  exit 1
fi

if [ -z "$DB_USER" ]; then
  echo "❌ Error: DB_USER environment variable is not set"
  exit 1
fi

if [ -z "$DB_PASSWORD" ]; then
  echo "❌ Error: DB_PASSWORD environment variable is not set"
  exit 1
fi

echo "✅ Environment variables validated"
echo "   DB_HOST: $DB_HOST"
echo "   DB_NAME: $DB_NAME"
echo "   DB_USER: $DB_USER"
echo ""

# Test database connectivity
echo "Testing database connectivity..."
if command -v psql &> /dev/null; then
  PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -U $DB_USER -d $DB_NAME -c "SELECT version();" > /dev/null 2>&1
  if [ $? -eq 0 ]; then
    echo "✅ Database connection successful"
  else
    echo "❌ Error: Cannot connect to database"
    exit 1
  fi
else
  echo "⚠️  Warning: psql not available, skipping connectivity test"
fi
echo ""

# Count migration files
MIGRATION_COUNT=$(ls -1 migrations/*.sql 2>/dev/null | wc -l)
echo "Found $MIGRATION_COUNT migration files"
echo ""

# Run Flyway info to show current state
echo "Current migration status:"
flyway -configFiles=flyway.conf info

echo ""
echo "========================================="
echo "Applying migrations..."
echo "========================================="
echo ""

# Run Flyway migrate
flyway -configFiles=flyway.conf migrate

MIGRATION_EXIT_CODE=$?

echo ""
echo "========================================="
echo "Migration Results"
echo "========================================="
echo ""

if [ $MIGRATION_EXIT_CODE -eq 0 ]; then
  echo "✅ Migrations applied successfully"
  echo ""
  echo "Final migration status:"
  flyway -configFiles=flyway.conf info
  exit 0
else
  echo "❌ Migration failed with exit code: $MIGRATION_EXIT_CODE"
  echo ""
  echo "Migration status:"
  flyway -configFiles=flyway.conf info
  exit $MIGRATION_EXIT_CODE
fi
