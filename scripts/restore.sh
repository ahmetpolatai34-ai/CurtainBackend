#!/bin/bash

# Curtain Production - MySQL Restore Script
# This script restores the database from a backup file

# Configuration
BACKUP_DIR="/opt/curtain/backups"
CONTAINER_NAME="mysql-db-1"
DB_NAME="curtain_db"
DB_USER="root"
DB_PASSWORD="root_password"

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${YELLOW}=== Curtain Production Database Restore ===${NC}"

# Check if backup directory exists
if [ ! -d "$BACKUP_DIR" ]; then
    echo -e "${RED}ERROR: Backup directory not found: $BACKUP_DIR${NC}"
    exit 1
fi

# List available backups
echo "Available backups:"
echo "================================"
ls -lh $BACKUP_DIR/curtain_backup_*.sql.gz 2>/dev/null | awk '{print NR". "$9" ("$5")"}'

if [ $? -ne 0 ]; then
    echo -e "${RED}No backups found!${NC}"
    exit 1
fi

echo "================================"

# If backup file provided as argument
if [ -n "$1" ]; then
    BACKUP_FILE="$1"
else
    # Ask user to select backup
    echo -e "${YELLOW}Enter the backup filename (or press Enter for latest):${NC}"
    read BACKUP_INPUT
    
    if [ -z "$BACKUP_INPUT" ]; then
        # Use latest backup
        BACKUP_FILE=$(ls -t $BACKUP_DIR/curtain_backup_*.sql.gz 2>/dev/null | head -1)
        echo "Using latest backup: $(basename $BACKUP_FILE)"
    else
        BACKUP_FILE="$BACKUP_DIR/$BACKUP_INPUT"
    fi
fi

# Check if backup file exists
if [ ! -f "$BACKUP_FILE" ]; then
    echo -e "${RED}ERROR: Backup file not found: $BACKUP_FILE${NC}"
    exit 1
fi

# Warning
echo -e "${RED}WARNING: This will REPLACE all current data in the database!${NC}"
echo -e "${YELLOW}Are you sure you want to continue? (yes/no):${NC}"
read CONFIRM

if [ "$CONFIRM" != "yes" ]; then
    echo "Restore cancelled."
    exit 0
fi

# Check if MySQL container is running
if ! docker ps | grep -q $CONTAINER_NAME; then
    echo -e "${RED}ERROR: MySQL container is not running!${NC}"
    echo "Starting MySQL container..."
    cd /opt/curtain
    docker compose up -d mysql-db
    sleep 10
fi

# Restore backup
echo "Restoring database from: $(basename $BACKUP_FILE)"

# Decompress and restore
if gunzip -c $BACKUP_FILE | docker exec -i $CONTAINER_NAME mysql -u $DB_USER -p$DB_PASSWORD $DB_NAME; then
    echo -e "${GREEN}✓ Database restored successfully!${NC}"
    echo "Restored from: $(basename $BACKUP_FILE)"
else
    echo -e "${RED}✗ Restore failed!${NC}"
    exit 1
fi

echo "================================"
