import { test } from 'node:test';
import assert from 'node:assert/strict';
import { nfcLink, safeDestination } from '../src/utils/nfc.js';
import { deleteCustomer } from '../src/services/customers.js';

test('NFC links use configured API and stable card code, including same-origin hosting', () => {
  assert.equal(nfcLink('ABC123', 'https://nfc-api.webonix.com.tr/', 'https://nfc.webonix.com.tr'), 'https://nfc-api.webonix.com.tr/r/ABC123');
  assert.equal(nfcLink('ABC123', '', 'https://example.test'), 'https://example.test/r/ABC123');
  assert.equal(nfcLink('../admin', 'https://example.test', 'https://example.test'), '');
  assert.equal(safeDestination('javascript:alert(1)'), '');
  assert.equal(safeDestination('https://user:pass@example.test'), '');
  assert.equal(safeDestination('https://example.test/path'), 'https://example.test/path');
});
test('customer deletion reports missing endpoint and server rejection without pretending success', async () => {
  const previous = globalThis.fetch;
  try {
    for (const status of [404, 405, 501, 409, 500, 204]) {
      globalThis.fetch = async (url, options) => {
        if (url.endsWith('/csrf')) return Response.json({ token: 'fixture' });
        assert.ok(url.endsWith('/api/admin/customers/42'));
        assert.equal(options.method, 'DELETE');
        assert.equal(options.credentials, 'include');
        assert.equal(options.headers.get('X-XSRF-TOKEN'), 'fixture');
        return status === 204 ? new Response(null, { status }) : Response.json({ message: 'Rejected' }, { status });
      };
      if (status === 204) await deleteCustomer(42);
      else await assert.rejects(deleteCustomer(42), status === 409 || status === 500 ? /Rejected/ : /henüz etkin değil/);
    }
  } finally { globalThis.fetch = previous; }
});
