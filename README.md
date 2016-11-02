# Vert.x 2.x is **deprecated** - use instead http://vertx.io/docs/vertx-core/java/#_handling_requests

## This module is now deprecated, as multi-part form upload functionality now exists in the Vert.x core API

## Form Upload

This non runnable module implements multipart form upload functionality.

### Dependencies

No dependencies

### Name

The module name is `vertx.formupload-vV`. (Where V is the version, e.g. 1.0)

### Configuration

No configuration

### Usage

If you are using this module from another module (moduleA) then you should add the name of this module to the
'includes' field in the mod.json for moduleA.

If you are using this module from a verticle which is started using vertx run at the command line you should add the
command line option: '-includes vertx.formupload-vV' (where V is the version) when starting the verticle, e.g.

vertx run MyVerticle.java -includes vertx.formupload-v1.0
