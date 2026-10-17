"""GYM-029: realistic GitHub CLI drafts, retry identity and mutation boundaries."""
import contextlib
import io
import importlib.util
import json
import os
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location('publisher', Path(__file__).with_name('publish-release.py'))
publisher = importlib.util.module_from_spec(spec)
spec.loader.exec_module(publisher)


class Remote:
    def __init__(self, release=None, tag=None, assets=None):
        self.release, self.tag, self.assets = release, tag, assets or {}
        self.mutations = []

    def response(self):
        return {'isDraft': self.release['draft'], 'targetCommitish': self.release['target_commitish'],
                'assets': [{'name': name} for name in self.assets]}

    def run(self, args, **kwargs):
        if args[0] != 'gh':
            assert args[0].endswith('/apksigner')
            return subprocess.CompletedProcess(args, 0)
        if args[1:3] == ['release', 'view']:
            return subprocess.CompletedProcess(args, 0 if self.release else 1,
                json.dumps(self.response()) if self.release else '', '' if self.release else 'release not found\n')
        assert '/git/ref/tags/' in args[-1], args
        return subprocess.CompletedProcess(args, 0 if self.tag else 1,
            json.dumps({'object': {'sha': self.tag}}) if self.tag else '',
            '' if self.tag else 'gh: Not Found (HTTP 404)')

    def output(self, args, **kwargs):
        if args[0] == 'jarsigner':
            return 'jar verified.\n'
        op = args[1:3]
        if op == ['api', '--method']:
            assert not self.tag
            self.tag = 'a'
            self.mutations.append('tag')
        elif op == ['release', 'create']:
            assert self.tag == 'a'
            self.release = {'target_commitish': 'a', 'draft': True}
            self.mutations.append('create')
        elif op == ['release', 'upload']:
            path = Path(args[4])
            assert path.name not in self.assets
            self.assets[path.name] = path.read_bytes()
            self.mutations.append(path.name)
        elif op == ['release', 'download']:
            name = args[args.index('--pattern') + 1]
            (Path(args[args.index('--dir') + 1]) / name).write_bytes(self.assets[name])
        elif op == ['release', 'edit']:
            self.release['draft'] = False
            self.mutations.append('publish')
        elif args[1] == 'api':
            # GitHub's by-tag REST endpoint cannot find a draft; do not regress to it.
            assert '/git/ref/tags/' in args[2], args
            return json.dumps({'object': {'sha': self.tag}})
        else:
            raise AssertionError(args)
        return ''


class PublicationTest(unittest.TestCase):
    def exercise(self, remote, dry_run=False):
        with tempfile.TemporaryDirectory() as root:
            directory = Path(root) / 'release'
            directory.mkdir()
            for ext in ('apk', 'aab'):
                (directory / f'gym-assystant-0.1.1001.{ext}').write_bytes(ext.encode())
            env = {'GITHUB_REPOSITORY': 'owner/repo', 'GITHUB_SHA': 'a',
                   'RELEASE_CODE': '1001', 'RUNNER_TEMP': root, 'ANDROID_HOME': '/fake'}
            with patch.dict(os.environ, env), patch('subprocess.check_output', remote.output), patch('subprocess.run', remote.run), contextlib.redirect_stdout(io.StringIO()):
                publisher.publish(dry_run)
        return remote

    def test_fresh_release_and_existing_draft_lookup(self):
        remote = self.exercise(Remote())
        self.assertEqual(3, len(remote.assets))
        self.assertFalse(remote.release['draft'])

    def test_retry_preserves_published_bytes(self):
        first = self.exercise(Remote())
        retry = self.exercise(Remote(first.release.copy(), 'a', first.assets.copy()))
        self.assertEqual([], retry.mutations)

    def test_partial_draft_reuses_uploaded_asset(self):
        name = 'gym-assystant-0.1.1001.apk'
        remote = self.exercise(Remote({'target_commitish': 'a', 'draft': True}, 'a', {name: b'old bytes'}))
        self.assertEqual(b'old bytes', remote.assets[name])
        self.assertFalse(remote.release['draft'])

    def test_sha_conflicts_refuse_all_mutation(self):
        for release, tag in [(None, 'b'), ({'target_commitish': 'b', 'draft': True}, 'a')]:
            remote = Remote(release, tag)
            with self.assertRaises(RuntimeError):
                self.exercise(remote)
            self.assertEqual([], remote.mutations)

    def test_bad_checksums_are_not_overwritten(self):
        first = self.exercise(Remote())
        remote = Remote(first.release, 'a', {**first.assets, 'SHA256SUMS': b'bad'})
        with self.assertRaises(RuntimeError):
            self.exercise(remote)
        self.assertEqual([], remote.mutations)

    def test_dry_run_never_mutates_missing_or_existing_draft(self):
        for remote in (Remote(), Remote({'target_commitish': 'a', 'draft': True}, 'a')):
            self.exercise(remote, dry_run=True)
            self.assertEqual([], remote.mutations)

    def test_release_lookup_errors_are_not_missing_drafts(self):
        with patch('subprocess.run', return_value=subprocess.CompletedProcess([], 1, '', 'HTTP 403: forbidden')):
            with self.assertRaisesRegex(RuntimeError, '403'):
                publisher.find_release('owner/repo', 'v0.1.1001')


if __name__ == '__main__':
    unittest.main()
