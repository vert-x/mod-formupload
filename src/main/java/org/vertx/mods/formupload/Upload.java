package org.vertx.mods.formupload;

import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.file.AsyncFile;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.streams.Pump;
import org.vertx.java.core.streams.ReadStream;

import java.nio.charset.Charset;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public class Upload implements ReadStream {

  private Handler<Buffer> dataHandler;
  private Handler<Void> endHandler;

  private final HttpServerRequest req;
  private final Vertx vertx;

  public final String name;
  public final String filename;
  public final String contentType;
  public final String contentTransferEncoding;
  public final Charset charset;
  public final long size;

  private boolean paused;
  private Buffer pauseBuff;
  private boolean complete;

  protected Upload(Vertx vertx, HttpServerRequest req, String name, String filename, String contentType,
                   String contentTransferEncoding,
                   Charset charset, long size) {
    this.vertx = vertx;
    this.req = req;
    this.name = name;
    this.filename = filename;
    this.contentType = contentType;
    this.contentTransferEncoding = contentTransferEncoding;
    this.charset = charset;
    this.size = size;
  }

  public void dataHandler(Handler<Buffer> handler) {
    this.dataHandler = handler;
  }

  public void pause() {
    req.pause();
    paused = true;
  }

  public void resume() {
    if (paused) {
      req.resume();
      paused = false;
      if (pauseBuff != null) {
        receiveData(pauseBuff);
        pauseBuff = null;
      }
      if (complete && endHandler != null) {
        endHandler.handle(null);
      }
    }
  }

  public void exceptionHandler(Handler<Exception> handler) {
  }

  public void endHandler(Handler<Void> handler) {
    this.endHandler = handler;
  }

  public void bodyHandler(final Handler<Buffer> handler) {
    final Buffer buff = new Buffer();
    dataHandler = new Handler<Buffer>() {
      public void handle(Buffer b) {
        buff.appendBuffer(b);
      }
    };
    endHandler = new Handler<Void>() {
      public void handle(Void event) {
        handler.handle(buff);
      }
    };
  }

  public void streamToDisk(String filename) {
    streamToDisk(filename, new AsyncResultHandler<Void>() {
      public void handle(AsyncResult<Void> event) {
      }
    });
  }

  public void streamToDisk(String filename, final AsyncResultHandler<Void> resultHandler) {
    pause();
    vertx.fileSystem().open(filename, new AsyncResultHandler<AsyncFile>() {
      public void handle(final AsyncResult<AsyncFile> ar) {
        if (ar.succeeded()) {
          Pump p = Pump.createPump(Upload.this, ar.result.getWriteStream());
          p.start();
          endHandler(new Handler<Void>() {
            public void handle(Void event) {
              ar.result.close(resultHandler);
            }
          });
          resume();
        } else {
          resultHandler.handle(new AsyncResult<Void>(ar.exception));
        }
      }
    });
  }

  protected void receiveData(Buffer data) {
    if (!paused) {
      if (dataHandler != null) {
        dataHandler.handle(data);
      }
    } else {
      if (pauseBuff == null) {
        pauseBuff = new Buffer();
      }
      pauseBuff.appendBuffer(data);
    }
  }

  protected void complete() {
    if (paused) {
      complete = true;
    } else if (endHandler != null) {
      endHandler.handle(null);
    }
  }



}
