@echo off
set "DB_URL=jdbc:mysql://localhost:3307/edifiq?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
set "DB_USER=root"
set "DB_PASSWORD=root"
set "RABBITMQ_HOST=localhost"
set "RABBITMQ_PORT=5672"
set "RABBITMQ_USER=guest"
set "RABBITMQ_PASSWORD=guest"

echo Development environment variables loaded:
set DB_URL
echo   DB_USER=%DB_USER%
echo   RABBITMQ_HOST=%RABBITMQ_HOST%
echo   RABBITMQ_PORT=%RABBITMQ_PORT%
