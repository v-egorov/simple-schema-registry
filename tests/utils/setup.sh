#!/bin/bash

# Test data setup utilities
# Source this file to get access to setup functions

source "$(dirname "$0")/common.sh"

# Setup test consumers
setup_test_consumers() {
    log_info "Setting up test consumers..."

    # Create mobile app consumer
    create_test_consumer "test-mobile-app" "Test Mobile App" "Test consumer for mobile applications"

    # Create web dashboard consumer
    create_test_consumer "test-web-dashboard" "Test Web Dashboard" "Test consumer for web dashboard"

    # Create analytics consumer
    create_test_consumer "test-analytics" "Test Analytics" "Test consumer for analytics service"

    log_success "Test consumers setup complete"
}

# Setup test schemas
setup_test_schemas() {
    log_info "Setting up test schemas..."

    # User profile schema
    local user_schema='{
        "type": "object",
        "properties": {
            "id": {"type": "integer"},
            "name": {"type": "string"},
            "email": {"type": "string", "format": "email"},
            "age": {"type": "integer", "minimum": 0}
        },
        "required": ["id", "name"]
    }'
    create_test_schema "test-user-profile" "$user_schema"

    # Product schema
    local product_schema='{
        "type": "object",
        "properties": {
            "productId": {"type": "string"},
            "name": {"type": "string"},
            "price": {"type": "number", "minimum": 0},
            "category": {"type": "string"}
        },
        "required": ["productId", "name", "price"]
    }'
    create_test_schema "test-product" "$product_schema"

    # Order schema
    local order_schema='{
        "type": "object",
        "properties": {
            "orderId": {"type": "string"},
            "userId": {"type": "integer"},
            "items": {
                "type": "array",
                "items": {
                    "type": "object",
                    "properties": {
                        "productId": {"type": "string"},
                        "quantity": {"type": "integer", "minimum": 1}
                    }
                }
            },
            "totalAmount": {"type": "number", "minimum": 0}
        },
        "required": ["orderId", "userId", "items"]
    }'
    create_test_schema "test-order" "$order_schema"

    log_success "Test schemas setup complete"
}

# Setup test transformation templates
setup_test_templates() {
    log_info "Setting up test transformation templates..."

    # Mobile app template for user data
    local mobile_template='. | {id: .id, name: .name, email: .email}'
    create_test_template "test-mobile-app" "$mobile_template"

    # Web dashboard template for user data
    local web_template='. | {userId: .id, fullName: .name, emailAddress: .email, age: .age}'
    create_test_template "test-web-dashboard" "$web_template"

    # Analytics template for user data
    local analytics_template='. | {user_id: .id, user_name: .name, user_email: .email, user_age: .age}'
    create_test_template "test-analytics" "$analytics_template"

    log_success "Test templates setup complete"
}

# Complete test environment setup
setup_test_environment() {
    log_info "Setting up complete test environment..."

    setup_test_consumers
    setup_test_schemas
    setup_test_templates

    log_success "Test environment setup complete"
}

# Quick setup for specific test scenarios
setup_user_scenario() {
    log_info "Setting up user profile scenario..."

    create_test_consumer "test-mobile-app" "Test Mobile App"
    create_test_schema "test-user-profile" '{
        "type": "object",
        "properties": {
            "id": {"type": "integer"},
            "name": {"type": "string"},
            "email": {"type": "string", "format": "email"}
        },
        "required": ["id", "name"]
    }'
    create_test_template "test-mobile-app" '. | {id: .id, name: .name, email: .email}'

    log_success "User scenario setup complete"
}

setup_product_scenario() {
    log_info "Setting up product catalog scenario..."

    create_test_consumer "test-web-dashboard" "Test Web Dashboard"
    create_test_schema "test-product" '{
        "type": "object",
        "properties": {
            "productId": {"type": "string"},
            "name": {"type": "string"},
            "price": {"type": "number"}
        },
        "required": ["productId", "name"]
    }'
    create_test_template "test-web-dashboard" '. | {id: .productId, title: .name, cost: .price}'

    log_success "Product scenario setup complete"
}