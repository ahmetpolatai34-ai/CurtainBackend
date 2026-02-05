#!/bin/bash

# Curtain Production - Manual Backup Script
# Use this for immediate backups before major changes

# Configuration
BACKUP_DIR="/opt/curtain/backups"
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="curtain_manual_$DATE.sql"
CONTAINER_NAME="curtain-mysql-db-1"
DB_NAME="curtain_db"
DB_USER="root"
DB_PASSWORD="root_password"

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${YELLOW}=== Manual Backup ===${NC}"

# Create backup directory if it doesn't exist
mkdir -p $BACKUP_DIR

# Check if MySQL container is running
if ! docker ps | grep -q $CONTAINER_NAME; then
    echo -e "${RED}ERROR: MySQL container is not running!${NC}"
    exit 1
fi

# Create backup
echo "Creating manual backup..."
if docker exec $CONTAINER_NAME mysqldump -u $DB_USER -p$DB_PASSWORD $DB_NAME > $BACKUP_DIR/$BACKUP_FILE 2>/dev/null; then
    
    # Compress backup
    gzip $BACKUP_DIR/$BACKUP_FILE
    BACKUP_SIZE=$(du -h $BACKUP_DIR/$BACKUP_FILE.gz | cut -f1)
    
    echo -e "${GREEN}✓ Manual backup successful!${NC}"
    echo "  File: $BACKUP_FILE.gz"
    echo "  Size: $BACKUP_SIZE"
    echo "  Location: $BACKUP_DIR/$BACKUP_FILE.gz"
    
else
    echo -e "${RED}✗ Backup failed!${NC}"
    exit 1
fi
