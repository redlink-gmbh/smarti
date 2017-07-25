## Client Configuration

The configuration module allows to manage client specific configurations. It consists of a storage component, a service layer, REST services and an UI.

### Client Configuration API

__base path:__ `/config`

The configuration REST Services allow to manage configurations via API calls.

All endpoints use `application/json` as media type.

#### Component Configuration

The JSON serialization of a configuration has the following base attributes

```
{
    "_class": "io.redlink.smarti.query.dbsearch.DbSearchEndpointConfiguration",
    "type": "query_dbsearch_keyword",
    "enabled": false,
    "unbound": false,
    "solrEndpoint": "http://dbsearch.test.org/solr/query"
}

```

* _`_class`_ (`String`): used to store the Java type of custom configuration implementation. Can be ommited when the default type (`ComponentConfiguration`) is used by a component.
* _`type`_ (`string`): The type of the component
* _`enabled`_ (`boolean`): Allows to disable a configuration withou deleting it.
* _`unbound`_ (`boolean`): if `true` this configuration is not bound to any component. This means that the `type` is wrong or the component of that `type` is currently not available

Any other field can be used for the actual configuration of the component. Their are no limitations to the kind of the configuration as long as the Component referenced by the `type` value can understand it.

__Notes about Server Side Configuration parsing__

Server-side parsing is done by [Jackson](https://github.com/FasterXML/jackson). The default `ComponentConfiguration` class parses additional parameters as `Map`. So By default configuration will use the default JSON to Java mappings.

Components that use custom configuration classes can define additional fields. Additioal fields will be handeld as with the base `ComponentConfiguration` class.

#### Create a Client Configuration

__endpoint:__ `POST /config/{client-id}` <br>
__request body__ _(optional)_: `{<conf-cat>: [<comp-conf>,<comp-conf>]}`<br>
__response body__: the created client configuration `{<conf-cat>: [<comp-conf>,<comp-conf>]}`

The endpoint accepts an optional request body with the configuration. If none is parsed the configuration for the client is initialized with the default configuration.

The response is the default configuration formatted as JSON

__example:__

```
curl -X POST -H "content-type: application/json" "http://localhost:8080/config/client42"

{
  "queryBuilder": [
    {
      "_class": "io.redlink.smarti.query.dbsearch.DbSearchEndpointConfiguration",
      "type": "query_dbsearch_keyword",
      "enabled": false,
      "unbound": false,
      "solrEndpoint": "http://dbsearch.test.org/solr/query"
    },
    [..]
  ]
}

```

#### Update a Client Configuration

__endpoint:__ `PUT /config/{client-id}`<br>
__request body__ _(required)_: `{<conf-cat>: [<comp-conf>,<comp-conf>]}`<br>
__response body__: the updated client configuration

Typically a configuration initialized by a `POST /config/{client-id}` will be changed and afterwards `PUT` to the same URI to apply the updates.

All updated configurations are validates (see the [Client Configuration Validation](#clientConfigurationValidation) section for more information)

#### Retrieve Client Configuration

__endpoint__: `GET config/{client-id}` <br>
__response body__: the created client configuration `{<conf-cat>: [<comp-conf>,<comp-conf>]}`

Retrieve the configuration for the client with the `{client-id}`. If no configuration is present a `404 Not Found` is returned

#### Client Configuration Validation

Configuration are validated on any update. This validation has several steps that are done for every component configuration:

1. it is checked if the Component targeted by the Configuration is active. For this the `category` and the `type` field of the configuration are matched against registerd components. If this succeeds `unbound` is set to `false`. 
2. for all _bound_ configurations the expected configuration class of the component is matched with the `_class` value of the configuration. If those do not match the configuration is marked as _illegal_
3. the configuration is parsed to the component for validation. This validation can fail because of missing or illegal field values.

During the validation process all validation errors are collected. If one or more are present a `422 Unprocessable Entity` response is generated. This response include [JSON Path](https://github.com/json-path/JsonPath) pointer to missing and illegal fields to allow UI to highlight those issues.

The following listing shows an example where an illegal URL was configured as `solrEndpoint` for the 2nd configuration of the `queryBuilder` category.

```
curl -X PUT -H "content-type: application/json" "http://localhost:8080/config/client25" --data-binary '{
  "queryBuilder": [
    [..],
    {
      "_class": "io.redlink.smarti.query.dbsearch.DbSearchEndpointConfiguration",
      "type": "query_dbsearch",
      "enabled": true,
      "unbound": false,
      "solrEndpoint": "dummy://dbsearch.test.org/solr/query/"
    }, 
    [..]
    ]
}'

< HTTP/1.1 422 
< Content-Type: application/json;charset=UTF-8

{
  "data": {
    "illegal": {
      "queryBuilder[1].solrEndpoint": "unknown protocol: dummy"
    }
  },
  "message": "Unable to process io.redlink.smarti.model.config.Configuration because of 1 illegal [queryBuilder[1].solrEndpoint] and 0 missing fields []",
  "status": 422
}
```