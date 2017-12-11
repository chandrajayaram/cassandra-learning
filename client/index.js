// Require icon used so it's included in any bundles
require('killrvideo-icon.png');

// Require basic CSS needed by the app
require('bootswatch/cosmo/bootstrap.css');
require('font-awesome/css/font-awesome.css');
require('gemini-scrollbar/gemini-scrollbar.css');
require('app.css');
require('react-joyride/lib/react-joyride-compiled.css')

// Render the application
  var app = require('./js');
  
  // Kinda a goofy hack, but because of the combination of babel + webpack (see https://github.com/webpack/webpack/issues/706), we
  // need to call either .default() or .renderApp() to get at the exported function from './js'
  app.renderApp(document.getElementById("react-container"), null);



