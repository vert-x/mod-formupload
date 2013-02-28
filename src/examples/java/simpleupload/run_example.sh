# We set VERTX_MODS to point to where we built the module
export VERTX_MODS=$(readlink -f '../../../../build/mod')
# And then we run the example
vertx run SimpleUploadServer.java -includes vertx.formupload-v1.0
