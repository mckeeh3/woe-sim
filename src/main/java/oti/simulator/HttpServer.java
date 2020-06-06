package oti.simulator;

import akka.actor.typed.ActorSystem;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.model.ContentTypes;
import akka.http.javadsl.model.StatusCodes;
import akka.http.javadsl.server.Route;
import akka.stream.Materializer;
import org.slf4j.Logger;

import static akka.http.javadsl.server.Directives.*;

class HttpServer {
  private final ActorSystem<Void> actorSystem;

  HttpServer(String host, int port, ActorSystem<Void> actorSystem) {
    this.actorSystem = actorSystem;

    startHttpServer(host, port, actorSystem);
  }

  private void startHttpServer(String host, int port, ActorSystem<Void> actorSystem) {
    Materializer materializer = Materializer.matFromSystem(actorSystem);

    Http.get(actorSystem.classicSystem())
        .bindAndHandle(route().flow(actorSystem.classicSystem(), materializer),
            ConnectHttp.toHost(host, port), materializer);
  }

  private Route route() {
    return concat(
        path("", () -> getFromResource("oti.html", ContentTypes.TEXT_HTML_UTF8)),
        path("", () -> getFromResource("oti.js", ContentTypes.APPLICATION_JSON)),
        path("", () -> getFromResource("p5.js", ContentTypes.APPLICATION_JSON)),
        path("", () -> getFromResource("mappa.js", ContentTypes.APPLICATION_JSON)),
        path("selection-create", () -> concat(
            post(() -> entity(
                Jackson.unmarshaller(Region.SelectionCreate.class),
                selectionCreate -> {
                  return complete(StatusCodes.CREATED, selectionCreate, Jackson.marshaller());
                })
            )
        ))
    );
  }

  private Logger log() {
    return actorSystem.log();
  }
}
