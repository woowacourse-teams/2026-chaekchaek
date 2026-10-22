#!/usr/bin/env python3
"""Exercise the real iOS shell entrypoint with fake Apple tools, without booting devices."""
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest

SCRIPT = Path(__file__).with_name('test-ios-hig.sh')
FAKE_TOOL = '''
import json, os
from pathlib import Path
import sys
root = Path(os.environ['MOBILE_QUEUE_TEST_DIR'])
args = sys.argv[1:]
name = Path(sys.argv[0]).name
with (root/'events').open('a') as output:
    output.write(json.dumps([name, *args])+'\\n')
if name == 'xcodebuild':
    raise SystemExit(23 if os.environ.get('FAIL_BUILD') else 0)
if args[:3] == ['simctl','list','devices']:
    if os.environ.get('BAD_DEVICE_QUERY'):
        print('invalid json'); raise SystemExit(1)
    print(json.dumps({'devices': {'test': [{'name':'iPhone Test','udid':'test-udid','state':(root/'state').read_text()}]}}))
elif args[:2] == ['simctl','boot']:
    (root/'state').write_text('Booted')
elif args[:2] == ['simctl','shutdown']:
    (root/'state').write_text('Shutdown')
elif args[:2] == ['simctl','ui'] and len(args)==4:
    print('large')
'''


class IOSQueueEntryTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.root = Path(self.tmp.name)
        (self.root/'state').write_text('Shutdown')
        for name in ['xcrun','xcodebuild']:
            tool=self.root/name
            tool.write_text('#!'+sys.executable+'\n'+FAKE_TOOL)
            tool.chmod(0o755)
        self.env=dict(os.environ, PATH=str(self.root)+os.pathsep+os.environ['PATH'], MOBILE_QUEUE_TEST_DIR=str(self.root), AGENT_COORDINATION_DIR=str(self.root/'queue'), AGENT_SESSION_ID='ios-entry-test')
        self.env.pop('MOBILE_VALIDATION_JOB',None)

    def tearDown(self):
        self.tmp.cleanup()

    def execute(self):
        out=subprocess.run(['sh',str(SCRIPT),'test-udid'],env=self.env,capture_output=True,text=True,timeout=15)
        events=[json.loads(line) for line in (self.root/'events').read_text().splitlines()]
        return out,events

    def test_build_precedes_boot_and_cleanup_runs_after_both_sizes(self):
        out,events=self.execute()
        self.assertEqual(out.returncode,0,out.stderr)
        self.assertIn('"state": "running"',out.stderr)
        build=next(i for i,e in enumerate(events) if e[:2]==['xcodebuild','build-for-testing'])
        boot=next(i for i,e in enumerate(events) if e[:3]==['xcrun','simctl','boot'])
        self.assertLess(build,boot)
        self.assertEqual(sum(e[:2]==['xcodebuild','test-without-building'] for e in events),2)
        self.assertEqual(events[-1],['xcrun','simctl','shutdown','test-udid'])
        self.assertEqual((self.root/'state').read_text(),'Shutdown')

    def test_build_failure_never_boots_device(self):
        self.env['FAIL_BUILD']='1'
        out,events=self.execute()
        self.assertEqual(out.returncode,23,out.stderr)
        self.assertFalse(any(e[:3]==['xcrun','simctl','boot'] for e in events))

    def test_running_device_is_not_taken_or_stopped(self):
        (self.root/'state').write_text('Booted')
        out,events=self.execute()
        self.assertEqual(out.returncode,2,out.stderr)
        self.assertTrue(all(e[:4]==['xcrun','simctl','list','devices'] for e in events))
        self.assertEqual((self.root/'state').read_text(),'Booted')

    def test_query_error_does_not_boot_or_build(self):
        self.env['BAD_DEVICE_QUERY']='1'
        out,events=self.execute()
        self.assertNotEqual(out.returncode,0)
        self.assertTrue(all(e[:4]==['xcrun','simctl','list','devices'] for e in events))


if __name__ == '__main__': unittest.main()
