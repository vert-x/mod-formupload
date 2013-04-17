/*
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import org.vertx.java.core.Handler;
import org.vertx.java.core.VoidHandler;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.platform.Verticle;
import org.vertx.mods.formupload.Attribute;
import org.vertx.mods.formupload.MultipartRequest;

public class SimpleFormServer extends Verticle {

  public void start() {
    vertx.createHttpServer().requestHandler(new Handler<HttpServerRequest>() {
      public void handle(final HttpServerRequest req) {
        if (req.uri().equals("/")) {
          // Serve the index page
          req.response().sendFile("index.html");
        } else if (req.uri().startsWith("/form")) {
          req.response().setChunked(true);
          MultipartRequest mpReq = new MultipartRequest(vertx, req);
          mpReq.attributeHandler(new Handler<Attribute>() {
            @Override
            public void handle(Attribute attr) {
              req.response().write("Got attr " + attr.name + " : " + attr.value + "\n");
            }
          });
          req.endHandler(new VoidHandler() {
            protected void handle() {
              req.response().end();
            }
          });
        } else {
          req.response().setStatusCode(404);
          req.response().end();
        }
      }
    }).listen(8080);
  }
}
