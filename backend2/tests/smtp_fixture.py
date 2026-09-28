"""Local-only SMTP sink for synthetic integration-test addresses; never relays mail."""
import email
import json
import pathlib
import socketserver
import sys
import uuid

outbox = pathlib.Path(sys.argv[2])

class SMTP(socketserver.StreamRequestHandler):
    def send(self, text):
        self.wfile.write((text + '\r\n').encode())
        self.wfile.flush()

    def handle(self):
        self.connection.settimeout(10)
        self.send('220 localhost fixture')
        recipient = ''
        while line := self.rfile.readline():
            command = line.decode().strip()
            upper = command.upper()
            if upper.startswith(('EHLO', 'HELO')):
                self.send('250 localhost')
            elif upper.startswith('RCPT TO:'):
                recipient = command[8:].strip('<>')
                self.send('550 fixture rejected' if 'fail@' in recipient else '250 OK')
            elif upper == 'DATA':
                self.send('354 send message')
                lines = []
                while (line := self.rfile.readline()) not in (b'.\r\n', b''):
                    lines.append(line[1:] if line.startswith(b'..') else line)
                message = email.message_from_bytes(b''.join(lines))
                payload = '\n'.join(part.get_payload(decode=True).decode('utf-8') for part in message.walk() if not part.is_multipart())
                (outbox / (uuid.uuid4().hex + '.json')).write_text(json.dumps({'to': recipient, 'body': payload}))
                self.send('250 accepted')
            elif upper == 'QUIT':
                self.send('221 bye')
                break
            elif upper.startswith(('MAIL FROM:', 'RSET', 'NOOP')):
                self.send('250 OK')
            else:
                self.send('502 unsupported')

class Server(socketserver.ThreadingTCPServer):
    allow_reuse_address = True

with Server(('127.0.0.1', int(sys.argv[1])), SMTP) as server:
    server.serve_forever()
