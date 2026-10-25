const { querySql } = require('./db.js');

console.log('Ticket columns:\n', querySql("SELECT COLUMN_NAME, DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'Ticket';"));
console.log('Promotion columns:\n', querySql("SELECT COLUMN_NAME, DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'Promotion';"));
