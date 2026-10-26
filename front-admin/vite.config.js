import {defineConfig} from 'vite';
import vue from '@vitejs/plugin-vue';
export default defineConfig({plugins:[vue()],base:'/extensions/',server:{host:'127.0.0.1',port:5177,strictPort:true,proxy:{'/admin':{target:process.env.API_PROXY_TARGET || 'http://127.0.0.1:8080'}}}});
