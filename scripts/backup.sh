#!/bin/bash

# Curtain Production - MySQL Backup Script
# This script creates daily backups of the MySQL database

# Configuration
BACKUP_DIR="/opt/curtain/backups"
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="curtain_backup_$DATE.sql"
CONTAINER_NAME="curtain-mysql-db-1"
DB_NAME="curtain_db"
DB_USER="root"
DB_PASSWORD="root_password"
RETENTION_DAYS=7

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${YELLOW}=== Curtain Production Backup ===${NC}"
echo "Starting backup at $(date)"

# Create backup directory if it doesn't exist
mkdir -p $BACKUP_DIR

# Check if MySQL container is running
if ! docker ps | grep -q $CONTAINER_NAME; then
    echo -e "${RED}ERROR: MySQL container is not running!${NC}"
    exit 1
fi

# Create backup
echo "Creating backup: $BACKUP_FILE"
if docker exec $CONTAINER_NAME mysqldump -u $DB_USER -p$DB_PASSWORD $DB_NAME > $BACKUP_DIR/$BACKUP_FILE 2>/dev/null; then
    
    # Compress backup
    gzip $BACKUP_DIR/$BACKUP_FILE
    BACKUP_SIZE=$(du -h $BACKUP_DIR/$BACKUP_FILE.gz | cut -f1)
    
    echo -e "${GREEN}✓ Backup successful!${NC}"
    echo "  File: $BACKUP_FILE.gz"
    echo "  Size: $BACKUP_SIZE"
    echo "  Location: $BACKUP_DIR"
    
    # Delete old backups
    echo "Cleaning up old backups (older than $RETENTION_DAYS days)..."
    find $BACKUP_DIR -name "curtain_backup_*.sql.gz" -mtime +$RETENTION_DAYS -delete
    
    BACKUP_COUNT=$(ls -1 $BACKUP_DIR/curtain_backup_*.sql.gz 2>/dev/null | wc -l)
    echo -e "${GREEN}✓ Cleanup complete. Total backups: $BACKUP_COUNT${NC}"
    
else
    echo -e "${RED}✗ Backup failed!${NC}"
    exit 1
fi

echo "Backup completed at $(date)"
echo "================================"
