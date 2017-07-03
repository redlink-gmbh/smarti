const path = require('path');

module.exports = {
    entry: './',
    output: {
        filename: 'rocket.chat.js',
        path: path.resolve(__dirname, 'target/classes/public/plugin/v1/')
    },
    module: {
        rules: [{
            test: /\.scss$/,
            use: [{
                loader: "style-loader" // creates style nodes from JS strings
            }, {
                loader: "css-loader" // translates CSS into CommonJS
            }, {
                loader: "sass-loader" // compiles Sass to CSS
            }]
        }]
    },
    node: {
        fs: 'empty'
    }
};