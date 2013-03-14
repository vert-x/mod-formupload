package org.vertx.mods.test.integration;

import org.junit.Test;
import org.vertx.java.core.Handler;
import org.vertx.java.core.SimpleHandler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientRequest;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;
import org.vertx.mods.formupload.Attribute;
import org.vertx.mods.formupload.MultipartRequest;
import org.vertx.testtools.TestVerticle;
import org.vertx.testtools.TestVerticleInfo;

import static org.vertx.testtools.VertxAssert.*;

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
 */
@TestVerticleInfo(includes="io.vertx~mod-formupload~2.0.0-SNAPSHOT")
public class FormUploadTest extends TestVerticle {

  @Test
  public void testFormUpload() {

    vertx.createHttpServer().requestHandler(new Handler<HttpServerRequest>() {
      public void handle(final HttpServerRequest req) {
        if (req.uri.startsWith("/form")) {
          req.response.setChunked(true);
          MultipartRequest mpReq = new MultipartRequest(vertx, req);
          mpReq.attributeHandler(new Handler<Attribute>() {
            @Override
            public void handle(Attribute attr) {
              req.response.write("Got attr " + attr.name + " : " + attr.value + "\n");
              // do some asserts here
              // etc
            }
          });
          req.endHandler(new SimpleHandler() {
            protected void handle() {
              req.response.end();
            }
          });
        }
      }
    }).listen(8080);

    HttpClientRequest req = vertx.createHttpClient().setPort(8080).post("/form", new Handler<HttpClientResponse>() {
      @Override
      public void handle(HttpClientResponse resp) {
        // assert the response
        assertEquals(200, resp.statusCode);
        resp.bodyHandler(new Handler<Buffer>() {
          public void handle(Buffer body) {
            // assert the body if you like
          }
        });
        testComplete();
      }
    });
    // The tricky part of this test is working out what needs to be sent to simulate the form.
    Buffer buffer = new Buffer("this is the body of the POST");
    req.headers().put("content-length", buffer.length());
    req.write(buffer).end();

  }


}