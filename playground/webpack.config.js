var webpack = require("webpack");
var path = require('path');


var CLIENT_DIR = path.resolve(__dirname);

// Path constants
var Paths = {
  JS: path.resolve(CLIENT_DIR),
  BUILD_OUTPUT: path.resolve(CLIENT_DIR)
};


module.exports = {
	entry: "./index.js",
	output: {
		path: Paths.BUILD_OUTPUT,
    	filename: 'bundle.js'
	},
	devServer: {
		inline: true,
		contentBase: '.',
		port: 3000
	},
	resolve: {
    extensions: [ '', '.js', '.jsx' ],
    root: [
      Paths.JS
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
			}
		]
	}
}