@echo off
REM Environment Setup Script for PGSwitch Application (Windows)
REM This script helps you set up the required environment variables

echo [INFO] PGSwitch Environment Setup Script
echo [INFO] ==================================

REM Generate master key using PowerShell
echo [INFO] Generating AES-256 master key...
for /f %%i in ('powershell -command "[System.Convert]::ToBase64String([System.Security.Cryptography.RandomNumberGenerator]::GetBytes(32))"') do set MASTER_KEY=%%i

echo [SUCCESS] Master key generated successfully
echo [INFO] Generated Master Key:
echo %MASTER_KEY%
echo.

REM Create .env file
echo [INFO] Creating .env file...
(
echo # PGSwitch Application Environment Variables
echo # Generated on %date% %time%
echo.
echo # =============================================================================
echo # MONGODB DATABASE CONFIGURATION
echo # =============================================================================
echo.
echo # MongoDB Connection URI ^(Internal - Default^)
echo MONGODB_URI=mongodb://mongo:root@fintechqr-onboarding-pgswitch-ms1zvh:27017
echo.
echo # MongoDB Database Name
echo MONGODB_DATABASE=pgswitch
echo.
echo # MongoDB Authentication Credentials
echo MONGODB_USERNAME=mongo
echo MONGODB_PASSWORD=root
echo MONGODB_AUTH_DATABASE=admin
echo.
echo # MongoDB Connection Pool Settings
echo MONGODB_POOL_SIZE=100
echo MONGODB_MIN_POOL_SIZE=5
echo MONGODB_MAX_WAIT_TIME=120000
echo MONGODB_MAX_IDLE_TIME=0
echo MONGODB_MAX_LIFE_TIME=0
echo.
echo # =============================================================================
echo # ENCRYPTION CONFIGURATION ^(CRITICAL FOR SECURITY^)
echo # =============================================================================
echo.
echo # Master Encryption Key ^(Base64 encoded 256-bit AES key^)
echo APP_CRYPTO_MASTER_KEY=%MASTER_KEY%
echo.
echo # Key Rotation IDs ^(comma-separated list of key IDs for rotation^)
echo APP_CRYPTO_KEY_ROTATION_IDS=
echo.
echo # =============================================================================
echo # FILE STORAGE CONFIGURATION
echo # =============================================================================
echo.
echo # Base directory for file storage ^(KYC documents^)
echo APP_STORAGE_DIR=./storage
echo.
echo # KYC documents subdirectory
echo APP_KYC_DIR=./storage/kyc
echo.
echo # =============================================================================
echo # APPLICATION CONFIGURATION
echo # =============================================================================
echo.
echo # Application Name
echo SPRING_APPLICATION_NAME=pgswitch
echo.
echo # Server Port
echo SERVER_PORT=9095
echo.
echo # =============================================================================
echo # LOGGING CONFIGURATION
echo # =============================================================================
echo.
echo # MongoDB Query Logging ^(set to INFO or WARN for production^)
echo LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_DATA_MONGODB_CORE_MONGOTEMPLATE=DEBUG
echo LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_DATA_MONGODB_CORE_QUERY_QUERY=DEBUG
echo.
echo # Application Logging Level
echo LOGGING_LEVEL_COM_VOL_PGSWITCH=INFO
echo.
echo # =============================================================================
echo # SECURITY CONFIGURATION
echo # =============================================================================
echo.
echo # Security Headers
echo SERVER_FORWARD_HEADERS_STRATEGY=framework
echo.
echo # Actuator Endpoints ^(comma-separated^)
echo MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,info,metrics
echo.
echo # Health Check Probes
echo MANAGEMENT_ENDPOINT_HEALTH_PROBES_ENABLED=true
echo.
echo # =============================================================================
echo # FILE UPLOAD LIMITS
echo # =============================================================================
echo.
echo # Maximum file size per upload
echo SPRING_SERVLET_MULTIPART_MAX_FILE_SIZE=10MB
echo.
echo # Maximum request size ^(including all files^)
echo SPRING_SERVLET_MULTIPART_MAX_REQUEST_SIZE=25MB
echo.
echo # =============================================================================
echo # DEVELOPMENT/DEBUG CONFIGURATION
echo # =============================================================================
echo.
echo # Enable debug mode ^(set to false for production^)
echo DEBUG_MODE=true
echo.
echo # Enable MongoDB query logging ^(set to false for production^)
echo MONGODB_DEBUG=true
) > .env

echo [SUCCESS] .env file created successfully

REM Create storage directories
echo [INFO] Creating storage directories...
if not exist "storage" mkdir storage
if not exist "storage\kyc" mkdir storage\kyc

echo [SUCCESS] Storage directories created

echo.
echo [SUCCESS] Environment setup completed!
echo.
echo [INFO] Next steps:
echo [INFO] 1. Review the .env file and update any values as needed
echo [INFO] 2. Start the application: mvn spring-boot:run
echo [INFO] 3. Test the API: curl http://localhost:9095/actuator/health
echo.
echo [WARNING] Security reminders:
echo [WARNING] - Never commit the .env file to version control
echo [WARNING] - Use different keys for different environments
echo [WARNING] - Rotate keys regularly (recommended: every 90 days)

pause
