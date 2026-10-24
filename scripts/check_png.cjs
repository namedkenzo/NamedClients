'use strict';
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const dir=process.argv[2];
for(const name of ['light-idle.png','dark-idle.png','dark-connecting.png','dark-connected-preview.png']){
 const b=fs.readFileSync(path.join(dir,name));
 assert.ok(b.length>1024,`${name}: missing image content`);
 assert.equal(b.subarray(0,8).toString('hex'),'89504e470d0a1a0a',`${name}: invalid PNG signature`);
 assert.ok(b.readUInt32BE(16)>100 && b.readUInt32BE(20)>100,`${name}: invalid dimensions`);
 assert.equal(b.subarray(b.length-8,b.length-4).toString(),'IEND',`${name}: incomplete PNG`);
 console.log(`${name}: ${b.readUInt32BE(16)} x ${b.readUInt32BE(20)}, ${b.length} bytes`);
}
