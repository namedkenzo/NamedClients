'use strict';
const fs=require('node:fs'),path=require('node:path'),{execFileSync}=require('node:child_process');
const root=path.resolve(__dirname,'..'),target=path.join(root,'.compat-ipv6');
if(!fs.readFileSync(path.join(target,'go.mod'),'utf8').startsWith('module namedicmp\n'))throw Error('Unexpected fixture');
for(const dir of ['internal/client','internal/reliable','internal/wire']){
 fs.mkdirSync(path.join(target,dir),{recursive:true});
 for(const name of fs.readdirSync(path.join(root,dir))){if(name.endsWith('.go'))fs.writeFileSync(path.join(target,dir,name),fs.readFileSync(path.join(root,dir,name),'utf8').replaceAll('namedp/internal/','namedicmp/internal/'));}
}
const binary=path.join(target,'ipv6-integration.test');
execFileSync('go',['test','-race','-c','-tags=namedicmp_integration','-o',binary,'./internal/client'],{cwd:target,stdio:'inherit'});
execFileSync('sudo',['setcap','cap_net_raw+ep',binary],{stdio:'inherit'});
execFileSync(binary,['-test.v','-test.run','TestIPv6PanelTCPUDPAndReaders','-test.timeout','120s'],{cwd:target,stdio:'inherit'});
