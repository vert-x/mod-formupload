# We set VERTX_MODS to point to where we built the module
#export VERTX_MODS=$(readlink -f '../../../../mods')
export VERTX_MODS=/Users/norman/Documents/workspace-intellij/mod-formupload/mods
# And then we run the example
vertx run SimpleUploadServer.java -includes io.vertx~mod-formupload~2.0.0-SNAPSHOT
