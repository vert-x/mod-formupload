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


import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.platform.Verticle;
import org.vertx.mods.formupload.MultipartRequest;
import org.vertx.mods.formupload.Upload;

public class SimpleUploadServer extends Verticle {

  public void start() {
    vertx.createHttpServer().requestHandler(new Handler<HttpServerRequest>() {
      public void handle(final HttpServerRequest req) {
        if (req.uri().equals("/")) {
          // Serve the index page
          req.response().sendFile("index.html");
        } else if (req.uri().startsWith("/form")) {
          MultipartRequest mpReq = new MultipartRequest(vertx, req);
          mpReq.uploadHandler(new Handler<Upload>() {
            @Override
            public void handle(final Upload upload) {
              upload.streamToDisk(upload.filename, new AsyncResultHandler<Void>() {
                @Override
                public void handle(AsyncResult<Void> res) {
                  if (res.succeeded()) {
                    req.response().end("Upload successful, you should see the file in the server directory");
                  } else {
                    req.response().end("Upload failed");
                  }
                }
              });
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
