# Agent Instructions for JSON Schema Registry

## Build Commands
- `mvn clean compile` - Compile the project
- `mvn clean package` - Build JAR with tests
- `mvn clean package -DskipTests` - Build JAR without tests
- `mvn spring-boot:run` - Run the application locally

## Test Commands
- `mvn test` - Run all tests
- `mvn test -Dtest=ClassName` - Run single test class
- `mvn test -Dtest=ClassName#testMethod` - Run single test method
- `mvn verify` - Run tests with integration tests

## Code Style Guidelines
- **Language**: Java 17 with Spring Boot 3.1.4
- **Imports**: Organize imports alphabetically, separate java.* from others
- **Naming**: CamelCase for classes/methods, UPPER_SNAKE_CASE for constants
- **Types**: Use generics, avoid raw types, prefer interfaces over implementations
- **Error Handling**: Use custom exceptions, validate inputs with Jakarta Validation
- **Formatting**: 4-space indentation, 120 char line limit, trailing newlines
- **Documentation**: Use JavaDoc for public APIs, keep methods focused and small