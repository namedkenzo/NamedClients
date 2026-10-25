'use strict';
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const root=path.resolve(__dirname,'..'),read=p=>fs.readFileSync(path.join(root,p),'utf8');
const gradle=read('android/app/build.gradle.kts'),version=gradle.match(/versionName = "([^"]+)"/)[1];
assert.ok(read('cmd/namedp-android-engine/main_android.go').includes('fmt.Println("'+version+'")'));
assert.ok(fs.existsSync(path.join(root,'docs/RELEASE-android-v'+version+'.md')));
assert.ok(read('android/app/src/main/java/dev/namedkenzo/namedp/RelayDelayResult.kt').includes('https://www.gstatic.com/generate_204'));
assert.ok(read('internal/androidrelay/delay.go').includes('https://www.gstatic.com/generate_204'));
console.log('Independent Android version '+version+' and relay endpoint verified.');
