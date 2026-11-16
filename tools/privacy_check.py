"""Check tracked project files for forbidden local files and common credential patterns."""
from pathlib import Path
import re,subprocess,sys

ROOT=Path(__file__).resolve().parent.parent
FORBIDDEN_SUFFIXES={'.keystore','.jks','.p12','.pem','.key','.idsig','.apk','.aab','.log','.har','.db','.sqlite','.mp4','.webm'}
FORBIDDEN_DIRS={'.local','out','out-personal','cache','captures','screenshots','secrets','__pycache__'}
PATTERNS={
    'private key':re.compile(rb'-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----'),
    'GitHub token':re.compile(rb'(?:gh[pousr]_[A-Za-z0-9]{30,}|github_pat_[A-Za-z0-9_]{30,})'),
    'Google API credential':re.compile(rb'AIza[A-Za-z0-9_-]{35}'),
    'Google OAuth access token':re.compile(rb'ya29\.[A-Za-z0-9_-]{20,}'),
    'AWS access key':re.compile(rb'AKIA[A-Z0-9]{16}'),
    'local Windows user path':re.compile(rb'[A-Za-z]:[\\/]+Users[\\/]+[^\\/\s]+'),
}
def project_files():
    if (ROOT/'.git').exists():
        result=subprocess.run(['git','ls-files','-z'],cwd=ROOT,stdout=subprocess.PIPE,check=True)
        return [ROOT/p.decode('utf-8') for p in result.stdout.split(b'\0') if p]
    return [p for p in ROOT.rglob('*') if p.is_file() and not any(x in FORBIDDEN_DIRS|{'.git'} for x in p.relative_to(ROOT).parts)]
def main():
    files=project_files();findings=[]
    for path in files:
        rel=path.relative_to(ROOT);name=path.name
        if path.suffix.lower() in FORBIDDEN_SUFFIXES or any(x in FORBIDDEN_DIRS for x in rel.parts) or name in {'config.properties','youtube-channels.json'} or name=='.env' or name.startswith('.env.'):
            findings.append((rel.as_posix(),'forbidden local file'));continue
        data=path.read_bytes()
        for label,pattern in PATTERNS.items():
            if pattern.search(data):findings.append((rel.as_posix(),label))
    if findings:
        for path,label in findings:print(path+': '+label)
        print('Privacy check failed:',len(findings),'finding(s). Values are deliberately not printed.');return 1
    print('Privacy check passed:',len(files),'project files; no forbidden files or known credential patterns.');return 0
if __name__=='__main__':sys.exit(main())
