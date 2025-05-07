#!/bin/bash

# Build and start the containers
docker-compose up -d

# Display container status
docker-compose ps

echo "-------------------------------------"
echo "User Manager environment is running!"
echo "-------------------------------------"
echo "API: http://localhost:8080"
echo "Adminer: http://localhost:8081"
echo "  - System: MariaDB"
echo "  - Server: db"
echo "  - Username: user_manager"
echo "  - Password: user_manager_password"
echo "  - Database: user_manager"
echo "-------------------------------------"
echo "To stop the environment, run: docker-compose down"
echo "To view logs, run: docker-compose logs -f"