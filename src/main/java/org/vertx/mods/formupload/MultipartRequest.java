package org.vertx.mods.formupload;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.handler.codec.http.*;
import org.jboss.netty.handler.codec.http.multipart.*;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public class MultipartRequest {

  private final Vertx vertx;
  private final HttpServerRequest req;
  private Handler<Attribute> attrHandler;
  private Handler<Upload> uploadHandler;
  private final Map<String, String> attributes = new HashMap<>();
  private final HttpRequest nettyReq;

  private HttpPostRequestDecoder decoder;

  public MultipartRequest(Vertx vertx, HttpServerRequest req) {
    this.vertx = vertx;
    this.req = req;
    nettyReq = new DefaultHttpRequest(HttpVersion.HTTP_1_1, new HttpMethod(req.method), req.uri);
    for (Map.Entry<String, String> header: req.headers().entrySet()) {
      nettyReq.addHeader(header.getKey(), header.getValue());
    }
    try {
      decoder = new HttpPostRequestDecoder(new DataFactory(), nettyReq);
    } catch (Exception e) {
      throw convertException(e);
    }
    req.dataHandler(new Handler<Buffer>() {
      public void handle(Buffer data) {
        try {
          decoder.offer(new DefaultHttpChunk(data.getChannelBuffer()));
        } catch (Exception e) {
          throw convertException(e);
        }
      }
    });
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

  private RuntimeException convertException(Exception e) {
    RuntimeException re = new RuntimeException(e.getMessage());
    re.setStackTrace(e.getStackTrace());
    return re;
  }

  private class NettyFileUpload implements FileUpload {

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
    public void setContent(ChannelBuffer channelBuffer) throws IOException {
      completed = true;
      upload.receiveData(new Buffer(channelBuffer));
      upload.complete();
    }

    @Override
    public void addContent(ChannelBuffer channelBuffer, boolean last) throws IOException {
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
    public ChannelBuffer getChannelBuffer() throws IOException {
      throw new UnsupportedOperationException();
    }

    @Override
    public ChannelBuffer getChunk(int i) throws IOException {
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
  }


  private class InternalMemoryAttribute extends MemoryAttribute {

    private InternalMemoryAttribute(String name) {
      super(name);
    }

    private InternalMemoryAttribute(String name, String value) throws IOException {
      super(name, value);
    }

    @Override
    public void setContent(ChannelBuffer channelBuffer) throws IOException {
      super.setContent(channelBuffer);
      attributeCreated();
    }

    @Override
    public void addContent(ChannelBuffer channelBuffer, boolean last) throws IOException {
      super.addContent(channelBuffer, last);
      if (isCompleted()) {
        attributeCreated();
      }
    }

    void attributeCreated() {
      if (!getName().equals("name")) {
        // Netty has a habit of adding multiple extra attributes of name 'name' and value of the name of the
        // real attribute, so we screen these out.
        // This is however a problem - what if the user has a real attribute called 'name'?
        attributes.put(getName(), getValue());
        if (attrHandler != null) {
          attrHandler.handle(new Attribute(getName(), getValue()));
        }
      }
    }
  }


  private class DataFactory implements HttpDataFactory {

    @Override
    public org.jboss.netty.handler.codec.http.multipart.Attribute createAttribute(HttpRequest httpRequest, String name) {
      org.jboss.netty.handler.codec.http.multipart.Attribute nettyAttr;
      try {
        nettyAttr = new InternalMemoryAttribute(name);
      } catch (Exception e) {
        throw convertException(e);
      }
      return nettyAttr;
    }

    @Override
    public org.jboss.netty.handler.codec.http.multipart.Attribute createAttribute(HttpRequest httpRequest,
                                                                                  String name, String value) {
      org.jboss.netty.handler.codec.http.multipart.Attribute nettyAttr;
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
