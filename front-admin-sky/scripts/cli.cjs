const {spawnSync} = require('child_process');
const path = require('path');
const args = [path.join(__dirname, '../node_modules/@vue/cli-service/bin/vue-cli-service.js'), ...process.argv.slice(2)];
const env = {...process.env, NODE_OPTIONS: [process.env.NODE_OPTIONS, '--openssl-legacy-provider'].filter(Boolean).join(' ')};
process.exit(spawnSync(process.execPath, args, {stdio: 'inherit', env}).status ?? 1);
