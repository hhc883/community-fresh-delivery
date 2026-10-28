"""Serve built frontends on loopback, with Sky admin extensions and API forwarding.

Development/manual preview only. Use deploy/nginx-fresh.conf for deployment.
"""
import argparse
import http.client
from functools import partial
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import unquote, urlsplit


class Handler(SimpleHTTPRequestHandler):
    extensions = None
    backend = None
    spa = False
    static_directory = Path(__file__).resolve().parents[1] / 'front' / 'static'

    def translate_path(self, path):
        url_path = unquote(urlsplit(path).path)
        if url_path.startswith('/static/'):
            target = (self.static_directory / url_path[len('/static/'):]).resolve()
            if not target.is_relative_to(self.static_directory.resolve()):
                return str(self.static_directory / '__invalid_path__')
            return str(target)
        if self.extensions and (url_path == '/extensions' or url_path.startswith('/extensions/')):
            relative = url_path.removeprefix('/extensions').lstrip('/')
            target = (self.extensions / relative).resolve()
            if not target.is_relative_to(self.extensions):
                return str(self.extensions / '__invalid_path__')
            if not target.is_file() and not target.suffix:
                target = self.extensions / 'index.html'
            return str(target)
        target = super().translate_path(path)
        if self.spa and not Path(target).exists() and not Path(url_path).suffix:
            return str(Path(self.directory) / 'index.html')
        return target

    def proxy(self):
        if not self.backend or not urlsplit(self.path).path.startswith(('/admin/', '/common/', '/api/', '/user/', '/uploads/')):
            return False
        connection = http.client.HTTPConnection(self.backend.hostname, self.backend.port or 8080, timeout=25)
        try:
            length = int(self.headers.get('Content-Length', '0'))
            if length < 0 or length > 5 * 1024 * 1024:
                self.send_error(413)
                return True
            body = self.rfile.read(length) if length else None
            headers = {key: value for key, value in self.headers.items()
                       if key.lower() not in ('host', 'connection', 'transfer-encoding', 'accept-encoding')}
            connection.request(self.command, self.path, body=body, headers=headers)
            response = connection.getresponse()
            data = response.read()
            self.send_response(response.status)
            for key, value in response.getheaders():
                if key.lower() not in ('connection', 'transfer-encoding', 'content-length', 'server', 'date'):
                    self.send_header(key, value)
            self.send_header('Content-Length', str(len(data)))
            self.end_headers()
            if self.command != 'HEAD':
                self.wfile.write(data)
        except (OSError, ValueError, http.client.HTTPException):
            self.send_error(502, 'Backend unavailable')
        finally:
            connection.close()
        return True

    def do_GET(self):
        if not self.proxy():
            super().do_GET()

    def do_HEAD(self):
        if not self.proxy():
            super().do_HEAD()

    def do_write(self):
        if not self.proxy():
            self.send_error(405)

    do_POST = do_PUT = do_DELETE = do_PATCH = do_write

    def end_headers(self):
        self.send_header('Cache-Control', 'no-store')
        super().end_headers()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--directory', required=True, type=Path)
    parser.add_argument('--extensions', type=Path)
    parser.add_argument('--port', required=True, type=int)
    parser.add_argument('--backend', help='Loopback backend, e.g. http://127.0.0.1:8080')
    parser.add_argument('--spa', action='store_true')
    args = parser.parse_args()
    for directory in (args.directory, args.extensions):
        if directory is not None and not (directory / 'index.html').is_file():
            parser.error('Build index.html is missing: ' + str(directory))
    if args.backend:
        target = urlsplit(args.backend)
        if target.scheme != 'http' or target.hostname not in ('127.0.0.1', 'localhost'):
            parser.error('Preview backend must use HTTP loopback')
        Handler.backend = target
    Handler.extensions = args.extensions.resolve() if args.extensions else None
    Handler.spa = args.spa
    ThreadingHTTPServer(('127.0.0.1', args.port), partial(Handler, directory=str(args.directory.resolve()))).serve_forever()


if __name__ == '__main__':
    main()
