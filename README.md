# Alto-Shaam Master Label System (Backend)

Spring Boot 3.5 / Java 21 / PostgreSQL. Generates B-10 Master Labels (6 x 4 in, two per A4 page) as PDF.

## Run
    mvn spring-boot:run          # needs PostgreSQL, see application.properties
    docker build -t label-system . && docker run -p 8081:8081 -e DB_PASSWORD=... -e DB_URL=... label-system

Health check: `GET /actuator/health`

## Environment variables
| Variable | Default | Purpose |
|---|---|---|
| DB_URL / DB_USERNAME / DB_PASSWORD | localhost:5433 / postgres / 1234 | Database (always override in production) |
| DB_POOL_SIZE | 20 | Max DB connections |
| PORT | 8081 | HTTP port |
| CORS_ORIGINS | localhost:5173,... | Allowed front-end origins |
| LABEL_STORAGE_DIR | ./data/generated | Where PDFs are stored (use a persistent volume) |
| LABEL_MAX_ROWS | 500 | Max rows per request |
| LABEL_MAX_PHYSICAL | 5000 | Max physical labels per request |
| PDF_MAX_CONCURRENT | 4 | PDFs rendered at the same time |
| PDF_ACQUIRE_TIMEOUT | 30 | Seconds a request waits for a render slot before HTTP 503 |
| DDL_AUTO | update | Hibernate schema mode (use `validate` once the schema is stable) |
