# Rocket.Chat Dataset Creation

This is a api test for Smarti to test the basic features.

## Usage

The environment is packaged as docker-image:

```bash
docker build -t rocketchat:datacreation .
docker run --rm rocketchat:datacreation [options]
```

### Available Options

_Mandatory arguments to long options are mandatory for short options too_

* `--username`: _admin-user_ for Rocket.Chat, default is `rocketchat.internal.admin.test`
* `--password`: _admin_password_ for Rocket.Chat, default is `rocketchat.internal.admin.test`
* `--url`: _base-url_ for Smarti, default is `http://localhost:3000/`
* `--loglevel`: _loglevel_ for Requests, default is `error`
* `--numRequests`: _number-of-requests_ for dataset, default is 5
* `--maxMessages`: _max-number-of-messages_ for each request, default is 3


### Troubleshooting

#### Linux
To make the Docker-Container connect to your lokal Rocket.Chat, you need to start it with the --net=host option.

#### Mac
To make the Docker-Container connect to your lokal Rocket.Chat, use the following url: http://docker.for.mac.host.internal:port/