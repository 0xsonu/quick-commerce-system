-- Create databases for each service
CREATE DATABASE IF NOT EXISTS auth_service;
CREATE DATABASE IF NOT EXISTS user_service;
CREATE DATABASE IF NOT EXISTS inventory_service;
CREATE DATABASE IF NOT EXISTS cart_service;
CREATE DATABASE IF NOT EXISTS order_service;
CREATE DATABASE IF NOT EXISTS payment_service;
CREATE DATABASE IF NOT EXISTS shipping_service;

-- Create users and grant permissions
CREATE USER IF NOT EXISTS 'auth_user'@'%' IDENTIFIED BY 'auth_password';
CREATE USER IF NOT EXISTS 'user_user'@'%' IDENTIFIED BY 'user_password';
CREATE USER IF NOT EXISTS 'inventory_user'@'%' IDENTIFIED BY 'inventory_password';
CREATE USER IF NOT EXISTS 'cart_user'@'%' IDENTIFIED BY 'cart_password';
CREATE USER IF NOT EXISTS 'order_user'@'%' IDENTIFIED BY 'order_password';
CREATE USER IF NOT EXISTS 'payment_user'@'%' IDENTIFIED BY 'payment_password';
CREATE USER IF NOT EXISTS 'shipping_user'@'%' IDENTIFIED BY 'shipping_password';

-- Grant permissions
GRANT ALL PRIVILEGES ON auth_service.* TO 'auth_user'@'%';
GRANT ALL PRIVILEGES ON user_service.* TO 'user_user'@'%';
GRANT ALL PRIVILEGES ON inventory_service.* TO 'inventory_user'@'%';
GRANT ALL PRIVILEGES ON cart_service.* TO 'cart_user'@'%';
GRANT ALL PRIVILEGES ON order_service.* TO 'order_user'@'%';
GRANT ALL PRIVILEGES ON payment_service.* TO 'payment_user'@'%';
GRANT ALL PRIVILEGES ON shipping_service.* TO 'shipping_user'@'%';

FLUSH PRIVILEGES;