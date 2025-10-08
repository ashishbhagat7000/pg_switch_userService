#!/bin/bash

# Key Generation Script for PGSwitch Application
# This script generates secure cryptographic keys for the PGSwitch application
# Usage: ./generate-keys.sh [key-size] [count] [output-format]

set -e

# Default values
KEY_SIZE=256
COUNT=1
OUTPUT_FORMAT="env"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

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

# Function to show usage
show_usage() {
    echo "Usage: $0 [key-size] [count] [output-format]"
    echo ""
    echo "Parameters:"
    echo "  key-size      Key size in bits (128, 192, 256) [default: 256]"
    echo "  count         Number of keys to generate [default: 1]"
    echo "  output-format Output format (env, properties, json, raw) [default: env]"
    echo ""
    echo "Examples:"
    echo "  $0                    # Generate 1 AES-256 key in env format"
    echo "  $0 128 3 env          # Generate 3 AES-128 keys in env format"
    echo "  $0 256 1 properties   # Generate 1 AES-256 key in properties format"
    echo "  $0 192 2 json         # Generate 2 AES-192 keys in JSON format"
    echo ""
    echo "Output formats:"
    echo "  env         Environment variable format (export APP_CRYPTO_MASTER_KEY=...)"
    echo "  properties  Spring Boot properties format (app.crypto.master-key-base64=...)"
    echo "  json        JSON format for API usage"
    echo "  raw         Raw Base64 keys only"
}

# Function to validate key size
validate_key_size() {
    local size=$1
    if [[ ! "$size" =~ ^(128|192|256)$ ]]; then
        print_error "Invalid key size: $size"
        print_error "Key size must be 128, 192, or 256"
        exit 1
    fi
}

# Function to validate count
validate_count() {
    local count=$1
    if [[ ! "$count" =~ ^[0-9]+$ ]] || [ "$count" -lt 1 ] || [ "$count" -gt 10 ]; then
        print_error "Invalid count: $count"
        print_error "Count must be a positive integer between 1 and 10"
        exit 1
    fi
}

# Function to validate output format
validate_output_format() {
    local format=$1
    if [[ ! "$format" =~ ^(env|properties|json|raw)$ ]]; then
        print_error "Invalid output format: $format"
        print_error "Output format must be: env, properties, json, or raw"
        exit 1
    fi
}

# Function to generate a single key
generate_key() {
    local key_size=$1
    local key_bytes=$((key_size / 8))
    
    # Generate random bytes and encode to Base64
    if command -v openssl >/dev/null 2>&1; then
        openssl rand -base64 $key_bytes
    elif command -v base64 >/dev/null 2>&1; then
        # Fallback to /dev/urandom
        head -c $key_bytes /dev/urandom | base64
    else
        print_error "Neither openssl nor base64 command found"
        print_error "Please install openssl or base64 utility"
        exit 1
    fi
}

# Function to generate keys and output in env format
output_env_format() {
    local keys=("$@")
    local timestamp=$(date +"%Y-%m-%d %H:%M:%S")
    
    echo "# PGSwitch Master Keys Generated on $timestamp"
    echo "# Key Size: $KEY_SIZE bits"
    echo "# Count: $COUNT"
    echo ""
    
    for i in "${!keys[@]}"; do
        local key_num=$((i + 1))
        echo "export APP_CRYPTO_MASTER_KEY_${key_num}=${keys[i]}"
    done
    
    if [ $COUNT -eq 1 ]; then
        echo "export APP_CRYPTO_MASTER_KEY=${keys[0]}"
    fi
    
    echo ""
    echo "# For key rotation, you can use multiple keys:"
    echo "export APP_CRYPTO_KEY_ROTATION_IDS=key1,key2,key3"
}

# Function to generate keys and output in properties format
output_properties_format() {
    local keys=("$@")
    local timestamp=$(date +"%Y-%m-%d %H:%M:%S")
    
    echo "# PGSwitch Master Keys Generated on $timestamp"
    echo "# Key Size: $KEY_SIZE bits"
    echo "# Count: $COUNT"
    echo ""
    
    for i in "${!keys[@]}"; do
        local key_num=$((i + 1))
        echo "app.crypto.master-key-base64-${key_num}=${keys[i]}"
    done
    
    if [ $COUNT -eq 1 ]; then
        echo "app.crypto.master-key-base64=${keys[0]}"
    fi
    
    echo ""
    echo "# For key rotation:"
    echo "app.crypto.key-rotation-ids=key1,key2,key3"
}

# Function to generate keys and output in JSON format
output_json_format() {
    local keys=("$@")
    local timestamp=$(date +"%Y-%m-%d %H:%M:%S")
    
    echo "{"
    echo "  \"generatedAt\": \"$timestamp\","
    echo "  \"keySize\": $KEY_SIZE,"
    echo "  \"count\": $COUNT,"
    echo "  \"keys\": ["
    
    for i in "${!keys[@]}"; do
        local key_num=$((i + 1))
        echo -n "    {"
        echo -n "\"id\": \"key$key_num\","
        echo -n "\"key\": \"${keys[i]}\","
        echo -n "\"size\": $KEY_SIZE"
        if [ $i -lt $((${#keys[@]} - 1)) ]; then
            echo "},"
        else
            echo "}"
        fi
    done
    
    echo "  ]"
    echo "}"
}

# Function to generate keys and output in raw format
output_raw_format() {
    local keys=("$@")
    
    for key in "${keys[@]}"; do
        echo "$key"
    done
}

# Parse command line arguments
if [ $# -gt 0 ]; then
    if [ "$1" = "-h" ] || [ "$1" = "--help" ]; then
        show_usage
        exit 0
    fi
    
    KEY_SIZE=$1
fi

if [ $# -gt 1 ]; then
    COUNT=$2
fi

if [ $# -gt 2 ]; then
    OUTPUT_FORMAT=$3
fi

# Validate inputs
validate_key_size $KEY_SIZE
validate_count $COUNT
validate_output_format $OUTPUT_FORMAT

# Print header
print_info "PGSwitch Key Generation Script"
print_info "=============================="
print_info "Key Size: $KEY_SIZE bits"
print_info "Count: $COUNT"
print_info "Output Format: $OUTPUT_FORMAT"
print_info ""

# Check for required tools
if ! command -v openssl >/dev/null 2>&1 && ! command -v base64 >/dev/null 2>&1; then
    print_error "Required tools not found"
    print_error "Please install openssl or base64 utility"
    exit 1
fi

# Generate keys
print_info "Generating $COUNT AES-$KEY_SIZE key(s)..."
keys=()

for ((i=1; i<=COUNT; i++)); do
    key=$(generate_key $KEY_SIZE)
    keys+=("$key")
    print_success "Generated key $i"
done

print_info ""
print_info "Output:"
print_info "======="

# Output in requested format
case $OUTPUT_FORMAT in
    "env")
        output_env_format "${keys[@]}"
        ;;
    "properties")
        output_properties_format "${keys[@]}"
        ;;
    "json")
        output_json_format "${keys[@]}"
        ;;
    "raw")
        output_raw_format "${keys[@]}"
        ;;
esac

print_info ""
print_success "Key generation completed successfully!"

# Security recommendations
print_warning "Security Recommendations:"
print_warning "========================="
print_warning "1. Store keys securely (environment variables, secret managers)"
print_warning "2. Never commit keys to version control"
print_warning "3. Use different keys for different environments"
print_warning "4. Rotate keys regularly (recommended: every 90 days)"
print_warning "5. Monitor key usage and access"
print_warning "6. Use proper access controls for key management endpoints"

# Usage instructions
print_info ""
print_info "Usage Instructions:"
print_info "==================="
print_info "1. Copy the generated key(s) to your environment variables"
print_info "2. Set APP_CRYPTO_MASTER_KEY in your application environment"
print_info "3. For production, use a proper key management system (AWS KMS, Azure Key Vault)"
print_info "4. Test key functionality before deploying to production"
