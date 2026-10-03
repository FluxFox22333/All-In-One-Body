"""Build against an existing exact target API; no game libraries are redistributed.
Usage: python tools/build_cached.py --jdk PATH --api minecraft-patched.jar --deps DIR --junit junit-console.jar
For normal online builds use ./gradlew clean build (JDK 25).
"""
from pathlib import Path
import argparse, subprocess, os, shutil, zipfile
p=argparse.ArgumentParser()
for name in ['jdk','api','deps','junit']: p.add_argument('--'+name,required=True,type=Path)
a=p.parse_args(); root=Path(__file__).resolve().parents[1]; build=root/'build'
classes=build/'classes'; tests=build/'tests'
for d in [classes,tests]:
 if d.exists(): shutil.rmtree(d)
 d.mkdir(parents=True)
cp=os.pathsep.join(map(str,[a.api,*sorted(a.deps.glob('*.jar'))]))
def run(args): subprocess.run(list(map(str,args)),check=True)
run([a.jdk/'bin/javac','-proc:none','--release','25','-encoding','UTF-8','-cp',cp,'-d',classes,*sorted((root/'src/main/java').rglob('*.java'))])
run([a.jdk/'bin/javac','-proc:none','--release','25','-encoding','UTF-8','-cp',str(classes)+os.pathsep+str(a.junit),'-d',tests,*sorted((root/'src/test/java').rglob('*.java'))])
run([a.jdk/'bin/java','-jar',a.junit,'execute','--class-path',str(classes)+os.pathsep+str(tests),'--select-package','dev.fivefold.core','--reports-dir',build/'test-results','--disable-ansi-colors'])
jar=build/'libs/fivefold-0.5.0-alpha.jar'; jar.parent.mkdir(exist_ok=True)
with zipfile.ZipFile(jar,'w',zipfile.ZIP_DEFLATED) as z:
 z.writestr('META-INF/MANIFEST.MF','Manifest-Version: 1.0\nImplementation-Version: 0.5.0-alpha\n\n')
 for base in [classes,root/'src/main/resources']:
  for f in sorted(base.rglob('*')):
   if f.is_file(): z.write(f,f.relative_to(base).as_posix())
print(jar)
