-- dedicated user for the app
CREATE DATABASE IF NOT EXISTS securedoc;
CREATE USER 'securedoc_user'@'localhost' IDENTIFIED BY 'securedoc!123';
GRANT ALL PRIVILEGES ON securedoc.* TO 'securedoc_user'@'localhost';
FLUSH PRIVILEGES;
exit;