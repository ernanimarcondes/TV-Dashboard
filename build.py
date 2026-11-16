"""Build TV-Dashboard with Python 3, JDK 17 and Android SDK 34. No Gradle required."""
from pathlib import Path
import argparse,json,os,re,shutil,subprocess,sys,zipfile

ROOT=Path(__file__).resolve().parent
def version(path):return tuple(int(x) for x in re.findall(r'\d+',path.name))
def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--personal',action='store_true',help='Use ignored local config.properties and channel settings')
    args=parser.parse_args()
    sdk_value=os.environ.get('ANDROID_HOME') or os.environ.get('ANDROID_SDK_ROOT')
    if not sdk_value:raise SystemExit('Set ANDROID_HOME to your Android SDK directory.')
    sdk=Path(sdk_value).resolve();ext='.exe' if os.name=='nt' else ''
    jdk=Path(os.environ['JAVA_HOME'])/'bin' if os.environ.get('JAVA_HOME') else None
    def java_tool(name):
        candidate=jdk/(name+ext) if jdk else shutil.which(name)
        if not candidate or not Path(candidate).exists():raise SystemExit('JDK 17+ is required: '+name)
        return str(candidate)
    tools=sorted((p for p in (sdk/'build-tools').iterdir() if (p/('aapt2'+ext)).exists()),key=version)
    if not tools:raise SystemExit('Install Android SDK build-tools 34.0.0.')
    bt=tools[-1];android=sdk/'platforms/android-34/android.jar'
    if not android.exists():raise SystemExit('Install Android SDK platform android-34.')
    out=ROOT/'out';out.mkdir(exist_ok=True)
    gen=out/'gen/io/freemytv';gen.mkdir(parents=True,exist_ok=True)
    classes=out/'classes';classes.mkdir(exist_ok=True)
    properties={}
    if args.personal and (ROOT/'config.properties').exists():
        for line in (ROOT/'config.properties').read_text(encoding='utf-8-sig').splitlines():
            if '=' in line and not line.lstrip().startswith('#'):
                key,value=line.split('=',1);properties[key.strip()]=value.strip()
    values={
        'TMDB_KEY':('tmdb_key',''),'WATCHMODE_KEY':('watchmode_key',''),'PLEX_URL':('plex_url',''),
        'PLEX_TOKEN':('plex_token',''),'TRAKT_CLIENT_ID':('trakt_client_id',''),
        'TMDB_REGION':('tmdb_region','BR'),'LATITUDE':('latitude',''),'LONGITUDE':('longitude',''),
        'CITY':('city',''),'LANGUAGE':('language','pt'),'CURRENCY':('currency','BRL'),
        'TEMPERATURE':('temperature','C'),'HIDE_SERVICES':('hide_services',''),'THEME':('theme','dark'),
        'IMMICH_SERVER':('immich_server',''),'HA_URL':('home_assistant_url',''),'HA_ENTITIES':('home_assistant_entities','')}
    config='package io.freemytv;\nfinal class Config {\n'
    for field,(key,default) in values.items():config+=' static final String '+field+' = '+json.dumps(properties.get(key,default),ensure_ascii=True)+';\n'
    flags={'HOME_ASSISTANT':False,'DISABLE_ASSISTANT':False,'SHOW_CRYPTO':False,'HIDE_ANIMATION':False,
        'SCREENSAVER':True,'GENERIC':False,'OPEN_IN_STREMIO':False,'OPEN_IN_NUVIO':False}
    for field,default in flags.items():
        value=properties.get(field.lower(),str(default).lower())=='true' if field not in {'GENERIC','HOME_ASSISTANT'} else default
        config+=' static final boolean '+field+' = '+str(value).lower()+';\n'
    (gen/'Config.java').write_text(config+'}\n',encoding='utf-8')
    # Always package assets from a clean staging directory. --personal is required for local channel choices.
    assets=out/'assets';assets.mkdir(exist_ok=True)
    for item in (ROOT/'app/assets').rglob('*'):
        if not item.is_file():continue
        rel=item.relative_to(ROOT/'app/assets')
        if item.name=='youtube-channels.json' or item.name.startswith('video-'):continue
        target=assets/rel;target.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(item,target)
    channels=ROOT/'app/assets/racing/youtube-channels.json' if args.personal else ROOT/'app/assets/racing/youtube-channels.example.json'
    if not channels.exists():channels=ROOT/'app/assets/racing/youtube-channels.example.json'
    parsed=json.loads(channels.read_text(encoding='utf-8-sig'))
    (assets/'racing/youtube-channels.json').write_text(json.dumps(parsed,ensure_ascii=False),encoding='utf-8')
    xml=out/'res/xml';xml.mkdir(parents=True,exist_ok=True)
    (xml/'network_security_config.xml').write_text('<network-security-config><base-config cleartextTrafficPermitted="false" /></network-security-config>',encoding='utf-8')
    def run(*command):subprocess.run([str(x) for x in command],check=True,cwd=ROOT)
    run(bt/('aapt2'+ext),'compile','--dir',ROOT/'app/res','-o',out/'res.zip')
    run(bt/('aapt2'+ext),'compile','--dir',out/'res','-o',out/'res-gen.zip')
    run(bt/('aapt2'+ext),'link','-o',out/'base.apk','-I',android,'--manifest',ROOT/'app/AndroidManifest.xml','--java',out/'gen',out/'res.zip',out/'res-gen.zip','--min-sdk-version','26','--target-sdk-version','33','-A',assets,'-0','ttf')
    with zipfile.ZipFile(out/'base.apk') as source,zipfile.ZipFile(out/'fixed.apk','w') as target:
        for info in source.infolist():
            data=source.read(info.filename);info.filename=info.filename.replace('\\','/');target.writestr(info,data)
    shutil.move(out/'fixed.apk',out/'base.apk')
    sources=list((ROOT/'app/src').rglob('*.java'))+list((out/'gen').rglob('*.java'))
    (out/'sources.txt').write_text('\n'.join('"'+p.as_posix()+'"' for p in sources),encoding='utf-8')
    run(java_tool('javac'),'-nowarn','-Xlint:-options','-source','8','-target','8','-bootclasspath',str(android)+os.pathsep+str(bt/'core-lambda-stubs.jar'),'-classpath',android,'-encoding','UTF-8','-d',classes,'@'+str(out/'sources.txt'))
    with zipfile.ZipFile(out/'classes.jar','w',zipfile.ZIP_DEFLATED) as jar:
        for item in classes.rglob('*.class'):jar.write(item,item.relative_to(classes).as_posix())
    run(java_tool('java'),'-Xmx1024M','-cp',bt/'lib/d8.jar','com.android.tools.r8.D8','--release','--min-api','26','--lib',android,'--output',out,out/'classes.jar')
    with zipfile.ZipFile(out/'base.apk','a',compression=zipfile.ZIP_DEFLATED) as apk:apk.write(out/'classes.dex','classes.dex')
    run(bt/('zipalign'+ext),'-f','-p','4',out/'base.apk',out/'aligned.apk')
    key=ROOT/'.local/debug.keystore';key.parent.mkdir(exist_ok=True)
    if not key.exists():run(java_tool('keytool'),'-genkeypair','-keystore',key,'-alias','androiddebugkey','-keyalg','RSA','-keysize','2048','-validity','10000','-storepass','android','-keypass','android','-dname','CN=Android Debug,O=Android,C=US')
    apk=out/'TV-Dashboard.apk'
    run(java_tool('java'),'-jar',bt/'lib/apksigner.jar','sign','--ks',key,'--ks-key-alias','androiddebugkey','--ks-pass','pass:android','--key-pass','pass:android','--out',apk,out/'aligned.apk')
    run(java_tool('java'),'-jar',bt/'lib/apksigner.jar','verify','--verbose',apk)
    print('Built:',apk)
    print('Configuration:', 'personal local files' if args.personal else 'public defaults; no local settings included')
if __name__=='__main__':main()
