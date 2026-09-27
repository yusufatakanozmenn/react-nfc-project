#!/usr/bin/env python3
"""Query OSV for exact Maven versions from the most recent test classpath.
Run backend tests first. Only public package names/versions leave this machine.
"""
import json
from pathlib import Path
import urllib.request
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
reports = sorted((root / 'backend/target/surefire-reports').glob('TEST-*.xml'))
if not reports:
    raise SystemExit('Run backend tests first; no resolved classpath found.')
classpath = ET.parse(reports[0]).find("./properties/property[@name='java.class.path']").attrib['value']
queries = []
for jar in classpath.split(':'):
    if '/.m2/repository/' not in jar or not jar.endswith('.jar'):
        continue
    parts = jar.split('/.m2/repository/', 1)[1].split('/')
    group, artifact, version = '.'.join(parts[:-3]), parts[-3], parts[-2]
    queries.append({'package': {'ecosystem': 'Maven', 'name': f'{group}:{artifact}'}, 'version': version})
if not queries:
    raise SystemExit('No Maven packages found; audit incomplete.')
findings = []
pending = queries
while pending:
    request = urllib.request.Request('https://api.osv.dev/v1/querybatch',
        data=json.dumps({'queries': pending}).encode(), headers={'Content-Type': 'application/json'})
    with urllib.request.urlopen(request, timeout=60) as response:
        data = json.load(response)
    if len(data.get('results', [])) != len(pending):
        raise SystemExit('Incomplete audit response.')
    pages = []
    for query, result in zip(pending, data['results']):
        for vuln in result.get('vulns', []):
            findings.append({'package': query['package']['name'], 'version': query['version'], 'id': vuln['id']})
        if result.get('next_page_token'):
            pages.append({**query, 'page_token': result['next_page_token']})
    pending = pages
print(json.dumps({'checked_packages': len(queries), 'findings': findings}, indent=2))
raise SystemExit(1 if findings else 0)
