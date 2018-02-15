# Smarti API Test

This is a api test for Smarti to test the basic features.

## Usage

The environment is packaged as docker-image:

```bash
docker build -t smarti:apiTest .
docker run --rm smarti:apiTest [options]
```

### Available Options

_Mandatory arguments to long options are mandatory for short options too_

* `--username`: _admin-user_ for Smarti, default is `admin`
* `--password`: _admin_password_ for Smarti, default is `admin`
* `--url`: _base-url_ for Smarti, default is `http://localhost:8080/`
* `--loglevel`: _loglevel_ for Requests, default is `error`

### Troubleshooting

#### Linux
To make the Docker-Container connect to your lokal Smarti, you need to start it with the --net=host option.

#### Mac
To make the Docker-Container connect to your lokal Smarti, use the following url: http://docker.for.mac.host.internal:<port>/