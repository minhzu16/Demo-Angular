# Fix Cart Database Schema
# Auto-run SQL commands to allow NULL for userId and sessionId

Write-Host "=== Fixing Cart Database Schema ===" -ForegroundColor Cyan

# Database connection details
$host_db = "127.0.0.1"
$port_db = "3307"
$user_db = "sa"
$pass_db = "123"
$database = "mvp_ecommerce"

# SQL commands
$sql = @"
USE $database;
ALTER TABLE carts MODIFY COLUMN user_id INT NULL COMMENT 'null if guest user';
ALTER TABLE carts MODIFY COLUMN session_id VARCHAR(255) NULL COMMENT 'null if logged user';
SELECT 'Schema updated successfully!' as message;
"@

Write-Host "Connecting to MySQL at $host_db`:$port_db..." -ForegroundColor Yellow

# Run SQL command
try {
    $sql | mysql -h $host_db -P $port_db -u $user_db -p$pass_db 2>&1
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "`n✓ Database schema fixed successfully!" -ForegroundColor Green
        Write-Host "  - user_id column now allows NULL" -ForegroundColor Green
        Write-Host "  - session_id column now allows NULL" -ForegroundColor Green
        Write-Host "`nYou can now restart the Cart service." -ForegroundColor Cyan
    } else {
        Write-Host "`n✗ Failed to update database schema" -ForegroundColor Red
        Write-Host "Please check if MySQL is running and credentials are correct." -ForegroundColor Yellow
    }
} catch {
    Write-Host "`n✗ Error: $_" -ForegroundColor Red
    Write-Host "`nMake sure MySQL client is installed and in PATH" -ForegroundColor Yellow
    Write-Host "Or run manually:" -ForegroundColor Yellow
    Write-Host "  mysql -h $host_db -P $port_db -u $user_db -p$pass_db" -ForegroundColor Gray
    Write-Host "  USE $database;" -ForegroundColor Gray
    Write-Host "  ALTER TABLE carts MODIFY COLUMN user_id INT NULL;" -ForegroundColor Gray
    Write-Host "  ALTER TABLE carts MODIFY COLUMN session_id VARCHAR(255) NULL;" -ForegroundColor Gray
}

Write-Host "`nPress any key to exit..." -ForegroundColor Gray
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
