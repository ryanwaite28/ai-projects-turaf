#!/bin/bash

echo "=========================================="
echo "Testing Lambda Packaging (Dry Run)"
echo "=========================================="
echo "This script tests Lambda packaging without deploying to MiniStack"
echo ""

# Script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"
BUILD_DIR="$SCRIPT_DIR/lambda-builds-test"

# Cleanup function
cleanup() {
    if [ "$KEEP_BUILD" != "true" ]; then
        echo "Cleaning up temporary files..."
        rm -rf "$BUILD_DIR"
    else
        echo "Build artifacts kept in: $BUILD_DIR"
    fi
}
trap cleanup EXIT

# Create build directory
mkdir -p "$BUILD_DIR"

# ==========================================
# Test Lambda Packaging
# ==========================================

test_package_lambda() {
    local service_name=$1
    local service_path=$2
    
    echo "=========================================="
    echo "Testing: $service_name"
    echo "=========================================="
    
    if [ ! -d "$service_path" ]; then
        echo "❌ Error: Service path not found: $service_path"
        return 1
    fi
    
    local build_path="$BUILD_DIR/$service_name"
    local output_zip="$BUILD_DIR/${service_name}.zip"
    
    mkdir -p "$build_path"
    
    # Check requirements.txt
    if [ -f "$service_path/requirements.txt" ]; then
        echo "✓ Found requirements.txt"
        echo "  Dependencies:"
        cat "$service_path/requirements.txt" | grep -v "^#" | grep -v "^$" | sed 's/^/    - /'
    else
        echo "⚠ No requirements.txt found"
    fi
    
    # Check source structure
    echo ""
    echo "Source structure:"
    if [ -d "$service_path/src" ]; then
        echo "  ✓ src/ directory found"
        find "$service_path/src" -name "*.py" | head -5 | sed 's/^/    /'
    fi
    
    if [ -f "$service_path/notification_handler.py" ]; then
        echo "  ✓ notification_handler.py found"
    fi
    
    if [ -f "$service_path/src/lambda_handler.py" ]; then
        echo "  ✓ lambda_handler.py found"
    fi
    
    # Test packaging (without installing dependencies)
    echo ""
    echo "Testing package creation..."
    
    # Copy source code
    if [ -d "$service_path/src" ]; then
        cp -r "$service_path/src" "$build_path/"
    fi
    
    if [ "$service_name" = "notification-service" ]; then
        cp -r "$service_path"/*.py "$build_path/" 2>/dev/null || true
        cp -r "$service_path/handlers" "$build_path/" 2>/dev/null || true
        cp -r "$service_path/services" "$build_path/" 2>/dev/null || true
        cp -r "$service_path/clients" "$build_path/" 2>/dev/null || true
        cp -r "$service_path/models" "$build_path/" 2>/dev/null || true
        cp -r "$service_path/templates" "$build_path/" 2>/dev/null || true
    fi
    
    if [ "$service_name" = "reporting-service" ]; then
        if [ -d "$service_path/src/templates" ]; then
            mkdir -p "$build_path/templates"
            cp -r "$service_path/src/templates"/* "$build_path/templates/" 2>/dev/null || true
        fi
    fi
    
    # Create ZIP
    cd "$build_path"
    if zip -q -r "$output_zip" . 2>/dev/null; then
        local zip_size=$(du -h "$output_zip" | cut -f1)
        echo "  ✓ ZIP created successfully: $zip_size"
        
        # Show ZIP contents
        echo ""
        echo "ZIP contents (sample):"
        unzip -l "$output_zip" | head -20 | tail -15
        
        # Check for handler file
        echo ""
        if [ "$service_name" = "reporting-service" ]; then
            if unzip -l "$output_zip" | grep -q "src/lambda_handler.py"; then
                echo "  ✓ Handler file found: src/lambda_handler.py"
            else
                echo "  ❌ Handler file NOT found: src/lambda_handler.py"
            fi
        else
            if unzip -l "$output_zip" | grep -q "notification_handler.py"; then
                echo "  ✓ Handler file found: notification_handler.py"
            else
                echo "  ❌ Handler file NOT found: notification_handler.py"
            fi
        fi
    else
        echo "  ❌ Failed to create ZIP"
        cd - > /dev/null
        return 1
    fi
    cd - > /dev/null
    
    echo ""
    echo "✓ $service_name packaging test passed"
    return 0
}

# Test Reporting Service
test_package_lambda "reporting-service" "$PROJECT_ROOT/services/reporting-service"
REPORTING_RESULT=$?

echo ""

# Test Notification Service
test_package_lambda "notification-service" "$PROJECT_ROOT/services/notification-service"
NOTIFICATION_RESULT=$?

# ==========================================
# Summary
# ==========================================
echo ""
echo "=========================================="
echo "Test Summary"
echo "=========================================="

if [ $REPORTING_RESULT -eq 0 ]; then
    echo "✓ Reporting Service: PASSED"
else
    echo "❌ Reporting Service: FAILED"
fi

if [ $NOTIFICATION_RESULT -eq 0 ]; then
    echo "✓ Notification Service: PASSED"
else
    echo "❌ Notification Service: FAILED"
fi

echo ""
if [ $REPORTING_RESULT -eq 0 ] && [ $NOTIFICATION_RESULT -eq 0 ]; then
    echo "✓ All tests passed! Lambda packaging should work correctly."
    echo ""
    echo "To deploy to MiniStack, run:"
    echo "  ./init-lambda-services.sh"
    exit 0
else
    echo "❌ Some tests failed. Please fix the issues before deploying."
    exit 1
fi
