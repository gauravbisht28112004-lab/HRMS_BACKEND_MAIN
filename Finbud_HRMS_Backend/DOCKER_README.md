# Finbud HRMS - Docker Setup

## Prerequisites

- Docker Engine 24.0+
- Docker Compose 2.0+
- 4GB+ RAM available

## Quick Start

```bash
# Clone and navigate to project
cd finbud-hrms

# Set environment variables (optional)
export OPENAI_API_KEY="your-openai-api-key"
export MAIL_USERNAME="your-email@gmail.com"
export MAIL_PASSWORD="your-app-password"

# Start all services
docker-compose up -d

# View logs
docker-compose logs -f app

# Stop services
docker-compose down

# Stop and remove volumes (clears database)
docker-compose down -v
```

## Services

| Service | Port | Description |
|---------|------|-------------|
| finbud-hrms | 8080 | Spring Boot Application |
| postgres | 5432 | PostgreSQL with pgvector |
| redis | 6379 | Redis Cache |

## API Documentation

Once running, access Swagger UI at: http://localhost:8080/swagger-ui.html

## Database Access

```bash
# Connect to PostgreSQL
docker exec -it finbud-postgres psql -U postgres -d finbud_hrms
```

## Production Deployment

For production deployment:

1. Update `application-prod.properties`
2. Use environment-specific `.env` file
3. Configure SSL certificates
4. Set up monitoring and logging

```bash
# Production startup
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d
```
