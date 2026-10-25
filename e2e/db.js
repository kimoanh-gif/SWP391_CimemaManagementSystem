const { execSync } = require('child_process');

function querySql(sql) {
  const cmd = `& "C:\\Program Files\\Microsoft SQL Server\\Client SDK\\ODBC\\170\\Tools\\Binn\\SQLCMD.EXE" -S "localhost\\SQLEXPRESS" -d "CinemaManagementDB" -E -W -s "|" -Q "${sql.replace(/"/g, '""')}"`;
  const output = execSync(cmd, { shell: 'powershell.exe', encoding: 'utf-8' });
  return output.trim();
}

module.exports = { querySql };

if (require.main === module) {
  console.log('Testing db.js query...');
  const res = querySql('SELECT MovieID, Title, Duration FROM dbo.Movie;');
  console.log(res);
}
