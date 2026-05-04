CREATE DATABASE IF NOT EXISTS securedoc
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- dedicated user for the app
CREATE USER 'securedoc_user'@'localhost' IDENTIFIED BY 'securedoc!123';
GRANT ALL PRIVILEGES ON securedoc.* TO 'securedoc_user'@'localhost';
FLUSH PRIVILEGES;
exit;