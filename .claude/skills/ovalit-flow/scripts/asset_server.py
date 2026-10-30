# 앱에 넣은 게임 이미지 폴더를 Figma 플러그인이 받아 갈 수 있게 로컬에만 띄운다.
# Desktop Bridge 플러그인은 localhost만 부를 수 있고, 플러그인 쪽 fetch라 CORS 헤더가 있어야 한다.
# 쓰는 법: python3 -I asset_server.py <폴더> <포트>
import sys
from functools import partial
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer


class CorsHandler(SimpleHTTPRequestHandler):
    def end_headers(self):
        self.send_header("Access-Control-Allow-Origin", "*")
        super().end_headers()


root, port = sys.argv[1], int(sys.argv[2])
ThreadingHTTPServer(("127.0.0.1", port), partial(CorsHandler, directory=root)).serve_forever()
