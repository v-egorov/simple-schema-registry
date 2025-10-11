# JSON Schema Registry and Transformation Service - Development Makefile

.PHONY: help build clean test up down logs db-migrate db-seed restart shell db-connect db-status db-tables db-size db-schemas-count db-schemas-recent db-schemas-subjects db-schemas-versions db-consumers-list db-consumers-active db-consumers-stats db-templates-list db-templates-engines db-templates-recent db-indexes db-locks db-connections db-vacuum-analyze db-reindex db-cleanup-test-data db-schema-evolution db-compatibility-checks db-orphaned-data db-export-schema db-backup db-activity

# Default target
help: ## Show this help message
	@echo "JSON Schema Registry and Transformation Service"
	@echo ""
	@echo "Available targets:"
	@awk 'BEGIN {FS = ":.*?## "} /^[a-zA-Z_-]+:.*?## / {printf "  %-15s %s\n", $$1, $$2}' $(MAKEFILE_LIST)

# Build targets
build: ## Build the application JAR
	mvn clean package -DskipTests

build-docker: ## Build Docker image
	docker-compose build

# Test targets
test: ## Run all tests (see docs/testing.md for execution order)
	mvn test

test-unit: ## Run unit tests only
	mvn test -Dtest="*Test"

test-integration: ## Run integration tests only (see docs/testing.md for details)
	mvn test -Dtest="*IT"

test-integration-manual: ## Run integration tests manually with proper ordering
	./tests/run-all.sh

# Docker Compose targets
up: ## Start all services (app + database)
	docker-compose up -d

up-build: ## Start all services and rebuild if necessary
	docker-compose up --build -d

down: ## Stop all services
	docker-compose down

restart: ## Restart all services
	docker-compose restart

# Logging targets
logs: ## Show logs from all services
	docker-compose logs -f

logs-app: ## Show logs from app service only
	docker-compose logs -f app

logs-db: ## Show logs from database service only
	docker-compose logs -f db

# Database targets
db-migrate: ## Run database migrations
	mvn flyway:migrate

db-clean: ## Clean database (drop all objects)
	mvn flyway:clean

db-info: ## Show database migration info
	mvn flyway:info

db-seed: ## Seed database with sample data (if implemented)
	@echo "Database seeding not yet implemented"

# Development targets
run: ## Run the application locally (requires PostgreSQL running)
	mvn spring-boot:run

run-dev: ## Run in development mode with live reload
	mvn spring-boot:run -Dspring-boot.run.fork=false

shell: ## Open shell in running app container
	docker-compose exec app sh

shell-db: ## Open PostgreSQL shell
	docker-compose exec db psql -U schema_user -d schema_registry

# Database Diagnostics targets
db-connect: ## Connect to database (alias for shell-db)
	docker-compose exec db psql -U schema_user -d schema_registry

db-status: ## Show database status and basic information
	@echo "=== Database Status ==="
	@docker-compose exec -T db psql -U schema_user -d schema_registry -c "SELECT version();"
	@docker-compose exec -T db psql -U schema_user -d schema_registry -c "SELECT current_database(), current_user, now();"
	@echo ""
	@echo "=== Connection Info ==="
	@docker-compose exec -T db psql -U schema_user -d schema_registry -c "SELECT count(*) as active_connections FROM pg_stat_activity WHERE datname = 'schema_registry';"
	@echo ""
	@echo "=== Table Counts ==="
	@docker-compose exec -T db psql -U schema_user -d schema_registry -c "SELECT schemaname, relname, n_tup_ins as inserts, n_tup_upd as updates, n_tup_del as deletes FROM pg_stat_user_tables;"

db-tables: ## List all tables with row counts and sizes
	@echo "=== Tables with Row Counts ==="
	@docker-compose exec -T db psql -U schema_user -d schema_registry -c "SELECT schemaname, relname, n_tup_ins + n_tup_upd + n_tup_del as total_operations, pg_size_pretty(pg_total_relation_size(schemaname||'.'||relname)) as size FROM pg_stat_user_tables ORDER BY pg_total_relation_size(schemaname||'.'||relname) DESC;"

db-size: ## Show database and table sizes
	@echo "=== Database Size ==="
	@docker-compose exec -T db psql -U schema_user -d schema_registry -c "SELECT pg_size_pretty(pg_database_size(current_database())) as database_size;"
	@echo ""
	@echo "=== Table Sizes ==="
	@docker-compose exec -T db psql -U schema_user -d schema_registry -c "SELECT schemaname, relname, pg_size_pretty(pg_total_relation_size(schemaname||'.'||relname)) as total_size, pg_size_pretty(pg_relation_size(schemaname||'.'||relname)) as table_size, pg_size_pretty(pg_total_relation_size(schemaname||'.'||relname) - pg_relation_size(schemaname||'.'||relname)) as index_size FROM pg_stat_user_tables ORDER BY pg_total_relation_size(schemaname||'.'||relname) DESC;"

# Schema Registry Diagnostics
db-schemas-count: ## Count schemas by subject and show version stats
	@echo "=== Schema Counts by Subject ==="
	@docker-compose exec -T db psql -U schema_user -d schema_registry -c "SELECT subject, COUNT(*) as versions, MAX(version) as latest_version, MIN(created_at) as first_created, MAX(created_at) as last_updated FROM schemas GROUP BY subject ORDER BY versions DESC, last_updated DESC;"

db-schemas-recent: ## Show recently created schemas
	@echo "=== Recent Schema Registrations ==="
	@docker-compose exec -T db psql -U schema_user -d schema_registry -c "SELECT subject, version, compatibility, created_at FROM schemas ORDER BY created_at DESC LIMIT 10;"

db-schemas-subjects: ## List all unique subjects with statistics
	@echo "=== Schema Subjects Overview ==="
	@docker-compose exec -T db psql -U schema_user -d schema_registry -c "SELECT COUNT(DISTINCT subject) as total_subjects, COUNT(*) as total_schemas, AVG(versions) as avg_versions_per_subject FROM (SELECT subject, COUNT(*) as versions FROM schemas GROUP BY subject) stats;"

db-schemas-versions: ## Show version distribution across subjects
	@echo "=== Schema Version Distribution ==="
	@docker-compose exec -T db psql -U schema_user -d schema_registry -c "SELECT version, COUNT(*) as count FROM schemas GROUP BY version ORDER BY version;"

# Consumer Diagnostics
db-consumers-list: ## List all consumers with template status
	@echo "=== Consumers with Template Status ==="
	@docker-compose exec -T db psql -U schema_user -d schema_registry -c "SELECT c.consumer_id, c.name, c.created_at, CASE WHEN t.consumer_id IS NOT NULL THEN 'Has Template' ELSE 'No Template' END as template_status, t.engine FROM consumers c LEFT JOIN transformation_templates t ON c.consumer_id = t.consumer_id ORDER BY c.created_at DESC;"

db-consumers-active: ## Show consumers with recent activity
	@echo "=== Recently Active Consumers ==="
	@docker-compose exec -T db psql -U schema_user -d schema_registry -c "SELECT c.consumer_id, c.name, GREATEST(c.updated_at, COALESCE(t.updated_at, c.created_at)) as last_activity FROM consumers c LEFT JOIN transformation_templates t ON c.consumer_id = t.consumer_id ORDER BY last_activity DESC LIMIT 10;"

db-consumers-stats: ## Consumer registration statistics
	@echo "=== Consumer Statistics ==="
	@docker-compose exec -T db psql -U schema_user -d schema_registry -c "SELECT COUNT(*) as total_consumers, COUNT(CASE WHEN t.consumer_id IS NOT NULL THEN 1 END) as with_templates, COUNT(CASE WHEN t.consumer_id IS NULL THEN 1 END) as without_templates FROM consumers c LEFT JOIN transformation_templates t ON c.consumer_id = t.consumer_id;"

# Transformation Diagnostics
db-templates-list: ## List all transformation templates
	@echo "=== Transformation Templates ==="
	@docker-compose exec -T db psql -U schema_user -d schema_registry -c "SELECT t.consumer_id, c.name as consumer_name, t.engine, LEFT(t.template_expression, 50) || CASE WHEN LENGTH(t.template_expression) > 50 THEN '...' ELSE '' END as template_preview, t.created_at FROM transformation_templates t JOIN consumers c ON t.consumer_id = c.consumer_id ORDER BY t.created_at DESC;"

db-templates-engines: ## Show template engine distribution
	@echo "=== Template Engine Distribution ==="
	@docker-compose exec -T db psql -U schema_user -d schema_registry -c "SELECT engine, COUNT(*) as count FROM transformation_templates GROUP BY engine ORDER BY count DESC;"

db-templates-recent: ## Show recently updated templates
	@echo "=== Recently Updated Templates ==="
	@docker-compose exec -T db psql -U schema_user -d schema_registry -c "SELECT t.consumer_id, c.name, t.engine, t.updated_at FROM transformation_templates t JOIN consumers c ON t.consumer_id = c.consumer_id ORDER BY t.updated_at DESC LIMIT 5;"

# Performance & Health
db-indexes: ## Show table indexes and their usage
	@echo "=== Table Indexes ==="
	@docker-compose exec -T db psql -U schema_user -d schema_registry -c "SELECT schemaname, relname, indexrelname, idx_scan as scans, pg_size_pretty(pg_relation_size(indexrelid)) as size FROM pg_stat_user_indexes ORDER BY pg_relation_size(indexrelid) DESC;"

db-locks: ## Check for active locks
	@echo "=== Active Locks ==="
	@docker-compose exec -T db psql -U schema_user -d schema_registry -c "SELECT locktype, relation::regclass, mode, granted, pid FROM pg_locks WHERE database = (SELECT oid FROM pg_database WHERE datname = current_database()) ORDER BY relation;"

db-connections: ## Show active database connections
	@echo "=== Active Connections ==="
	@docker-compose exec -T db psql -U schema_user -d schema_registry -c "SELECT pid, usename, client_addr, client_port, backend_start, query_start, state FROM pg_stat_activity WHERE datname = 'schema_registry' ORDER BY backend_start;"

# Maintenance & Cleanup
db-vacuum-analyze: ## Run VACUUM ANALYZE for optimization
	@echo "Running VACUUM ANALYZE on all tables..."
	@docker-compose exec -T db psql -U schema_user -d schema_registry -c "VACUUM ANALYZE;"
	@echo "VACUUM ANALYZE completed."

db-reindex: ## Reindex all tables (use with caution)
	@echo "WARNING: Reindexing can be resource intensive and may lock tables."
	@read -p "Are you sure you want to reindex all tables? (y/N): " confirm; \
	if [ "$$confirm" = "y" ] || [ "$$confirm" = "Y" ]; then \
		echo "Reindexing tables..."; \
		docker-compose exec -T db psql -U schema_user -d schema_registry -c "REINDEX TABLE CONCURRENTLY schemas; REINDEX TABLE CONCURRENTLY consumers; REINDEX TABLE CONCURRENTLY transformation_templates;"; \
		echo "Reindexing completed."; \
	else \
		echo "Reindexing cancelled."; \
	fi

db-cleanup-test-data: ## Remove test data (subjects/consumers starting with 'test-')
	@echo "WARNING: This will permanently delete test data!"
	@read -p "Are you sure you want to delete all test data? (y/N): " confirm; \
	if [ "$$confirm" = "y" ] || [ "$$confirm" = "Y" ]; then \
		echo "Deleting test schemas..."; \
		docker-compose exec -T db psql -U schema_user -d schema_registry -c "DELETE FROM schemas WHERE subject LIKE 'test-%';"; \
		echo "Deleting test consumers and their templates..."; \
		docker-compose exec -T db psql -U schema_user -d schema_registry -c "DELETE FROM consumers WHERE consumer_id LIKE 'test-%';"; \
		echo "Test data cleanup completed."; \
	else \
		echo "Cleanup cancelled."; \
	fi

# Advanced Diagnostics
db-schema-evolution: ## Analyze schema evolution patterns
	@echo "=== Schema Evolution Analysis ==="
	@docker-compose exec -T db psql -U schema_user -d schema_registry -c "SELECT subject, COUNT(*) as versions, MIN(created_at) as first_version, MAX(created_at) as latest_version, EXTRACT(EPOCH FROM (MAX(created_at) - MIN(created_at)))/3600 as evolution_hours FROM schemas GROUP BY subject HAVING COUNT(*) > 1 ORDER BY versions DESC;"

db-compatibility-checks: ## Show compatibility settings distribution
	@echo "=== Compatibility Settings Distribution ==="
	@docker-compose exec -T db psql -U schema_user -d schema_registry -c "SELECT compatibility, COUNT(*) as count FROM schemas GROUP BY compatibility ORDER BY count DESC;"

db-orphaned-data: ## Find orphaned records
	@echo "=== Orphaned Data Check ==="
	@echo "Templates without consumers:"
	@docker-compose exec -T db psql -U schema_user -d schema_registry -c "SELECT t.consumer_id, t.template_expression FROM transformation_templates t LEFT JOIN consumers c ON t.consumer_id = c.consumer_id WHERE c.consumer_id IS NULL;"

db-export-schema: ## Export current schema structure
	@echo "=== Database Schema Export ==="
	@docker-compose exec -T db pg_dump -U schema_user -d schema_registry --schema-only --no-owner --no-privileges > schema_export_$$(date +%Y%m%d_%H%M%S).sql
	@echo "Schema exported to schema_export_$$(date +%Y%m%d_%H%M%S).sql"

db-backup: ## Create database backup
	@echo "=== Database Backup ==="
	@docker-compose exec -T db pg_dump -U schema_user -d schema_registry --no-owner --no-privileges > db_backup_$$(date +%Y%m%d_%H%M%S).sql
	@echo "Database backup created: db_backup_$$(date +%Y%m%d_%H%M%S).sql"

db-activity: ## Show current database activity
	@echo "=== Current Database Activity ==="
	@docker-compose exec -T db psql -U schema_user -d schema_registry -c "SELECT pid, usename, client_addr, application_name, state, wait_event_type, wait_event, query_start::timestamp(0), now() - query_start as duration FROM pg_stat_activity WHERE state != 'idle' AND pid != pg_backend_pid() ORDER BY query_start;"

# Cleanup targets
clean: ## Clean build artifacts
	mvn clean
	docker-compose down -v

clean-all: ## Clean everything including Docker images
	docker-compose down -v --rmi all

# Health check
health: ## Check service health
	curl -f http://localhost:8080/actuator/health || echo "Service is not healthy"

# API documentation
docs: ## Open API documentation in browser
	@echo "API Documentation: http://localhost:8080/swagger-ui.html"
	@echo "OpenAPI Spec: http://localhost:8080/api-docs"

# Quick start
start: build up ## Build and start all services
	@echo "Services starting..."
	@sleep 10
	@make health
	@echo "API Documentation: http://localhost:8080/swagger-ui.html"

stop: down ## Stop all services