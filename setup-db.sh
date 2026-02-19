#!/bin/bash
# gonERP Database Setup Script
# Assumes 'steve' is already a PostgreSQL superuser

echo "Setting up gonERP PostgreSQL database..."

psql -U steve -d postgres << 'EOF'
-- Create database if not exists
SELECT 'CREATE DATABASE gonerp'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'gonerp')\gexec

\echo 'Database setup complete!'
EOF

echo ""
echo "Done! You can now start the backend with: cd backend && mvn spring-boot:run"
