#!/bin/bash

# Test data cleanup utilities
# Note: The current API doesn't provide DELETE endpoints, so cleanup is limited
# In a real scenario, you might need database access or test-specific endpoints

source "$(dirname "$0")/common.sh"

# Cleanup test consumers (limited since no DELETE API)
cleanup_test_consumers() {
    log_info "Cleaning up test consumers..."
    log_warning "Note: API doesn't provide consumer deletion endpoints"
    log_warning "Test consumers will remain in database"
    log_info "Manual cleanup may be required for: test-mobile-app, test-web-dashboard, test-analytics"
}

# Cleanup test schemas (limited since no DELETE API)
cleanup_test_schemas() {
    log_info "Cleaning up test schemas..."
    log_warning "Note: API doesn't provide schema deletion endpoints"
    log_warning "Test schemas will remain in database"
    log_info "Manual cleanup may be required for subjects: test-user-profile, test-product, test-order"
}

# Cleanup test templates (limited since no DELETE API)
cleanup_test_templates() {
    log_info "Cleaning up test templates..."
    log_warning "Note: API doesn't provide template deletion endpoints"
    log_warning "Test templates will remain in database"
    log_info "Manual cleanup may be required for consumers: test-mobile-app, test-web-dashboard, test-analytics"
}

# Complete cleanup (logs warnings about limitations)
cleanup_all_test_data() {
    log_info "Starting complete test data cleanup..."

    cleanup_test_consumers
    cleanup_test_schemas
    cleanup_test_templates

    log_warning "Due to API limitations, test data cleanup is not fully automated"
    log_warning "Consider implementing DELETE endpoints or database cleanup scripts for comprehensive testing"
}

# Database cleanup (requires direct database access)
# This is a template for manual database cleanup
cleanup_database_manual() {
    log_info "Manual database cleanup instructions:"
    echo ""
    echo "Connect to PostgreSQL database and run:"
    echo "DELETE FROM transformation_templates WHERE consumer_id LIKE 'test-%';"
    echo "DELETE FROM consumers WHERE consumer_id LIKE 'test-%';"
    echo "DELETE FROM schemas WHERE subject LIKE 'test-%';"
    echo ""
    echo "Or truncate all tables (WARNING: destroys all data):"
    echo "TRUNCATE TABLE transformation_templates, consumers, schemas RESTART IDENTITY CASCADE;"
}

# Reset test counters
reset_test_counters() {
    TESTS_RUN=0
    TESTS_PASSED=0
    TESTS_FAILED=0
    log_info "Test counters reset"
}

# Clean test artifacts (files, logs, etc.)
cleanup_test_artifacts() {
    log_info "Cleaning up test artifacts..."

    # Remove any test log files
    find "$(dirname "$0")/.." -name "*.log" -type f -delete 2>/dev/null

    # Remove any test output files
    find "$(dirname "$0")/.." -name "test-output-*.txt" -type f -delete 2>/dev/null

    # Remove any temporary test files
    find "$(dirname "$0")/.." -name "tmp-*.json" -type f -delete 2>/dev/null

    log_success "Test artifacts cleanup complete"
}

# Full cleanup including artifacts
full_cleanup() {
    log_info "Performing full cleanup..."

    cleanup_all_test_data
    cleanup_test_artifacts
    reset_test_counters

    log_success "Full cleanup complete"
}

# Quick cleanup for development
dev_cleanup() {
    log_info "Development cleanup (minimal)..."

    cleanup_test_artifacts
    reset_test_counters

    log_info "Development cleanup complete"
    log_warning "Test data in database not cleaned up (use manual cleanup if needed)"
}