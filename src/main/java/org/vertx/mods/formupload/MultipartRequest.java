package org.vertx.mods.formupload;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.multipart.*;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.impl.DefaultHttpServerRequest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 * @author <a href="mailto:nmaurer@redhat.com">Norman Maurer</a>
 */
public class MultipartRequest {

  private final Vertx vertx;
  private final HttpServerRequest req;
  private Handler<Attribute> attrHandler;
  private Handler<Upload> uploadHandler;
  private Handler<Void> endHandler;
  private final Map<String, String> attributes = new HashMap<>();
  private final HttpRequest nettyReq;

  private HttpPostRequestDecoder decoder;

  public MultipartRequest(Vertx vertx, HttpServerRequest req) {
    this.vertx = vertx;
    this.req = req;
    // TODO - this is a bit of a hack
    HttpRequest nettyReq = ((DefaultHttpServerRequest)req).nettyRequest();
    try {
      this.nettyReq = nettyReq;
      decoder = new HttpPostRequestDecoder(new DataFactory(), nettyReq);
      req.dataHandler(new Handler<Buffer>() {
        @Override
        public void handle(Buffer event) {
          try {
            decoder.offer(new DefaultHttpContent(event.getByteBuf()));
          } catch (HttpPostRequestDecoder.ErrorDataDecoderException e) {
            throw convertException(e);
          }
        }
      });
      req.endHandler(new Handler<Void>() {
        @Override
        public void handle(Void event) {
          try {
            decoder.offer(LastHttpContent.EMPTY_LAST_CONTENT);
          } catch (HttpPostRequestDecoder.ErrorDataDecoderException e) {
            throw convertException(e);
          } finally {
            // notify endhandler in all cases
            if (endHandler != null) {
              endHandler.handle(null);
            }
          }
        }
      });
    } catch (Exception e) {
      throw convertException(e);
    }
  }

  public void endHandler(Handler<Void> endHandler) {
    this.endHandler = endHandler;
  }

  public void attributeHandler(Handler<Attribute> handler) {
    this.attrHandler = handler;
  }

  public void uploadHandler(Handler<Upload> handler) {
    this.uploadHandler = handler;
  }

  public String getAttribute(String name) {
    return attributes.get(name);
  }

  public Set<String> getAttributeNames() {
    return attributes.keySet();
  }

  private static RuntimeException convertException(Exception e) {
    RuntimeException re = new RuntimeException(e.getMessage());
    re.setStackTrace(e.getStackTrace());
    return re;
  }

  private final static class NettyFileUpload implements FileUpload {

    private final Upload upload;


    private String name;
    private String filename;
    private String contentType;
    private String contentTransferEncoding;
    private Charset charset;
    private boolean completed;

    private NettyFileUpload(Upload upload, String name, String filename, String contentType, String contentTransferEncoding, Charset charset) {
      this.upload = upload;
      this.name = name;
      this.filename = filename;
      this.contentType = contentType;
      this.contentTransferEncoding = contentTransferEncoding;
      this.charset = charset;
    }

    @Override
    public void setContent(ByteBuf channelBuffer) throws IOException {
        completed = true;
        upload.receiveData(new Buffer(channelBuffer));
        upload.complete();
    }

    @Override
    public void addContent(ByteBuf channelBuffer, boolean last) throws IOException {
        upload.receiveData(new Buffer(channelBuffer));
        if (last) {
          completed = true;
          upload.complete();
        }

    }

    @Override
    public void setContent(File file) throws IOException {
      throw new UnsupportedOperationException();
    }

    @Override
    public void setContent(InputStream inputStream) throws IOException {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isCompleted() {
      return completed;
    }

    @Override
    public long length() {
      throw new UnsupportedOperationException();
    }

    @Override
    public void delete() {
      throw new UnsupportedOperationException();
    }

    @Override
    public byte[] get() throws IOException {
      throw new UnsupportedOperationException();
    }

    @Override
    public ByteBuf getChunk(int i) throws IOException {
      throw new UnsupportedOperationException();
    }

    @Override
    public String getString() throws IOException {
      throw new UnsupportedOperationException();
    }

    @Override
    public String getString(Charset charset) throws IOException {
      throw new UnsupportedOperationException();
    }

    @Override
    public void setCharset(Charset charset) {
      this.charset = charset;
    }

    @Override
    public Charset getCharset() {
      return charset;
    }

    @Override
    public boolean renameTo(File file) throws IOException {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isInMemory() {
      return false;
    }

    @Override
    public File getFile() throws IOException {
      throw new UnsupportedOperationException();
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public HttpDataType getHttpDataType() {
      throw new UnsupportedOperationException();
    }

    @Override
    public int compareTo(InterfaceHttpData o) {
      return 0;
    }

    @Override
    public String getFilename() {
      return filename;
    }

    @Override
    public void setFilename(String filename) {
      this.filename = filename;
    }

    @Override
    public void setContentType(String contentType) {
      this.contentType = contentType;
    }

    @Override
    public String getContentType() {
      return contentType;
    }

    @Override
    public void setContentTransferEncoding(String contentTransferEncoding) {
      this.contentTransferEncoding = contentTransferEncoding;
    }

    @Override
    public String getContentTransferEncoding() {
      return contentTransferEncoding;
    }

    @Override
    public ByteBuf getByteBuf() throws IOException {
      throw new UnsupportedOperationException();
    }

    @Override
    public FileUpload copy() {
      throw new UnsupportedOperationException();
    }

    @Override
    public FileUpload retain() {
      return this;
    }

    @Override
    public FileUpload retain(int increment) {
      return this;
    }

    @Override
    public ByteBuf content() {
      throw new UnsupportedOperationException();
    }

    @Override
    public int refCnt() {
      return 1;
    }

    @Override
    public boolean release() {
      return false;
    }

    @Override
    public boolean release(int decrement) {
      return false;
    }
  }


  private class InternalMemoryAttribute extends MemoryAttribute {

    private boolean notified;
    private InternalMemoryAttribute(String name) {
      super(name);
    }

    private InternalMemoryAttribute(String name, String value) throws IOException {
      super(name, value);
    }

    @Override
    public void setValue(String value) throws IOException {
      super.setValue(value);
      attributeCreated();
    }

    private void attributeCreated() {
      if (!notified && isCompleted()) {
        notified = true;
        attributes.put(getName(), getValue());
        if (attrHandler != null) {
          attrHandler.handle(new Attribute(getName(), getValue()));
        }
      }
    }

    @Override
    public void setContent(ByteBuf buffer) throws IOException {
        super.setContent(buffer);
        attributeCreated();
    }

    @Override
    public void setContent(InputStream inputStream) throws IOException {
        super.setContent(inputStream);
        attributeCreated();
    }

    @Override
    public void setContent(File file) throws IOException {
        super.setContent(file);
        attributeCreated();
    }

    @Override
    public void addContent(ByteBuf buffer, boolean last) throws IOException {
      super.addContent(buffer, last);
      attributeCreated();
    }
  }


  private class DataFactory implements HttpDataFactory {

    @Override
    public io.netty.handler.codec.http.multipart.Attribute createAttribute(HttpRequest httpRequest, String name) {
      io.netty.handler.codec.http.multipart.Attribute nettyAttr;
      try {
        nettyAttr = new InternalMemoryAttribute(name);
      } catch (Exception e) {
        throw convertException(e);
      }
      return nettyAttr;
    }

    @Override
    public io.netty.handler.codec.http.multipart.Attribute createAttribute(HttpRequest httpRequest,
                                                                                  String name, String value) {
      io.netty.handler.codec.http.multipart.Attribute nettyAttr;
      try {
        nettyAttr = new InternalMemoryAttribute(name, value);
      } catch (Exception e) {
        throw convertException(e);
      }
      return nettyAttr;
    }

    @Override
    public FileUpload createFileUpload(HttpRequest httpRequest, String name, String filename, String contentType, String contentTransferEncoding, Charset charset, long size) {
      if (uploadHandler != null) {
        Upload upload = new Upload(vertx, req, name, filename, contentType, contentTransferEncoding, charset,
            size);
        NettyFileUpload nettyUpload = new NettyFileUpload(upload, name, filename, contentType,
            contentTransferEncoding, charset);
        uploadHandler.handle(upload);
        return nettyUpload;
      }
      return null;
    }

    @Override
    public void removeHttpDataFromClean(HttpRequest httpRequest, InterfaceHttpData interfaceHttpData) {
    }

    @Override
    public void cleanRequestHttpDatas(HttpRequest httpRequest) {
    }

    @Override
    public void cleanAllHttpDatas() {
    }
  }
}
