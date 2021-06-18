const express = require('express')
const cookie_parser = require('cookie-parser')
const sqlite = require('sqlite3')
const path = require('path');
const url = require('url');
const jwt = require('jsonwebtoken');

const secret = 'secret';
const port = 3000

let db = new sqlite.Database('./sample.db', (err) => {
  if (err) {
    return console.error(err.message);
  }
  db.run('CREATE TABLE IF NOT EXISTS despetcheri (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT NOT NULL UNIQUE, password TEXT NOT NULL);');
  db.run('CREATE TABLE IF NOT EXISTS voditeli (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT NOT NULL UNIQUE, password TEXT NOT NULL, phone TEXT NOT NULL, car TEXT NOT NULL);');
  db.run('CREATE TABLE IF NOT EXISTS poezdki (id INTEGER PRIMARY KEY AUTOINCREMENT, from_place TEXT NOT NULL, to_place TEXT NOT NULL, km FLOAT NOT NULL, price INTEGER NOT NULL, phone TEXT NOT NULL, status TEXT NOT NULL, voditel_id INTEGER);');
});

const app = express();

app.set('view engine', 'hbs');
app.set('views', path.join(__dirname, '/views'));

const despetcheriServer = express();

despetcheriServer.use(express.urlencoded({ extended: true }));
despetcheriServer.use(express.json());
despetcheriServer.use(cookie_parser('PlaceSecret'))

function authDispetcher(req, res, next) {
  var userId = req.signedCookies.user;
  if (userId) {
    try {
      db.get('SELECT * FROM despetcheri WHERE id = ?;', [userId], (err, row) => {
        if (err || !row) res.redirect('/dispetcheri/login');
        else {
          next()
        }
      });
    } catch(err) {
      res.redirect('/dispetcheri/login');
    }
  } else {
    res.redirect('/dispetcheri/login');
  }
}

despetcheriServer.get('/login', (req, res) => {
  res.render('login');
})

despetcheriServer.post('/login', (req, res) => {
  db.get('SELECT * FROM despetcheri WHERE username = ? AND password = ?;', [req.body.username, req.body.password], (err, row) => {
    if (err || !row) {
      res.render('login', { error: 'Неправильный логин или пароль!' });
    }
    else {
      res.cookie('user', row.id, {signed: true})
      res.redirect('/dispetcheri/');
    }
  })
})

despetcheriServer.get('/', authDispetcher, (req, res) => {
  db.all('SELECT * FROM poezdki', (err, rows) => {
    if (err) {
      res.redirect(url.format({
        pathname:"/dispetcheri/error",
        query: {
          "error": err.message
        }
      }));
    }
    else res.render('index', { poezdki: rows });
  })
})

despetcheriServer.get('/calculate', authDispetcher, (req, res) => {
  // res.sendFile(path.join(__dirname, '/views/calculate.html'));
  res.render('calculate');
})

despetcheriServer.post('/calculate', authDispetcher, (req, res) => {
  db.run('INSERT INTO poezdki (from_place, to_place, km, price, phone, status) VALUES (?, ?, ?, ?, ?, ?)', [req.body.from, req.body.to, req.body.km, req.body.price, req.body.phone, 'Ожидает водителя'], (err) => {
    if (err) {
      console.error(err);
      if (err) {
        res.redirect(url.format({
          pathname:"/dispetcheri/error",
          query: {
            "error": err.message
          }
        }));
      }
    }
    else res.redirect('/dispetcheri');
  })
})

despetcheriServer.get('/addVoditel', authDispetcher, (req, res) => {
  res.render('addVoditel');
  // res.sendFile(path.join(__dirname, '/views/index.html'));
})

despetcheriServer.post('/addVoditel', authDispetcher, (req, res) => {
  db.run('INSERT INTO voditeli (username, password, phone, car) VALUES (?, ?, ?, ?)', [req.body.username, req.body.password, req.body.phone, req.body.car], (err) => {
    if (err) {
      res.redirect(url.format({
        pathname:"/dispetcheri/error",
        query: {
          "error": err.message
        }
      }));
    }
    else res.redirect('/dispetcheri/deleteVoditel');
  })
})

despetcheriServer.get('/deleteVoditel', authDispetcher, (req, res) => {
  db.all('SELECT * FROM voditeli', (err, rows) => {
    if (err) {
      res.redirect(url.format({
        pathname:"/dispetcheri/error",
        query: {
          "error": err.message
        }
      }));
    }
    else res.render('deleteVoditel', { voditeli: rows });
  })
  // res.sendFile(path.join(__dirname, '/views/index.html'));
})

despetcheriServer.post('/deleteVoditel', authDispetcher, (req, res) => {
  db.run('DELETE FROM voditeli WHERE id = ?;', [req.body.id], (err) => {
    if (err) {
      res.redirect(url.format({
        pathname:"/dispetcheri/error",
        query: {
          "error": err.message
        }
      }));
    }
    else res.redirect('/dispetcheri/deleteVoditel');
  })
})

despetcheriServer.get('/poezdka/:id/', authDispetcher, (req, res) => {
  db.get('SELECT * from poezdki WHERE id = ?;', [req.params.id], (err, row) => {
    if (err || !row) {
      console.error(err);
      if (err) {
        res.redirect(url.format({
          pathname:"/dispetcheri/error",
          query: {
            "error": err.message
          }
        }));
      } else {
        res.redirect('/dispetcheri/');
      }
    }
    else {
      row.time = Math.round(row.km * 3 + 5);
      if (row.voditel_id) {
        db.get('SELECT * FROM voditeli WHERE id = ?;', [row.voditel_id], (err1, row1) => {
          if (!err1) {
            if (row1) {
              res.render('poezdka', { poezdka: row, voditel: row1 });
            }
            else res.render('poezdka', { poezdka: row });
          }
          else {
            res.render('poezdka', { poezdka: row });
          }
        })
      }
      else {
        res.render('poezdka', { poezdka: row });
      }
    }
  })
  // res.sendFile(path.join(__dirname, '/views/index.html'));
})

despetcheriServer.post('/deletePoezdka', authDispetcher, (req, res) => {
  db.run('DELETE FROM poezdki WHERE id = ?;', [req.body.id], (err) => {
    if (err) {
      res.redirect(url.format({
        pathname:"/dispetcheri/error",
        query: {
          "error": err.message
        }
      }));
    }
    else res.redirect('/dispetcheri');
  })
})

despetcheriServer.get('/error', authDispetcher, (req, res) => {
  res.render('error', { error: req.query.error });
  // res.sendFile(path.join(__dirname, '/views/index.html'));
})

const voditeliServer = express();

voditeliServer.use(express.urlencoded({ extended: true }));
voditeliServer.use(express.json());

function auth(req, res, next) {
  var token = req.headers['x-access-token'];
  if (typeof token === 'string') {
    try {
      var decoded = jwt.verify(token, secret);
      db.get('SELECT * FROM voditeli WHERE id = ?;', [decoded.id], (err, row) => {
        if (err || !row) res.sendStatus(401);
        else {
          next()
        }
      });
    } catch(err) {
      res.sendStatus(401);
    }
  } else {
    res.sendStatus(401);
  }
}

voditeliServer.post('/login', (req, res) => {
  db.get('SELECT * FROM voditeli WHERE username = ? AND password = ?;', [req.body.login, req.body.password], (err, row) => {
    if (err || !row) res.sendStatus(400);
    else {
      var token = jwt.sign(row, secret);
      res.send(token);
    }
  });
});

voditeliServer.post('/checkToken', (req, res) => {
  var token = req.headers['x-access-token'];
  if (typeof token === 'string') {
    try {
      var decoded = jwt.verify(token, secret);
      db.get('SELECT * FROM voditeli WHERE id = ?;', [decoded.id], (err, row) => {
        if (err || !row) res.sendStatus(401);
        else {
          res.sendStatus(200);
        }
      });
    } catch(err) {
      res.sendStatus(401);
    }
  } else {
    res.sendStatus(401);
  }
})

voditeliServer.post('/poezdki', auth, (req, res) => {
  var token = req.headers['x-access-token'];
  if (typeof token === 'string') {
    try {
      var decoded = jwt.verify(token, secret);
      db.all('SELECT * from poezdki WHERE voditel_id IS NULL;', (err, rows) => {
        if (err) {
          res.sendStatus(400);
        } else {
          res.send({ poezdki: rows });
        }
      });
    } catch(err) {
      res.sendStatus(401);
    }
  } else {
    res.sendStatus(401);
  }
})

// var token = req.headers['x-access-token'];
//   if (typeof token === 'string') {
//     try {
//       var decoded = jwt.verify(token, secret);
//     } catch(err) {
//       res.sendStatus(403);
//     }
//   } else {
//   res.sendStatus(401);
// }

voditeliServer.post('/poezdka', auth, (req, res) => {
  var token = req.headers['x-access-token'];
  if (typeof token === 'string') {
    try {
      var decoded = jwt.verify(token, secret);
      var id = req.body.id;
      if (typeof id === 'number') {
        db.get('SELECT * from poezdki WHERE id = ?;', [id], (err, row) => {
          if (err || !row) res.sendStatus(400);
          else {
            if (row.voditel_id == null || row.voditel_id === decoded.id) {
              res.send({ poezdka: row });
            } else {
              res.sendStatus(403);
            }
          }
        });
      } else {
        res.sendStatus(400)
      }
    } catch(err) {
      res.sendStatus(401);
    }
  } else {
    res.sendStatus(401);
  }
});

voditeliServer.post('/currentPoezdka', auth, (req, res) => {
  var token = req.headers['x-access-token'];
  if (typeof token === 'string') {
    try {
      var decoded = jwt.verify(token, secret);
      db.get('SELECT * from poezdki WHERE voditel_id = ?;', [decoded.id], (err, row) => {
        if (err || !row) res.sendStatus(400);
        else {
          res.send({ poezdka: row });
        }
      });
    } catch(err) {
      res.sendStatus(401);
    }
  } else {
    res.sendStatus(401);
  }
})

voditeliServer.post('/startPoezdka', auth, (req, res) => {
  var token = req.headers['x-access-token'];
  if (typeof token === 'string') {
    try {
      var decoded = jwt.verify(token, secret);
      var id = req.body.id;
      if (typeof id === 'number') {
        db.get('SELECT * from poezdki WHERE id = ?;', [id], (err, row) => {
          if (err || !row) res.sendStatus(400);
          else {
            if (row.voditel_id == null) {
              db.all('SELECT * from poezdki WHERE voditel_id = ?;', [decoded.id], (err, rows) => {
                if (rows.length === 0 && !err) {
                  db.run('UPDATE poezdki SET voditel_id = ?, status = ? WHERE id = ?', [decoded.id, 'В пути', id], (err) => {
                    if (err) res.sendStatus(400);
                    else res.sendStatus(200);
                  })
                } else {
                  res.sendStatus(400);
                }
              })
            } else {
              res.sendStatus(403);
            }
          }
        });
      } else {
        res.sendStatus(400)
      }
    } catch(err) {
      res.sendStatus(401);
    }
  } else {
    res.sendStatus(401);
  }
});

voditeliServer.post('/endPoezdka', auth, (req, res) => {
  var token = req.headers['x-access-token'];
  if (typeof token === 'string') {
    try {
      var decoded = jwt.verify(token, secret);
      db.get('DELETE from poezdki WHERE voditel_id = ?;', [decoded.id], (err) => {
        if (err) res.sendStatus(400);
        else {
          res.sendStatus(200);
        }
      });
    } catch(err) {
      res.sendStatus(401);
    }
  } else {
    res.sendStatus(401);
  }
})

app.use('/dispetcheri', despetcheriServer);
app.use('/voditeli', voditeliServer);
app.get('/', (req, res) => {
  res.redirect('/dispetcheri');
})

app.listen(port, () => {
  console.log(`Example app listening at http://localhost:${port}`)
})