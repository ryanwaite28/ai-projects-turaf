function fn() {
  var env = karate.env || 'local';
  karate.log('karate.env system property was:', env);
  
  var config = {
    baseUrl: 'http://localhost:8080',
    wsUrl: 'ws://localhost:8081',
    waitTimeout: 30000,
    pollInterval: 1000,
    awsRegion: 'us-east-1'
  };
  
  if (env === 'dev') {
    config.baseUrl = 'https://api.dev.turafapp.com';
    config.wsUrl = 'wss://ws.dev.turafapp.com';
    config.awsAccountId = '801651112319';
  } else if (env === 'qa') {
    config.baseUrl = 'https://api.qa.turafapp.com';
    config.wsUrl = 'wss://ws.qa.turafapp.com';
    config.awsAccountId = '965932217544';
  } else if (env === 'prod') {
    config.baseUrl = 'https://api.turafapp.com';
    config.wsUrl = 'wss://ws.turafapp.com';
    config.awsAccountId = '811783768245';
  }
  
  // Configure timeouts
  karate.configure('connectTimeout', 10000);
  karate.configure('readTimeout', 30000);
  
  return config;
}
