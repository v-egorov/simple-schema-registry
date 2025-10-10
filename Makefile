# JSON Schema Registry and Transformation Service - Development Makefile

.PHONY: help build clean test up down logs db-migrate db-seed restart shell

# Default target
help: ## Show this help message
	@echo "JSON Schema Registry and Transformation Service"
	@echo ""
	@echo "Available targets:"
	@awk 'BEGIN {FS = ":.*?## "} /^[a-zA-Z_-]+:.*?## / {printf "  %-15s %s\n", $$1, $$2}' $(MAKEFILE_LIST)

# Build targets
build: ## Build the application JAR
	./mvnw clean package -DskipTests

build-docker: ## Build Docker image
	docker-compose build

# Test targets
test: ## Run all tests
	./mvnw test

test-unit: ## Run unit tests only
	./mvnw test -Dtest="*Test"

test-integration: ## Run integration tests only
	./mvnw test -Dtest="*IT"

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
	./mvnw flyway:migrate

db-clean: ## Clean database (drop all objects)
	./mvnw flyway:clean

db-info: ## Show database migration info
	./mvnw flyway:info

db-seed: ## Seed database with sample data (if implemented)
	@echo "Database seeding not yet implemented"

# Development targets
run: ## Run the application locally (requires PostgreSQL running)
	./mvnw spring-boot:run

run-dev: ## Run in development mode with live reload
	./mvnw spring-boot:run -Dspring-boot.run.fork=false

shell: ## Open shell in running app container
	docker-compose exec app sh

shell-db: ## Open PostgreSQL shell
	docker-compose exec db psql -U schema_user -d schema_registry

# Cleanup targets
clean: ## Clean build artifacts
	./mvnw clean
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