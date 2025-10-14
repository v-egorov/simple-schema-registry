#!/bin/bash

# Master Test Runner Script
# Executes all test suites with comprehensive reporting

set -e  # Exit on any error

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BASE_URL="${BASE_URL:-http://localhost:8080}"
PARALLEL="${PARALLEL:-false}"
JUNIT_REPORT="${JUNIT_REPORT:-false}"
VERBOSE="${VERBOSE:-false}"
HEALTH_ONLY="${HEALTH_ONLY:-false}"
QUICK="${QUICK:-false}"
CONSUMERS_ONLY="${CONSUMERS_ONLY:-false}"
SCHEMAS_ONLY="${SCHEMAS_ONLY:-false}"
TRANSFORM_ONLY="${TRANSFORM_ONLY:-false}"
WORKFLOWS_ONLY="${WORKFLOWS_ONLY:-false}"
ERROR_HANDLING_ONLY="${ERROR_HANDLING_ONLY:-false}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Test results
TOTAL_TESTS_RUN=0
TOTAL_TESTS_PASSED=0
TOTAL_TESTS_FAILED=0
SUITE_RESULTS=()

# Logging functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[PASS]${NC} $1"
}

log_error() {
    echo -e "${RED}[FAIL]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_header() {
    echo -e "${PURPLE}=== $1 ===${NC}"
}

log_suite() {
    echo -e "${CYAN}>>> $1${NC}"
}

# Print usage information
usage() {
    echo "JSON Schema Registry Test Runner"
    echo ""
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Options:"
    echo "  -h, --help          Show this help message"
    echo "  -u, --url URL       Base URL for API (default: http://localhost:8080)"
    echo "  -p, --parallel      Run tests in parallel (experimental)"
    echo "  -j, --junit FILE    Generate JUnit XML report"
    echo "  -v, --verbose       Verbose output"
    echo "  --health-only       Run only health checks"
    echo "  --quick             Run only critical tests"
    echo "  --consumers         Run only consumer tests"
    echo "  --schemas           Run only schema tests"
    echo "  --transform         Run only transformation tests"
    echo "  --workflows         Run only workflow tests"
    echo "  --error-handling    Run only error handling tests"
    echo ""
    echo "Environment variables:"
    echo "  BASE_URL            Same as --url"
    echo "  PARALLEL            Same as --parallel"
    echo "  JUNIT_REPORT        Path to JUnit XML report file"
    echo "  VERBOSE             Same as --verbose"
    echo ""
    echo "Examples:"
    echo "  $0                                    # Run all tests"
    echo "  $0 --url http://staging:8080         # Test staging environment"
    echo "  $0 --parallel                        # Run tests in parallel"
    echo "  $0 --junit results.xml               # Generate JUnit report"
    echo "  $0 --health-only                     # Quick health check"
    echo "  $0 --consumers                       # Test consumer functionality only"
    echo "  $0 --schemas                         # Test schema functionality only"
}

# Parse command line arguments
parse_args() {
    while [[ $# -gt 0 ]]; do
        case $1 in
            -h|--help)
                usage
                exit 0
                ;;
            -u|--url)
                BASE_URL="$2"
                shift 2
                ;;
            -p|--parallel)
                PARALLEL=true
                shift
                ;;
            -j|--junit)
                JUNIT_REPORT="$2"
                shift 2
                ;;
            -v|--verbose)
                VERBOSE=true
                shift
                ;;
            --health-only)
                HEALTH_ONLY=true
                shift
                ;;
            --quick)
                QUICK=true
                shift
                ;;
            --consumers)
                CONSUMERS_ONLY=true
                shift
                ;;
            --schemas)
                SCHEMAS_ONLY=true
                shift
                ;;
            --transform)
                TRANSFORM_ONLY=true
                shift
                ;;
            --workflows)
                WORKFLOWS_ONLY=true
                shift
                ;;
            --error-handling)
                ERROR_HANDLING_ONLY=true
                shift
                ;;
            *)
                log_error "Unknown option: $1"
                usage
                exit 1
                ;;
        esac
    done
}

# Run a single test script
run_test_script() {
    local script_path="$1"
    local script_name=$(basename "$script_path" .sh)
    local start_time=$(date +%s)

    if [ "$VERBOSE" = true ]; then
        # Run with verbose output
        if ! BASE_URL="$BASE_URL" bash "$script_path"; then
            log_error "$script_name failed"
            return 1
        fi
    else
        # Capture output
        if output=$(BASE_URL="$BASE_URL" bash "$script_path" 2>&1); then
            log_success "$script_name completed"
        else
            log_error "$script_name failed"
            if [ "$VERBOSE" = true ] || [ -n "$output" ]; then
                echo "$output"
            fi
            return 1
        fi
    fi

    local end_time=$(date +%s)
    local duration=$((end_time - start_time))

    log_info "$script_name completed in ${duration}s"
    return 0
}

# Run test suite
run_test_suite() {
    local suite_name="$1"
    local suite_dir="$2"
    local script_pattern="${3:-*.sh}"

    log_header "$suite_name Tests"

    local test_run=0
    local suite_tests_passed=0
    local suite_tests_failed=0

    if [ ! -d "$suite_dir" ]; then
        log_warning "Test suite directory not found: $suite_dir"
        return 0
    fi

    # Define script execution order for each suite
    local scripts=()
    if [ "$script_pattern" != "*.sh" ]; then
        # Specific script requested (e.g., for quick mode)
        scripts=("$script_pattern")
    else
        case "$suite_name" in
            "Health")
                scripts=("test-health.sh" "test-actuator.sh")
                ;;
            "Consumers")
                scripts=("test-consumer-register.sh" "test-consumer-get.sh" "test-consumer-list.sh")
                ;;
            "Schemas")
                scripts=("test-schema-register.sh" "test-schema-get-specific.sh" "test-schema-get-latest.sh" "test-schema-get-all.sh" "test-schema-subjects.sh" "test-schema-compatibility.sh")
                ;;
            "Transform")
                scripts=("test-transform-template-create.sh" "test-transform-template-get.sh" "test-transform-data.sh" "test-transform-engines.sh" "test-transform-router.sh" "test-transform-pipeline.sh")
                ;;
            "Workflows")
                scripts=("test-full-workflow.sh" "test-schema-evolution.sh")
                ;;
            "Error Handling")
                scripts=("test-errors-400.sh" "test-errors-404.sh" "test-errors-409.sh")
                ;;
            *)
                # Fallback: use all scripts in alphabetical order
                for script in "$suite_dir"/*.sh; do
                    if [ -f "$script" ]; then
                        scripts+=("$(basename "$script")")
                    fi
                done
                ;;
        esac
    fi
    log_info "Scripts array: ${scripts[*]}"
    local script_count=${#scripts[@]}
    if [ "$script_count" -eq 0 ]; then
        log_warning "No test scripts found in $suite_name"
        return 0
    fi

    log_info "Found $script_count test scripts in $suite_name"

    for script_name in "${scripts[@]}"; do
        local script_path="$suite_dir/$script_name"
        log_info "Checking $script_path"
        if [ -f "$script_path" ]; then
            if run_test_script "$script_path"; then
                suite_tests_passed=$((suite_tests_passed + 1))
            else
                suite_tests_failed=$((suite_tests_failed + 1))
            fi
            test_run=$((test_run + 1))
        else
            log_warning "Script not found: $script_path"
        fi
    done

    # Store suite results
    SUITE_RESULTS+=("$suite_name:$test_run:$suite_tests_passed:$suite_tests_failed")

    TOTAL_TESTS_RUN=$((TOTAL_TESTS_RUN + test_run))
    TOTAL_TESTS_PASSED=$((TOTAL_TESTS_PASSED + suite_tests_passed))
    TOTAL_TESTS_FAILED=$((TOTAL_TESTS_FAILED + suite_tests_failed))

    log_info "$suite_name: $test_run tests, $suite_tests_passed passed, $suite_tests_failed failed"
}

# Generate JUnit XML report
generate_junit_report() {
    local report_file="$1"

    if [ -z "$report_file" ]; then
        return 0
    fi

    log_info "Generating JUnit XML report: $report_file"

    cat > "$report_file" << EOF
<?xml version="1.0" encoding="UTF-8"?>
<testsuites name="JSON Schema Registry Tests" tests="$TOTAL_TESTS_RUN" failures="$TOTAL_TESTS_FAILED" time="0">
EOF

    for suite_result in "${SUITE_RESULTS[@]}"; do
        IFS=':' read -r suite_name tests_run tests_passed tests_failed <<< "$suite_result"

        cat >> "$report_file" << EOF
  <testsuite name="$suite_name" tests="$tests_run" failures="$tests_failed" time="0">
EOF

        # Add dummy test cases (simplified)
        for ((i=1; i<=tests_passed; i++)); do
            cat >> "$report_file" << EOF
    <testcase name="test_$i" classname="$suite_name" time="0"/>
EOF
        done

        for ((i=1; i<=tests_failed; i++)); do
            cat >> "$report_file" << EOF
    <testcase name="failed_test_$i" classname="$suite_name" time="0">
      <failure message="Test failed"/>
    </testcase>
EOF
        done

        cat >> "$report_file" << EOF
  </testsuite>
EOF
    done

    cat >> "$report_file" << EOF
</testsuites>
EOF

    log_success "JUnit report generated: $report_file"
}

# Print final summary
print_final_summary() {
    echo
    log_header "FINAL TEST SUMMARY"
    echo "========================================"
    echo "Total test suites: ${#SUITE_RESULTS[@]}"
    echo "Total tests run:   $TOTAL_TESTS_RUN"
    echo -e "Tests passed:      ${GREEN}$TOTAL_TESTS_PASSED${NC}"
    echo -e "Tests failed:      ${RED}$TOTAL_TESTS_FAILED${NC}"

    # Suite breakdown
    echo
    echo "Suite Results:"
    for suite_result in "${SUITE_RESULTS[@]}"; do
        IFS=':' read -r suite_name tests_run tests_passed tests_failed <<< "$suite_result"
        printf "  %-20s %3d tests (%2d passed, %2d failed)\n" "$suite_name:" "$tests_run" "$tests_passed" "$tests_failed"
    done

    echo
    if [ "$TOTAL_TESTS_FAILED" -eq 0 ]; then
        echo -e "${GREEN}🎉 ALL TESTS PASSED! 🎉${NC}"
        echo
        echo "Your JSON Schema Registry and Transformation Service is working correctly!"
        return 0
    else
        local pass_rate=$((TOTAL_TESTS_PASSED * 100 / TOTAL_TESTS_RUN))
        echo -e "${RED}❌ $TOTAL_TESTS_FAILED TESTS FAILED (${pass_rate}% pass rate)${NC}"
        echo
        echo "Please review the failed tests above and check your service configuration."
        return 1
    fi
}

# Health check function
run_health_check() {
    log_header "Health Check"

    # Source common utilities
    source "$SCRIPT_DIR/utils/common.sh"

    if check_service_health; then
        log_success "Service is healthy and ready for testing"
        return 0
    else
        log_error "Service is not healthy. Please start the service before running tests."
        echo
        echo "To start the service:"
        echo "  docker-compose up -d"
        echo "  # or"
        echo "  mvn spring-boot:run"
        return 1
    fi
}

# Main execution
main() {
    parse_args "$@"

    echo "JSON Schema Registry Test Suite"
    echo "==============================="
    echo "Base URL: $BASE_URL"
    echo "Parallel: $PARALLEL"
    echo "Verbose:  $VERBOSE"
    if [ -n "$JUNIT_REPORT" ]; then
        echo "JUnit Report: $JUNIT_REPORT"
    fi
    echo

    # Export BASE_URL for test scripts
    export BASE_URL

    # Run health check first
    if ! run_health_check; then
        exit 1
    fi

    echo

    # Determine which tests to run
    if [ "$HEALTH_ONLY" = true ]; then
        run_test_suite "Health" "$SCRIPT_DIR/health"
    elif [ "$QUICK" = true ]; then
        # Run only critical tests
        run_test_suite "Health" "$SCRIPT_DIR/health"
        run_test_suite "Consumers" "$SCRIPT_DIR/consumers" "test-consumer-register.sh"
        run_test_suite "Schemas" "$SCRIPT_DIR/schemas" "test-schema-register.sh"
        run_test_suite "Transform" "$SCRIPT_DIR/transform" "test-transform-data.sh"
    elif [ "$CONSUMERS_ONLY" = true ]; then
        run_test_suite "Consumers" "$SCRIPT_DIR/consumers"
    elif [ "$SCHEMAS_ONLY" = true ]; then
        run_test_suite "Schemas" "$SCRIPT_DIR/schemas"
    elif [ "$TRANSFORM_ONLY" = true ]; then
        run_test_suite "Transform" "$SCRIPT_DIR/transform"
    elif [ "$WORKFLOWS_ONLY" = true ]; then
        run_test_suite "Workflows" "$SCRIPT_DIR/workflows"
    elif [ "$ERROR_HANDLING_ONLY" = true ]; then
        run_test_suite "Error Handling" "$SCRIPT_DIR/error-handling"
    else
        # Run all test suites
        run_test_suite "Health" "$SCRIPT_DIR/health"
        run_test_suite "Consumers" "$SCRIPT_DIR/consumers"
        run_test_suite "Schemas" "$SCRIPT_DIR/schemas"
        run_test_suite "Transform" "$SCRIPT_DIR/transform"
        run_test_suite "Workflows" "$SCRIPT_DIR/workflows"
        run_test_suite "Error Handling" "$SCRIPT_DIR/error-handling"
    fi

    # Generate JUnit report if requested
    if [ "$JUNIT_REPORT" != false ]; then
        generate_junit_report "$JUNIT_REPORT"
    fi

    # Print final summary
    print_final_summary
}

# Run main function with all arguments
main "$@"