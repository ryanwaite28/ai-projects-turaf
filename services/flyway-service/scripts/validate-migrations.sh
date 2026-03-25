#!/bin/bash

set -e

echo "========================================="
echo "Flyway Migration Validation Script"
echo "========================================="
echo ""

MIGRATIONS_DIR="migrations"
ERRORS=0

# Check if migrations directory exists
if [ ! -d "$MIGRATIONS_DIR" ]; then
  echo "❌ Error: migrations directory not found"
  exit 1
fi

# Count migration files
MIGRATION_COUNT=$(ls -1 $MIGRATIONS_DIR/*.sql 2>/dev/null | wc -l)
echo "Found $MIGRATION_COUNT migration files"
echo ""

if [ $MIGRATION_COUNT -eq 0 ]; then
  echo "⚠️  Warning: No migration files found"
  exit 0
fi

echo "Validating migration files..."
echo ""

# Validate each migration file
for file in $MIGRATIONS_DIR/*.sql; do
  filename=$(basename "$file")
  
  # Check naming convention: V{NNN}__{service}_{description}.sql
  if [[ ! $filename =~ ^V[0-9]{3}__[a-z]+_[a-z_]+\.sql$ ]]; then
    echo "❌ Invalid filename format: $filename"
    echo "   Expected format: V{NNN}__{service}_{description}.sql"
    ERRORS=$((ERRORS + 1))
    continue
  fi
  
  # Extract version number
  VERSION=$(echo $filename | sed 's/V\([0-9]*\)__.*/\1/')
  
  # Check if file contains service identifier comment
  if ! grep -q "^-- Service:" "$file"; then
    echo "⚠️  Warning: Missing service identifier comment in $filename"
  fi
  
  # Check if file contains schema identifier comment
  if ! grep -q "^-- Schema:" "$file"; then
    echo "⚠️  Warning: Missing schema identifier comment in $filename"
  fi
  
  # Check if file contains description comment
  if ! grep -q "^-- Description:" "$file"; then
    echo "⚠️  Warning: Missing description comment in $filename"
  fi
  
  # Check if file is not empty
  if [ ! -s "$file" ]; then
    echo "❌ Empty migration file: $filename"
    ERRORS=$((ERRORS + 1))
    continue
  fi
  
  # Basic SQL syntax check (look for common issues)
  if grep -q "DROP DATABASE" "$file"; then
    echo "❌ Dangerous operation detected in $filename: DROP DATABASE"
    ERRORS=$((ERRORS + 1))
  fi
  
  if grep -q "DROP SCHEMA.*CASCADE" "$file"; then
    echo "⚠️  Warning: DROP SCHEMA CASCADE detected in $filename"
  fi
  
  echo "✅ $filename"
done

echo ""
echo "========================================="
echo "Validation Summary"
echo "========================================="
echo ""

if [ $ERRORS -eq 0 ]; then
  echo "✅ All migration files validated successfully"
  echo "   Total files: $MIGRATION_COUNT"
  exit 0
else
  echo "❌ Validation failed with $ERRORS error(s)"
  echo "   Total files: $MIGRATION_COUNT"
  exit 1
fi
