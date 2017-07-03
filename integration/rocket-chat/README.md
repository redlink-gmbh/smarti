## Build and watch

* `npm install`
* `./node_modules/.bin/webpack --config webpack.config.js --watch`

## Serve locally and tunnel

* e.g. `python -m SimpleHTTPServer 9999` and `ssh -R 9503:localhost:9999 redlink@cerbot.redlink.io`
* HINT: don't forget to adapt the script url in bookmarklet

## Provide a smarti application and tunnel

* ssh -R 9501:localhost:8080 redlink@cerbot.redlink.io

## Provide a Solr backend for test searches  on port 8984 and tunnel

* ssh -R 9502:localhost:8984 redlink@cerbot.redlink.io