import io
import json
import threading
import unittest
from urllib.error import HTTPError
from urllib.parse import urlsplit, parse_qs
from urllib.request import urlopen
from http.server import ThreadingHTTPServer
from server import Spotify, Handler, ApiError

ARTIST_ID='1234567890123456789012'
ARTIST={'id':ARTIST_ID,'name':'Example Artist','images':[{'url':'https://i.scdn.co/image/example','width':300}]}

class SpotifyTests(unittest.TestCase):
    def setUp(self):
        self.calls=[]
        self.fail_once=False
        def fake(req, timeout):
            self.calls.append(req)
            if 'accounts.spotify.com' in req.full_url:
                data={'access_token':'test_token','expires_in':3600}
            else:
                if self.fail_once:
                    self.fail_once=False
                    raise HTTPError(req.full_url,401,'Unauthorized',{},None)
                self.assertEqual(req.get_header('Authorization'),'Bearer test_token')
                if '/search?' in req.full_url:
                    q=parse_qs(urlsplit(req.full_url).query)
                    self.assertEqual(q['type'],['artist']);self.assertEqual(q['limit'],['10'])
                    self.assertEqual(q['q'],['A & B']);data={'artists':{'items':[ARTIST]}}
                else: data=ARTIST
            return io.BytesIO(json.dumps(data).encode())
        self.spotify=Spotify('testid','testsecret',fake)
    def test_search_artist_and_token_reuse(self):
        a=self.spotify.search('A & B')[0]
        self.assertEqual(a['id'],ARTIST_ID);self.assertEqual(a['image'],ARTIST['images'][0]['url'])
        self.assertEqual(a['url'],'https://open.spotify.com/artist/'+ARTIST_ID)
        self.assertEqual(self.spotify.artist(ARTIST_ID),a)
        self.assertEqual(len(self.calls),3)
        self.assertEqual(self.calls[0].data,b'grant_type=client_credentials')
    def test_refresh_on_401(self):
        self.fail_once=True
        self.assertEqual(self.spotify.search('A & B')[0]['id'],ARTIST_ID)
        self.assertEqual(len(self.calls),4)
    def test_no_image(self):
        self.assertEqual(Spotify.simplify({'name':'Example','id':ARTIST_ID})['image'],'')
    def test_rate_limit(self):
        def fail(req,timeout):raise HTTPError(req.full_url,429,'Too many',{},None)
        with self.assertRaises(ApiError) as cm:Spotify('x','y',fail).access_token()
        self.assertEqual(cm.exception.status,429)
    def test_http_routes(self):
        http=ThreadingHTTPServer(('127.0.0.1',0),Handler);http.spotify=self.spotify
        thread=threading.Thread(target=http.serve_forever,daemon=True);thread.start()
        base='http://127.0.0.1:'+str(http.server_port)
        try:
            with urlopen(base+'/health') as r:self.assertEqual(json.load(r),{'status':'ok'})
            with urlopen(base+'/search?q=A%20%26%20B') as r:self.assertEqual(json.load(r)['artists'][0]['id'],ARTIST_ID)
            with urlopen(base+'/artist?id='+ARTIST_ID) as r:self.assertEqual(json.load(r)['name'],'Example Artist')
            for path in ['/search?q=','/artist?id=bad']:
                with self.assertRaises(HTTPError) as cm:urlopen(base+path)
                self.assertEqual(cm.exception.code,400);cm.exception.close()
        finally:
            http.shutdown();http.server_close();thread.join()

if __name__=='__main__':unittest.main()
