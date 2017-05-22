# smarti

<!-- DOWNLOAD -->

## Installation

Installation packages are provided for [Debian](https://www.debian.org/)- and [RedHat](https://www.redhat.com/)-based systems. Alternatively the application can
be started by directly launching the executable jar: `java -jar smarti-${version}-exec.jar`.

### Requirements

**smarti** is a [Java](https://java.com/)-Application based on the [Spring Framework](https://spring.io/). 

To run, it has the following minimal system requirements:

* DualCore-CPU (> 2GHz)
* 4GB RAM
* 2GB temporary Storage
* available network-port (default: 8080)
* [**JavaVM**](https://java.com/), version **1.8**
* [**MongoDB**](https://www.mongodb.com/), version **>=2.6**

All direct runtime-dependencies such as third-party libraries are packaged with the main executable.

### Changing the User

When installed using one of the provided packages (`deb`, `rpm`), smarti runs as it's own system user `smarti`. This user is created during the installation 
process if it not already exists.

To run smarti as a different user (`dbsystel` in this example), do the following steps:

1. Create the new working-directory

        mkdir -p /data/smarti
        chown -R dbsystel: /data/smarti

1. Populate the new working-directory with the required configuration files, e.g. by copying the default settings:

        cp /etc/smarti/* /data/smarti

1. Update the systemd configuration for smarti

        systemctl edit smarti

    and add the following content:

        [Service]
        User = dbsystel
        WorkingDirectory = /data/smarti

1. Restart the smarti

        systemctl try-restart smarti

## Configuration

### application.properties
`/etc/smarti/application.properties`

    ## logging config file
    logging.config = ./logback.xml
    
    ## server port
    server.port = 8080
    
    ## mongo-db
    #spring.data.mongodb.uri=mongodb://localhost/smarti
    spring.data.mongodb.database = smarti
    #spring.data.mongodb.host = localhost
    #spring.data.mongodb.port = 27017
    #spring.data.mongodb.password =
    #spring.data.mongodb.username =
    
## Monitoring / Troubleshooting

### Logging

* Working-Directory at `/var/lib/smarti/`
* Log-Files available under `/var/lib/smarti/logs/`, a symlink to `/var/log/smarti/`
* [Log-Configuration](http://logback.qos.ch/manual/configuration.html) under `/var/lib/smarti/logback.xml`, a 
symlink to `/etc/smarti/logback.xml`

Please keep in mind, that if _Kompetenzmarktplatz_ runs as a different user it probably also has a custom working directory. 
In such case, logs are stored in the new working directory (or whatever is configured in the logging-configuration).

### Daemon Configuration

* `/etc/default/smarti`
    * JVM and JVM Options (e.g. `-Xmx2g`)
    * Program Arguments (overwriting settings from the `application.properties`)

### Health-Check
System-wide health check is available under `http://localhost:8080/system/health`.

## Third-Party Dependencies and Licenses

When installing via one of the provided packages (`rpm`, `deb`) a list of used third-party libraries and their licenses 
are available under 

* `/usr/share/doc/smarti/THIRD-PARTY.txt` (backend), and
* `/usr/share/doc/smarti/UI-THIRD-PARTY.json` (frontend, UI)

From the running system, similar files are served under `http://localhost:8080/THIRD-PARTY.txt` (backend) 
and `http://localhost:8080/3rdPartyLicenses.json` (frontend, UI).
