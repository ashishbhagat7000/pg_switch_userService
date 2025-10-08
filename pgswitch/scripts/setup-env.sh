#!/bin/bash

# Environment Setup Script for PGSwitch Application
# This script helps you set up the required environment variables

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to generate master key
generate_master_key() {
    print_info "Generating AES-256 master key..."
    
    if command -v openssl >/dev/null 2>&1; then
        MASTER_KEY=$(openssl rand -base64 32)
    elif command -v base64 >/dev/null 2>&1; then
        MASTER_KEY=$(head -c 32 /dev/urandom | base64)
    else
        print_error "Neither openssl nor base64 command found"
        print_error "Please install openssl or base64 utility"
        exit 1
    fi
    
    print_success "Master key generated successfully"
    echo "$MASTER_KEY"
}

# Function to create .env file
create_env_file() {
    local master_key=$1
    
    print_info "Creating .env file..."
    
    cat > .env << EOF
# PGSwitch Application Environment Variables
# Generated on $(date)

# =============================================================================
# MONGODB DATABASE CONFIGURATION
# =============================================================================

# MongoDB Connection URI (Internal - Default)
MONGODB_URI=mongodb://mongo:root@fintechqr-onboarding-pgswitch-ms1zvh:27017

# MongoDB Database Name
MONGODB_DATABASE=pgswitch

# MongoDB Authentication Credentials
MONGODB_USERNAME=mongo
MONGODB_PASSWORD=root
MONGODB_AUTH_DATABASE=admin

# MongoDB Connection Pool Settings
MONGODB_POOL_SIZE=100
MONGODB_MIN_POOL_SIZE=5
MONGODB_MAX_WAIT_TIME=120000
MONGODB_MAX_IDLE_TIME=0
MONGODB_MAX_LIFE_TIME=0

# =============================================================================
# ENCRYPTION CONFIGURATION (CRITICAL FOR SECURITY)
# =============================================================================

# Master Encryption Key (Base64 encoded 256-bit AES key)
APP_CRYPTO_MASTER_KEY=$master_key

# Key Rotation IDs (comma-separated list of key IDs for rotation)
APP_CRYPTO_KEY_ROTATION_IDS=

# =============================================================================
# FILE STORAGE CONFIGURATION
# =============================================================================

# Base directory for file storage (KYC documents)
APP_STORAGE_DIR=./storage

# KYC documents subdirectory
APP_KYC_DIR=./storage/kyc

# =============================================================================
# APPLICATION CONFIGURATION
# =============================================================================

# Application Name
SPRING_APPLICATION_NAME=pgswitch

# Server Port
SERVER_PORT=9095

# =============================================================================
# LOGGING CONFIGURATION
# =============================================================================

# MongoDB Query Logging (set to INFO or WARN for production)
LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_DATA_MONGODB_CORE_MONGOTEMPLATE=DEBUG
LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_DATA_MONGODB_CORE_QUERY_QUERY=DEBUG

# Application Logging Level
LOGGING_LEVEL_COM_VOL_PGSWITCH=INFO

# =============================================================================
# SECURITY CONFIGURATION
# =============================================================================

# Security Headers
SERVER_FORWARD_HEADERS_STRATEGY=framework

# Actuator Endpoints (comma-separated)
MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,info,metrics

# Health Check Probes
MANAGEMENT_ENDPOINT_HEALTH_PROBES_ENABLED=true

# =============================================================================
# FILE UPLOAD LIMITS
# =============================================================================

# Maximum file size per upload
SPRING_SERVLET_MULTIPART_MAX_FILE_SIZE=10MB

# Maximum request size (including all files)
SPRING_SERVLET_MULTIPART_MAX_REQUEST_SIZE=25MB

# =============================================================================
# DEVELOPMENT/DEBUG CONFIGURATION
# =============================================================================

# Enable debug mode (set to false for production)
DEBUG_MODE=true

# Enable MongoDB query logging (set to false for production)
MONGODB_DEBUG=true
EOF

    print_success ".env file created successfully"
}

# Function to create storage directories
create_storage_directories() {
    print_info "Creating storage directories..."
    
    mkdir -p ./storage/kyc
    
    print_success "Storage directories created"
}

# Function to test MongoDB connection
test_mongodb_connection() {
    print_info "Testing MongoDB connection..."
    
    if command -v mongosh >/dev/null 2>&1; then
        if mongosh "mongodb://mongo:root@fintechqr-onboarding-pgswitch-ms1zvh:27017/pgswitch" --eval "db.runCommand('ping')" >/dev/null 2>&1; then
            print_success "MongoDB connection test successful"
        else
            print_warning "MongoDB connection test failed - please check your credentials"
        fi
    else
        print_warning "mongosh not found - skipping MongoDB connection test"
        print_info "You can test the connection manually when starting the application"
    fi
}

# Function to show usage
show_usage() {
    echo "Usage: $0 [options]"
    echo ""
    echo "Options:"
    echo "  -h, --help     Show this help message"
    echo "  -k, --key      Generate master key only"
    echo "  -e, --env      Create .env file only"
    echo "  -a, --all      Generate key and create .env file (default)"
    echo ""
    echo "Examples:"
    echo "  $0              # Generate key and create .env file"
    echo "  $0 --key        # Generate master key only"
    echo "  $0 --env        # Create .env file only"
}

# Main function
main() {
    local generate_key=true
    local create_env=true
    local master_key=""
    
    # Parse command line arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            -h|--help)
                show_usage
                exit 0
                ;;
            -k|--key)
                generate_key=true
                create_env=false
                shift
                ;;
            -e|--env)
                generate_key=false
                create_env=true
                shift
                ;;
            -a|--all)
                generate_key=true
                create_env=true
                shift
                ;;
            *)
                print_error "Unknown option: $1"
                show_usage
                exit 1
                ;;
        esac
    done
    
    print_info "PGSwitch Environment Setup Script"
    print_info "=================================="
    
    # Generate master key if requested
    if [ "$generate_key" = true ]; then
        master_key=$(generate_master_key)
        echo ""
        print_info "Generated Master Key:"
        echo "$master_key"
        echo ""
    fi
    
    # Create .env file if requested
    if [ "$create_env" = true ]; then
        if [ -z "$master_key" ]; then
            print_warning "No master key provided - using empty key"
            print_warning "Please update APP_CRYPTO_MASTER_KEY in .env file"
            master_key=""
        fi
        create_env_file "$master_key"
    fi
    
    # Create storage directories
    create_storage_directories
    
    # Test MongoDB connection
    test_mongodb_connection
    
    print_info ""
    print_success "Environment setup completed!"
    print_info ""
    print_info "Next steps:"
    print_info "1. Review the .env file and update any values as needed"
    print_info "2. Start the application: mvn spring-boot:run"
    print_info "3. Test the API: curl http://localhost:9095/actuator/health"
    print_info ""
    print_warning "Security reminders:"
    print_warning "- Never commit the .env file to version control"
    print_warning "- Use different keys for different environments"
    print_warning "- Rotate keys regularly (recommended: every 90 days)"
}

# Run main function
main "$@"
