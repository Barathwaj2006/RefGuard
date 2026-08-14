const http = require('http');
const fs = require('fs');
const path = require('path');
const RefGuardPipeline = require('./pipeline');

class RefGuardServer {
  constructor(port = process.env.PORT || 3000) {
    this.port = port;
    this.pipeline = new RefGuardPipeline();
    this.server = null;
  }

  start() {
    return new Promise((resolve) => {
      this.server = http.createServer((req, res) => {
        this.handleRequest(req, res);
      });

      this.server.listen(this.port, () => {
        console.log('[RefGuard API & Demo Server] Running on http://localhost:' + this.port);
        console.log('  - API Endpoint (POST): http://localhost:' + this.port + '/api/v1/scan');
        console.log('  - Report Endpoint (POST): http://localhost:' + this.port + '/api/v1/report');
        console.log('  - Interactive Demo UI: http://localhost:' + this.port + '/');
        resolve(this.server);
      });
    });
  }

  stop() {
    return new Promise((resolve) => {
      if (this.server) {
        this.server.close(() => resolve());
      } else {
        resolve();
      }
    });
  }

  handleRequest(req, res) {
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization');

    if (req.method === 'OPTIONS') {
      res.writeHead(204);
      res.end();
      return;
    }

    const host = req.headers.host || 'localhost';
    const parsedUrl = new URL(req.url, 'http://' + host);
    const pathname = parsedUrl.pathname;

    if (pathname === '/health' && req.method === 'GET') {
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ status: 'HEALTHY', service: 'RefGuard-Integration', timestamp: new Date().toISOString() }));
      return;
    }

    if (pathname === '/api/v1/scan' && req.method === 'POST') {
      this.readBody(req, (err, body) => {
        if (err || !body) {
          this.sendError(res, 400, 'INVALID_REQUEST', 'Request body must be valid JSON');
          return;
        }
        try {
          const result = this.pipeline.processScan(body);
          res.writeHead(200, { 'Content-Type': 'application/json' });
          res.end(JSON.stringify(result, null, 2));
        } catch (e) {
          const code = e.statusCode || 500;
          this.sendError(res, code, 'SCAN_ERROR', e.message, e.details);
        }
      });
      return;
    }

    if (pathname === '/api/v1/report' && req.method === 'POST') {
      this.readBody(req, (err, body) => {
        if (err || !body) {
          this.sendError(res, 400, 'INVALID_REQUEST', 'Request body must be valid JSON');
          return;
        }
        try {
          const result = this.pipeline.processReport(body);
          res.writeHead(200, { 'Content-Type': 'application/json' });
          res.end(JSON.stringify(result, null, 2));
        } catch (e) {
          const code = e.statusCode || 400;
          this.sendError(res, code, 'REPORT_ERROR', e.message, e.details);
        }
      });
      return;
    }

    if (req.method === 'GET') {
      let filePath = pathname === '/' ? '/index.html' : pathname;
      let staticPath = path.join(__dirname, '../demo/public', filePath);

      if (fs.existsSync(staticPath) && fs.statSync(staticPath).isFile()) {
        const ext = path.extname(staticPath);
        const mimeTypes = {
          '.html': 'text/html',
          '.js': 'text/javascript',
          '.css': 'text/css',
          '.json': 'application/json',
          '.png': 'image/png',
          '.svg': 'image/svg+xml'
        };
        res.writeHead(200, { 'Content-Type': mimeTypes[ext] || 'text/plain' });
        fs.createReadStream(staticPath).pipe(res);
        return;
      }
    }

    res.writeHead(404, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ error_code: 'NOT_FOUND', message: 'Endpoint not found', timestamp: new Date().toISOString() }));
  }

  readBody(req, callback) {
    let raw = '';
    req.on('data', chunk => {
      raw += chunk;
      if (raw.length > 5 * 1024 * 1024) {
        req.destroy();
      }
    });
    req.on('end', () => {
      try {
        const parsed = JSON.parse(raw);
        callback(null, parsed);
      } catch (e) {
        callback(e, null);
      }
    });
    req.on('error', err => callback(err, null));
  }

  sendError(res, statusCode, code, message, details) {
    res.writeHead(statusCode, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({
      error_code: code,
      message,
      details: details || undefined,
      timestamp: new Date().toISOString()
    }, null, 2));
  }
}

if (require.main === module) {
  const server = new RefGuardServer(process.env.PORT || 3000);
  server.start();
}

module.exports = RefGuardServer;
