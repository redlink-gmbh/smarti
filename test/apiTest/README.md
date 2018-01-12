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

* `--user`: _admin-user_ for Smarti, default is `admin`
* `--pwd`: _admin_password_ for Smarti, default ist `admin`
* `--url`: _base-url_ for Smarti, default ist `http://localhost:8080/`