/*
 * Copyright 2013 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * @author <a href="http://tfox.org">Tim Fox</a>
 * @author <a href="mailto:nmaurer@redhat.com">Norman Maurer</a>
 */
package org.vertx.mods.test.integration;

import org.junit.Ignore;
import org.junit.Test;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.VoidHandler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpClientRequest;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.mods.formupload.Attribute;
import org.vertx.mods.formupload.MultipartRequest;
import org.vertx.mods.formupload.Upload;
import org.vertx.testtools.TestVerticle;
import org.vertx.testtools.TestVerticleInfo;


import java.util.concurrent.atomic.AtomicInteger;

import static org.vertx.testtools.VertxAssert.*;

@TestVerticleInfo(includes="io.vertx~mod-formupload~2.0.0-SNAPSHOT")
public class FormUploadTest extends TestVerticle {

  @Test
  public void testFormUploadFile() throws Exception {

    final AtomicInteger attributeCount = new AtomicInteger();
    final String content = "Vert.x rocks!";
    vertx.createHttpServer().requestHandler(new Handler<HttpServerRequest>() {
      public void handle(final HttpServerRequest req) {
        if (req.uri().startsWith("/form")) {
          req.response().setChunked(true);
          final MultipartRequest mpReq = new MultipartRequest(vertx, req);
          mpReq.attributeHandler(new Handler<Attribute>() {
            @Override
            public void handle(Attribute attr) {
              attributeCount.incrementAndGet();
              assertEquals("name", attr.name);
              assertEquals("file", attr.value);
              mpReq.attributeHandler(new Handler<Attribute>() {
                @Override
                public void handle(Attribute attr) {
                  attributeCount.incrementAndGet();
                  assertEquals("filename", attr.name);
                  assertEquals("tmp-0.txt", attr.value);
                  mpReq.attributeHandler(new Handler<Attribute>() {
                    @Override
                    public void handle(Attribute attr) {
                      attributeCount.incrementAndGet();
                      assertEquals("Content-Type", attr.name);
                      assertEquals("image/gif", attr.value);
                    }
                  });
                }
              });
            }
          });
          mpReq.uploadHandler(new Handler<Upload>() {
            @Override
            public void handle(final Upload event) {
              event.dataHandler(new Handler<Buffer>() {
                @Override
                public void handle(Buffer buffer) {
                  assertEquals(content, buffer.toString("UTF-8"));
                }
              });
            }
          });
          mpReq.endHandler(new VoidHandler() {
            protected void handle() {
              req.response().end();
            }
          });
        }
      }
    }).listen(8080, "0.0.0.0", new AsyncResultHandler<HttpServer>() {
      @Override
      public void handle(AsyncResult<HttpServer> ar) {
        assertTrue(ar.succeeded());
        HttpClientRequest req = vertx.createHttpClient().setPort(8080).post("/form", new Handler<HttpClientResponse>() {
          @Override
          public void handle(HttpClientResponse resp) {
            // assert the response
            assertEquals(200, resp.statusCode());
            resp.bodyHandler(new Handler<Buffer>() {
              public void handle(Buffer body) {
                assertEquals(0, body.length());
              }
            });
            assertEquals(3, attributeCount.get());
            testComplete();
          }
        });

        final String boundary = "dLV9Wyq26L_-JQxk6ferf-RT153LhOO";
        Buffer buffer = new Buffer();
        final String body =
                "--" + boundary + "\r\n" +
                        "Content-Disposition: form-data; name=\"file\"; filename=\"tmp-0.txt\"\r\n" +
                        "Content-Type: image/gif\r\n" +
                        "\r\n" +
                        content + "\r\n" +
                        "--" + boundary + "--\r\n";

        buffer.appendString(body);
        req.headers().set("content-length", String.valueOf(buffer.length()));
        req.headers().set("content-type", "multipart/form-data; boundary=" + boundary);
        req.write(buffer).end();      }
    });
  }

  @Test
  @Ignore
  public void testFormUploadAttributes() throws Exception {
    final AtomicInteger attributeCount = new AtomicInteger();
    vertx.createHttpServer().requestHandler(new Handler<HttpServerRequest>() {
      public void handle(final HttpServerRequest req) {
        if (req.uri().startsWith("/form")) {
          req.response().setChunked(true);
          final MultipartRequest mpReq = new MultipartRequest(vertx, req);
          mpReq.attributeHandler(new Handler<Attribute>() {
            @Override
            public void handle(Attribute attr) {
              attributeCount.incrementAndGet();
              assertEquals("framework", attr.name);
              assertEquals("vertx", attr.value);
              mpReq.attributeHandler(new Handler<Attribute>() {
                @Override
                public void handle(Attribute attr) {
                  attributeCount.incrementAndGet();
                  assertEquals("runson", attr.name);
                  assertEquals("jvm", attr.value);
                }
              });
            }
          });
          mpReq.uploadHandler(new Handler<Upload>() {
            @Override
            public void handle(final Upload event) {
              event.dataHandler(new Handler<Buffer>() {
                @Override
                public void handle(Buffer buffer) {
                  fail();
                }
              });
            }
          });
          mpReq.endHandler(new VoidHandler() {
            protected void handle() {
              req.response().end();
            }
          });
        }
      }
    }).listen(8080, "0.0.0.0", new AsyncResultHandler<HttpServer>() {
      @Override
      public void handle(AsyncResult<HttpServer> ar) {
        assertTrue(ar.succeeded());
        HttpClientRequest req = vertx.createHttpClient().setPort(8080).post("/form", new Handler<HttpClientResponse>() {
          @Override
          public void handle(HttpClientResponse resp) {
            // assert the response
            assertEquals(200, resp.statusCode());
            resp.bodyHandler(new Handler<Buffer>() {
              public void handle(Buffer body) {
                assertEquals(0, body.length());
              }
            });
            assertEquals(2, attributeCount.get());
            testComplete();
          }
        });
        Buffer buffer = new Buffer();
        buffer.appendString("framework=vertx&runson=jvm");
        req.headers().set("content-length", String.valueOf(buffer.length()));
        req.headers().set("content-type", "application/x-www-form-urlencoded");
        req.write(buffer).end();      }
    });
  }

  @Test
  public void testFormUploadAttributes2() throws Exception {
    final AtomicInteger attributeCount = new AtomicInteger();
    vertx.createHttpServer().requestHandler(new Handler<HttpServerRequest>() {
      public void handle(final HttpServerRequest req) {
        if (req.uri().startsWith("/form")) {
          req.response().setChunked(true);
          final MultipartRequest mpReq = new MultipartRequest(vertx, req);
          mpReq.attributeHandler(new Handler<Attribute>() {
            @Override
            public void handle(Attribute attr) {
              attributeCount.incrementAndGet();
              assertEquals("origin", attr.name);
              assertEquals("junit-testUserAlias", attr.value);
              mpReq.attributeHandler(new Handler<Attribute>() {
                @Override
                public void handle(Attribute attr) {
                  attributeCount.incrementAndGet();
                  assertEquals("login", attr.name);
                  assertEquals("admin%40foo.bar", attr.value);
                  mpReq.attributeHandler(new Handler<Attribute>() {
                    @Override
                    public void handle(Attribute attr) {
                      attributeCount.incrementAndGet();
                      assertEquals("password", attr.name);
                      assertEquals("admin", attr.value);
                    }
                  });
                }
              });
            }
          });
          mpReq.uploadHandler(new Handler<Upload>() {
            @Override
            public void handle(final Upload event) {
              event.dataHandler(new Handler<Buffer>() {
                @Override
                public void handle(Buffer buffer) {
                  fail();
                }
              });
            }
          });
          mpReq.endHandler(new VoidHandler() {
            protected void handle() {
              req.response().end();
            }
          });
        }
      }
    }).listen(8080, "0.0.0.0", new AsyncResultHandler<HttpServer>() {
      @Override
      public void handle(AsyncResult<HttpServer> ar) {
        assertTrue(ar.succeeded());
        HttpClientRequest req = vertx.createHttpClient().setPort(8080).post("/form", new Handler<HttpClientResponse>() {
          @Override
          public void handle(HttpClientResponse resp) {
            // assert the response
            assertEquals(200, resp.statusCode());
            resp.bodyHandler(new Handler<Buffer>() {
              public void handle(Buffer body) {
                assertEquals(0, body.length());
              }
            });
            assertEquals(3, attributeCount.get());
            testComplete();
          }
        });
        Buffer buffer = new Buffer();
        buffer.appendString("origin=junit-testUserAlias&login=admin%40foo.bar&password=admin");
        req.headers().set("content-length", String.valueOf(buffer.length()));
        req.headers().set("content-type", "application/x-www-form-urlencoded");
        req.write(buffer).end();
      }
    });
  }
}