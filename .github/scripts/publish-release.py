# GYM-029: shared by GitHub Actions and local Podman checks.
import hashlib
import json
import os
from pathlib import Path
import subprocess

def guard(release, tag_sha, expected):
    if tag_sha != expected or (release and release['target_commitish'] != expected):
        raise RuntimeError('Release/tag belongs to a different commit; refusing to publish')

def gh(*args):
    return subprocess.check_output(['gh', *args], text=True)

def optional_api(endpoint):
    result = subprocess.run(['gh', 'api', endpoint], capture_output=True, text=True)
    if result.returncode == 0:
        return json.loads(result.stdout)
    if '(HTTP 404)' in result.stderr:
        return None
    raise RuntimeError(result.stderr)

def find_release(repo, tag):
    result = subprocess.run(['gh', 'release', 'view', tag, '--repo', repo,
                             '--json', 'isDraft,targetCommitish,assets'], capture_output=True, text=True)
    if result.returncode:
        if result.stderr.strip() == 'release not found':
            return None
        raise RuntimeError(result.stderr)
    release = json.loads(result.stdout)
    return {'target_commitish': release['targetCommitish'], 'draft': release['isDraft'], 'assets': release['assets']}


def verify(directory, names):
    subprocess.run([os.environ['ANDROID_HOME'] + '/build-tools/35.0.0/apksigner',
                    'verify', str(directory / names[0])], check=True)
    bundle_check = subprocess.check_output(['jarsigner', '-verify', str(directory / names[1])], text=True)
    if 'jar verified.' not in bundle_check:
        raise RuntimeError('Bundle signature verification failed')


def publish(dry_run=False):
    repo = os.environ['GITHUB_REPOSITORY']
    sha = os.environ['GITHUB_SHA']
    version = '0.1.' + os.environ['RELEASE_CODE']
    tag = 'v' + version
    endpoint = f'repos/{repo}'
    release = find_release(repo, tag)
    commit = optional_api(f'{endpoint}/git/ref/tags/{tag}')
    guard(release, commit['object']['sha'] if commit else sha, sha)
    if release and not commit:
        raise RuntimeError('Existing release has no resolvable tag')
    directory = Path(os.environ['RUNNER_TEMP']) / 'release'
    names = [f'gym-assystant-{version}.{extension}' for extension in ('apk', 'aab')]
    verify(directory, names)
    if dry_run:
        print(f'Local checks passed for {tag} at {sha}; GitHub reads only, no publication.')
        return
    if not commit:
        gh('api', '--method', 'POST', f'{endpoint}/git/refs',
           '-f', f'ref=refs/tags/{tag}', '-f', f'sha={sha}')
    if not release:
        gh('release', 'create', tag, '--repo', repo, '--target', sha, '--verify-tag', '--draft',
           '--title', f'gym assystant {version}', '--notes',
           f'Commit: {sha}\nSigned offline Android APK and Play submission bundle.\n'
           'Verify SHA256SUMS before installing. See docs/architecture/android-distribution.md.')
        release = find_release(repo, tag)
        if release is None:
            raise RuntimeError('Created draft is not visible; refusing to continue')
    guard(release, json.loads(gh('api', f'{endpoint}/git/ref/tags/{tag}'))['object']['sha'], sha)
    assets = {asset['name'] for asset in release['assets']}
    for name in names:
        if name in assets:
            # Preserve already-uploaded bytes; rebuilding can change ZIP timestamps.
            gh('release', 'download', tag, '--repo', repo, '--pattern', name,
               '--dir', str(directory), '--clobber')
        else:
            gh('release', 'upload', tag, str(directory / name), '--repo', repo)
    checksums = ''.join(f'{hashlib.sha256((directory / name).read_bytes()).hexdigest()}  {name}\n'
                        for name in names)
    checksum_file = directory / 'SHA256SUMS'
    if 'SHA256SUMS' in assets:
        gh('release', 'download', tag, '--repo', repo, '--pattern', 'SHA256SUMS',
           '--dir', str(directory), '--clobber')
        if checksum_file.read_text() != checksums:
            raise RuntimeError('Existing release checksums do not match; refusing to overwrite')
    else:
        checksum_file.write_text(checksums)
        gh('release', 'upload', tag, str(checksum_file), '--repo', repo)
    verify(directory, names)
    # Check identity again immediately before publishing; never move an existing tag.
    guard(release, json.loads(gh('api', f'{endpoint}/git/ref/tags/{tag}'))['object']['sha'], sha)
    if release['draft']:
        gh('release', 'edit', tag, '--repo', repo, '--draft=false')
    print(f'Published {tag} for {sha}')


if __name__ == '__main__':
    import argparse
    parser = argparse.ArgumentParser(description='GYM-029: publish verified exact-commit Android assets.')
    parser.add_argument('--dry-run', action='store_true', help='Verify local assets and real GitHub reads without mutations.')
    publish(parser.parse_args().dry_run or os.environ.get('GYM_RELEASE_DRY_RUN') == '1')
