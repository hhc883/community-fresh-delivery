const path = require('path');
module.exports = {
  publicPath: '/', lintOnSave: false, productionSourceMap: false,
  pluginOptions: {'style-resources-loader': {preProcessor:'scss', patterns:[path.resolve(__dirname,'src/styles/_variables.scss'),path.resolve(__dirname,'src/styles/_mixins.scss')]}},
  devServer: {host:'127.0.0.1', port:5177, open:false, proxy:{'/admin':{target:'http://127.0.0.1:8080'}, '/ws':{target:'http://127.0.0.1:8080',ws:true}}},
  chainWebpack(config) {config.plugins.delete('fork-ts-checker');},
  css: {sourceMap:false}
};
