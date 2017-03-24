# JBoss A-MQ PersistentVolume Monitor

Camel Spring Boot app that monitors the A-MQ on OpenShift PersistentVolume and drains messages when a broker is decommissioned (scaled down).

## Running the example in OpenShift

It is assumed that:

- OpenShift platform is already running, if not you can find details how to [Install OpenShift at your site](https://docs.openshift.com/container-platform/3.3/install_config/index.html).
- Your system is configured for Fabric8 Maven Workflow, if not you can find a [Get Started Guide](https://access.redhat.com/documentation/en/red-hat-jboss-middleware-for-openshift/3/single/red-hat-jboss-fuse-integration-services-20-for-openshift/)

The example can be built and run on OpenShift using a single goal:

```
$ cd $PROJECT_ROOT
$ mvn fabric8:deploy -Dbroker.claimName=<MY_BROKER_PVC_NAME>
```
