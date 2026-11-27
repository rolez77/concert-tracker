"""Local development-only Spotify catalog proxy. Python 3.9+, no dependencies."""
import base64
import getpass
import json
import os
import re
import threading
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode, urlsplit, parse_qs
from urllib.request import Request, urlopen

ID_RE = re.compile(r'^[A-Za-z0-9]{22}$')

class ApiError(Exception):
    def __init__(self, status, message):
        self.status, self.message = status, message

class Spotify:
    def __init__(self, client_id, secret, opener=urlopen):
        self.auth = 'Basic ' + base64.b64encode((client_id + ':' + secret).encode()).decode()
        self.opener = opener
        self.lock = threading.Lock()
        self.token = None
        self.expires = 0

    def request(self, req, token_request=False):
        try:
            with self.opener(req, timeout=10) as response:
                return json.load(response)
        except HTTPError as e:
            if e.code == 429:
                raise ApiError(429, 'Spotify request quota or rate limit reached. Try later.') from None
            if token_request and e.code in (400, 401):
                raise ApiError(503, 'Spotify credentials rejected. Check the Client ID and Client Secret.') from None
            if e.code == 403:
                raise ApiError(403, 'Spotify access denied. Check developer app access and owner Premium status.') from None
            if e.code == 401:
                raise ApiError(401, 'Spotify authorization expired. Try again.') from None
            if e.code == 404:
                raise ApiError(404, 'Spotify artist not found.') from None
            raise ApiError(502, 'Spotify request failed. Try again later.') from None
        except (URLError, OSError, ValueError):
            raise ApiError(502, 'Could not contact Spotify. Check your internet connection.') from None

    def access_token(self):
        with self.lock:
            if self.token and time.monotonic() < self.expires:
                return self.token
            data = self.request(Request('https://accounts.spotify.com/api/token',
                data=b'grant_type=client_credentials', headers={'Authorization': self.auth,
                'Content-Type': 'application/x-www-form-urlencoded'}), True)
            self.token = data['access_token']
            self.expires = time.monotonic() + max(0, int(data.get('expires_in', 3600)) - 60)
            return self.token

    def get(self, path):
        for attempt in range(2):
            token = self.access_token()
            try:
                return self.request(Request('https://api.spotify.com/v1/' + path,
                    headers={'Authorization': 'Bearer ' + token}))
            except ApiError as e:
                if e.status != 401 or attempt: raise
                with self.lock:
                    if self.token == token: self.token = None

    @staticmethod
    def simplify(item):
        artist_id = item.get('id', '')
        if not ID_RE.fullmatch(artist_id) or not item.get('name'): return None
        images = [i for i in item.get('images', []) if isinstance(i, dict) and
                  urlsplit(i.get('url', '')).scheme == 'https' and
                  urlsplit(i.get('url', '')).hostname == 'i.scdn.co']
        # Select a useful thumbnail without modifying Spotify artwork.
        images.sort(key=lambda i: abs((i.get('width') or 300)-300))
        return {'id': artist_id, 'name': item['name'], 'image': images[0]['url'] if images else '',
                'url': 'https://open.spotify.com/artist/' + artist_id}

    def search(self, query):
        data = self.get('search?' + urlencode({'q': query, 'type': 'artist', 'limit': 10, 'market': 'US'}))
        return [a for item in data.get('artists', {}).get('items', []) if (a := self.simplify(item))]

    def artist(self, artist_id):
        data = self.simplify(self.get('artists/' + artist_id))
        if data is None: raise ApiError(404, 'Spotify artist not found.')
        return data

class Handler(BaseHTTPRequestHandler):
    def log_message(self, fmt, *args):
        # Avoid logging search terms or credentials.
        pass

    def send_json(self, status, body):
        data = json.dumps(body, ensure_ascii=False).encode('utf-8')
        self.send_response(status)
        self.send_header('Content-Type', 'application/json; charset=utf-8')
        self.send_header('Cache-Control', 'no-store')
        self.send_header('Content-Length', str(len(data)))
        self.end_headers()
        self.wfile.write(data)

    def do_GET(self):
        parsed = urlsplit(self.path)
        try:
            if parsed.path == '/health':
                result = {'status': 'ok'}
            elif parsed.path == '/search':
                query = parse_qs(parsed.query).get('q', [''])[0].strip()
                if not 1 <= len(query) <= 150: raise ApiError(400, 'Enter an artist name (1–150 characters).')
                result = {'artists': self.server.spotify.search(query)}
            elif parsed.path == '/artist':
                artist_id = parse_qs(parsed.query).get('id', [''])[0]
                if not ID_RE.fullmatch(artist_id): raise ApiError(400, 'Invalid artist ID.')
                result = self.server.spotify.artist(artist_id)
            else:
                raise ApiError(404, 'Unknown route.')
            self.send_json(200, result)
        except ApiError as e:
            self.send_json(e.status, {'error': e.message})
        except (BrokenPipeError, ConnectionResetError):
            pass
        except Exception:
            self.send_json(502, {'error': 'Unexpected Spotify response. Try again.'})


def main():
    client_id = os.environ.get('SPOTIFY_CLIENT_ID') or input('Spotify Client ID: ').strip()
    secret = os.environ.get('SPOTIFY_CLIENT_SECRET') or getpass.getpass('Spotify Client Secret (hidden): ').strip()
    if not client_id or not secret: raise SystemExit('Both credentials are required.')
    spotify = Spotify(client_id, secret)
    # Fail early if credentials are invalid. Never print tokens or secrets.
    try:
        spotify.access_token()
    except ApiError as e:
        raise SystemExit(e.message)
    server = ThreadingHTTPServer(('127.0.0.1', 8765), Handler)
    server.spotify = spotify
    print('Concert Logify Spotify service running at http://127.0.0.1:8765', flush=True)
    print('Keep this window open while using Spotify search. Press Ctrl+C to stop.', flush=True)
    try: server.serve_forever()
    except KeyboardInterrupt: pass
    finally: server.server_close()

if __name__ == '__main__':
    main()
