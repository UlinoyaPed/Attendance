#!/usr/bin/env python3
"""Exercise signing with disposable keys; no repository Secrets or released test APKs."""
import base64, hashlib, os, secrets, shutil, subprocess, tempfile, re
from pathlib import Path

repo = Path(__file__).resolve().parents[2]
script = repo / '.github/scripts/sign-nightly.sh'
sdk = Path(os.environ.get('ANDROID_SDK_ROOT') or os.environ['ANDROID_HOME'])
apksigner = str(max((sdk / 'build-tools').glob('*/apksigner'),
    key=lambda path: tuple(int(x) for x in re.findall(r'\d+', path.parent.name))))
jdk = os.environ['JAVA_HOME']
source = repo / 'app/build/outputs/apk/release/app-release-unsigned.apk'
with tempfile.TemporaryDirectory(prefix='attendance-sign-check-', dir=os.environ.get('RUNNER_TEMP')) as directory:
    root = Path(directory)
    password = secrets.token_urlsafe(32)
    env = os.environ.copy()
    for variable in ['SIGN_KEY_BASE64', 'SIGN_KEY_ALIAS', 'SIGN_KEY_PWD']:
        env.pop(variable, None)
    env.update(JAVA_HOME=jdk, ANDROID_HOME=str(sdk), ANDROID_SDK_ROOT=str(sdk), TEST_KEY_PASSWORD=password)
    fixture = root / 'fixture.keystore'
    generation = subprocess.run([jdk+'/bin/keytool', '-genkeypair', '-storetype', 'PKCS12', '-keystore', str(fixture),
        '-alias', 'fixture', '-keyalg', 'RSA', '-keysize', '2048', '-validity', '1', '-dname', 'CN=Disposable signing test',
        '-storepass:env', 'TEST_KEY_PASSWORD'], env=env, capture_output=True)
    assert generation.returncode == 0, 'Disposable test key generation failed'
    encoded = base64.b64encode(fixture.read_bytes()).decode()
    expected = subprocess.run([apksigner, 'verify', '--print-certs', str(source)], env=env, capture_output=True)
    assert expected.returncode != 0, 'Fixture input must be unsigned'
    signed_env = dict(SIGN_KEY_BASE64=encoded, SIGN_KEY_ALIAS='fixture', SIGN_KEY_PWD=password)
    def run_case(name, changes, success, signed, stale=False):
        work = root / name
        (work/'dist').mkdir(parents=True)
        (work/'temp').mkdir()
        shutil.copyfile(source, work/'dist/Attendance-nightly-unsigned.apk')
        if stale:
            (work/'dist/Attendance-nightly.apk').write_bytes(b'old generated output')
            (work/'dist/SIGNING-CERTIFICATE.txt').write_text('old fingerprint')
        case_env = env.copy()
        case_env.update(changes, RUNNER_TEMP=str(work/'temp'))
        result = subprocess.run(['bash', str(script)], cwd=work, env=case_env, capture_output=True)
        assert (result.returncode == 0) == success, f'{name}: unexpected exit status'
        output = work/'dist/Attendance-nightly.apk'
        assert output.exists() == signed, f'{name}: unverified artifact emitted'
        assert not list((work/'temp').iterdir()), f'{name}: private temporary files not cleaned'
        assert hashlib.sha256((work/'dist/Attendance-nightly-unsigned.apk').read_bytes()).digest() == hashlib.sha256(source.read_bytes()).digest(), f'{name}: unsigned input changed'
        for private in [encoded.encode(), password.encode()]:
            assert private not in result.stdout + result.stderr, f'{name}: secret appeared in logs'
        if signed:
            verification = subprocess.run([apksigner, 'verify', '--print-certs', str(output)], env=env, capture_output=True)
            assert verification.returncode == 0, 'Signed APK verification failed'
            fingerprint = (work/'dist/SIGNING-CERTIFICATE.txt').read_text()
            assert 'certificate SHA-256 digest:' in fingerprint, 'Public fingerprint missing'
            assert set(p.name for p in (work/'dist').iterdir()) == {'Attendance-nightly.apk','Attendance-nightly-unsigned.apk','SIGNING-CERTIFICATE.txt'}, 'Unexpected artifact emitted'
        print(name + ': passed')
    run_case('no_secrets', {}, True, False)
    run_case('no_secrets_with_stale_output', {}, True, False, stale=True)
    run_case('partial_secrets', {'SIGN_KEY_ALIAS':'fixture'}, False, False)
    run_case('valid_key', signed_env, True, True)
    run_case('wrong_password', {**signed_env, 'SIGN_KEY_PWD':'incorrect-disposable-password'}, False, False)
    run_case('wrong_alias', {**signed_env, 'SIGN_KEY_ALIAS':'nonexistent'}, False, False)
    run_case('invalid_base64', {**signed_env, 'SIGN_KEY_BASE64':'not-base64!'}, False, False)
    run_case('invalid_keystore', {**signed_env, 'SIGN_KEY_BASE64':base64.b64encode(b'not a keystore').decode()}, False, False)
