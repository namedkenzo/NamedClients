'use strict';
// CI-only source overlay: exercise the reference server's existing integration
// tests against THIS client's code, without a server dependency in go.mod.
const fs = require('node:fs');
const path = require('node:path');
const {execFileSync} = require('node:child_process');
const root = path.resolve(__dirname, '..');
const target = path.resolve(root, process.argv[2] || '.compat-server');
if (target !== path.join(root, '.compat-server')) throw new Error('Use the dedicated .compat-server checkout');
if (!fs.readFileSync(path.join(target,'go.mod'),'utf8').startsWith('module namedicmp\n')) throw new Error('Unexpected reference module');
const referenceTest = path.join(target,'internal/client/integration_test.go');
if (!fs.existsSync(referenceTest)) throw new Error('Pinned reference integration tests missing');
for (const dir of ['internal/client','internal/reliable','internal/wire']) {
  fs.mkdirSync(path.join(target,dir),{recursive:true});
  for (const file of fs.readdirSync(path.join(root,dir))) {
    if (!file.endsWith('.go')) continue;
    const code = fs.readFileSync(path.join(root,dir,file),'utf8').replaceAll('namedp/internal/','namedicmp/internal/');
    fs.writeFileSync(path.join(target,dir,file),code);
  }
}
execFileSync(process.env.GO_BINARY || 'go',['test','-race','./internal/client','./internal/core'],{cwd:target,stdio:'inherit'});
