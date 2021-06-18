const sqlite = require('sqlite3');
let db = new sqlite.Database('./sample.db');

let username = 'Dispetcher';
let password = 'Start';

db.run('INSERT INTO despetcheri (username, password) VALUES (?, ?);', [username, password], function(err) {
    if (err) console.log(err);
    else console.log('Добавлено');
});