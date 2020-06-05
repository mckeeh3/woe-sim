package oti.simulator;

import akka.actor.typed.ActorSystem;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.model.*;
import akka.http.javadsl.server.Route;
import akka.stream.Materializer;
import org.slf4j.Logger;

import java.io.*;
import java.util.Collections;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static akka.http.javadsl.server.Directives.*;
import static akka.http.javadsl.server.Directives.getFromResource;

class HttpServer {
  private final ActorSystem<Void> actorSystem;

  HttpServer(String host, int port, ActorSystem<Void> actorSystem) {
    this.actorSystem = actorSystem;

    startHttpServer(host, port, actorSystem);
  }

  private void startHttpServer(String host, int port, ActorSystem<Void> actorSystem) {
    try {
      Materializer materializer = Materializer.matFromSystem(actorSystem);
      final CompletionStage<ServerBinding> serverBindingCompletionStage = Http.get(actorSystem.classicSystem())
          .bindAndHandleSync(this::handleHttpRequest, ConnectHttp.toHost(host, port), materializer);

      serverBindingCompletionStage.toCompletableFuture().get(15, TimeUnit.SECONDS);
    } catch (InterruptedException | TimeoutException | ExecutionException e) {
      log().error("Monitor HTTP server error", e);
    } finally {
      log().info("HTTP server started on port {}", port);
    }
  }

  private HttpResponse handleHttpRequest(HttpRequest httpRequest) {
    switch (httpRequest.getUri().path()) {
      case "/":
        return htmlFileResponse("oti.html");
      case "/oti.js":
        return jsFileResponse("oti.js");
      case "/p5.js":
        return jsFileResponse("p5.js");
      case "/mappa.js":
        return jsFileResponse("mappa.js");
      case "/command":
        return handleCommandRequest(httpRequest);
      default:
        return HttpResponse.create().withStatus(404);
    }
  }

  private Route route() {
    return concat(
        path("", () -> getFromResource("oti.html", ContentTypes.TEXT_HTML_UTF8))
    );
  }

  private HttpResponse htmlFileResponse(String filename) {
    try {
      final String fileContents = readFile(filename);
      return HttpResponse.create()
          .withEntity(ContentTypes.TEXT_HTML_UTF8, fileContents)
          .withStatus(StatusCodes.ACCEPTED);
    } catch (IOException e) {
      log().error(String.format("I/O error on file '%s'", filename), e);
      return HttpResponse.create().withStatus(StatusCodes.INTERNAL_SERVER_ERROR);
    }
  }

  private HttpResponse jsFileResponse(String filename) {
    try {
      final String fileContents = readFile(filename);
      return HttpResponse.create()
          .withEntity(ContentTypes.create(MediaTypes.APPLICATION_JAVASCRIPT, HttpCharsets.UTF_8), fileContents)
          .withStatus(StatusCodes.ACCEPTED);
    } catch (IOException e) {
      log().error(String.format("I/O error on file '%s'", filename), e);
      return HttpResponse.create().withStatus(StatusCodes.INTERNAL_SERVER_ERROR);
    }
  }

  private HttpResponse handleCommandRequest(HttpRequest httpRequest) {
    try {
      final String jsonContents = ""; //loadNodes(actorSystem).toJson();
      return HttpResponse.create()
          .withEntity(ContentTypes.create(MediaTypes.APPLICATION_JAVASCRIPT, HttpCharsets.UTF_8), jsonContents)
          .withHeaders(Collections.singletonList(HttpHeader.parse("Access-Control-Allow-Origin", "*")))
          .withStatus(StatusCodes.ACCEPTED);
    } catch (Exception e) {
      log().error("I/O error on JSON response");
      return HttpResponse.create().withStatus(StatusCodes.INTERNAL_SERVER_ERROR);
    }
  }

  private String readFile(String filename) throws IOException {
    final InputStream inputStream = getClass().getClassLoader().getResourceAsStream(filename);
    if (inputStream == null) {
      throw new FileNotFoundException(String.format("Filename '%s'", filename));
    } else {
      final StringBuilder fileContents = new StringBuilder();

      try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
        String line;
        while ((line = br.readLine()) != null) {
          fileContents.append(String.format("%s%n", line));
        }
      }
      return fileContents.toString();
    }
  }

  private Logger log() {
    return actorSystem.log();
  }
}
