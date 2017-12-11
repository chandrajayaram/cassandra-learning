var webpack = require("webpack");
var path = require('path');
var ExtractTextPlugin = require('extract-text-webpack-plugin');


var CLIENT_DIR = path.resolve(__dirname);

// Path constants
var Paths = {
  JS: path.resolve(CLIENT_DIR, 'js'),
  CSS: path.resolve(CLIENT_DIR, 'css'),
  IMAGES: path.resolve(CLIENT_DIR, 'images'),
  BUILD_OUTPUT: path.resolve(CLIENT_DIR, 'dist')
};
var BootswatchPlugin = {
		  // Apply plugin to the resolver
		  apply: function(resolver) {
		    // Part of the path to bootswatch files
		    var bootswatchPath = 'node_modules' + path.sep + 'bootswatch' + path.sep;
		    
		    // Plugin will process files
		    resolver.plugin('file', function(request, callback) {
		      // Look for requests that are relative paths inside bootswatch
		      if (request.path.indexOf(bootswatchPath) !== -1 && request.request.startsWith('..')) {
		        // Resolve relative to the bootstrap CSS folder instead
		        var newRequest = {
		          path: path.resolve(request.path, '../../bootstrap/dist/css'),
		          request: request.request,
		          query: request.query,
		          directory: request.directory
		        };
		        this.doResolve(['file'], newRequest, callback);
		      } else {
		        callback();
		      }
		    });
		  }
		}


// Plugins for the build
var plugins = [
  // Put CSS that's extracted into killrvideo.css
  new ExtractTextPlugin('css/killrvideo.css', { allChunks: true }),
  
  new webpack.ResolverPlugin([ BootswatchPlugin ])
];

module.exports = {
	entry: "./index.js",
	output: {
		path: Paths.BUILD_OUTPUT,
    	filename: 'js/killrvideo.js'
	},
	devServer: {
		inline: true,
		contentBase: './dist',
		port: 3000
	},
	resolve: {
    extensions: [ '', '.js', '.jsx' ],
    root: [
      Paths.JS, Paths.CSS, Paths.IMAGES
    ]
  },
   module: {
		loaders: [
			{
				test: /\.(js|jsx)$/,
				exclude: /(node_modules)/,
				loader: ["babel-loader"],
				query: {
					presets: ["latest", "stage-0", "react"]
				}
			},
			{
				test: /\.json$/,
				exclude: /(node_modules)/,
				loader: "json-loader"
			},
			{
				test: /\.css$/,
				loader: ExtractTextPlugin.extract('style-loader', 'css-loader', { publicPath: '../' })
			},
			{
				test: /\.scss$/,
				loader: 'style-loader!css-loader!autoprefixer-loader!sass-loader'
			},
		    { 
				test: /\.png$/,
				include: Paths.IMAGES,
				loader: 'file',
				query: { name: 'images/[name].[ext]' } 
			 },
			 { 
				test: /\.(woff(2)?|ttf|eot|svg)(\?v=\d\.\d\.\d)?$/, 
				loader: 'file',
				query: { name: 'fonts/[name].[ext]' }
			 }
			
		]
	},
	plugins: plugins

}