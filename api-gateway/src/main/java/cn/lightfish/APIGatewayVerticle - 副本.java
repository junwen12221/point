/*
package cn.lightfish;

import cn.lightfish.common.RestAPIVerticle;
import cn.lightfish.common.functional.Functional;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CookieHandler;
import io.vertx.ext.web.handler.JWTAuthHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.redis.RedisClient;
import io.vertx.redis.RedisOptions;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.types.HttpEndpoint;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class APIGatewayVerticle extends RestAPIVerticle {
    private static final Logger logger = LoggerFactory.getLogger(APIGatewayVerticle.class);

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(APIGatewayVerticle.class.getName());
    }

    @Override
    public void start(Future<Void> future) throws Exception {
        super.start();
        final RedisClient client = RedisClient.create(vertx,
                new RedisOptions().setHost("127.0.0.1"));
        String host = config().getString("api.gateway.http.address", "localhost");
        int port = config().getInteger("api.gateway.http.port", 80);
        Router router = Router.router(vertx);
        router.route().handler(CookieHandler.create());
        router.route().handler(BodyHandler.create());
        JWTAuth jwt = JWTAuth.create(vertx, new JsonObject()
                .put("keyStore", new JsonObject()
                        .put("type", "jceks")
                        .put("path", "D:\\lightfish-point\\api-gateway\\src\\main\\java\\cn\\lightfish\\keystore.jceks")
                        .put("password", "secret")));
        router.route("/api/cmd*/
/*").handler(JWTAuthHandler.create(jwt, "/api/newToken"));
        router.post("/api/newToken").handler(ctx -> {
            ctx.response().putHeader("Content-Type", "text/plain");
            client.get(ctx.request().getParam("username"), (r) -> {
                if (r.succeeded()) {
                    String pwd = r.result();
                    if (pwd == null) {
                        ctx.response().end(new JsonObject().put("message", "User does not exist").encode());
                    } else {
                        if (ctx.request().getParam("password").equals(pwd)) {
                            JWTOptions opt = new JWTOptions().setExpiresInSeconds(6000L);
                            String token = jwt.generateToken(new JsonObject(), opt);
                            ctx.response().end(token);
                        } else {
                            ctx.response().end(new JsonObject().put("message", "wrong password").encode());
                        }
                    }
                } else {
                    ctx.response().end(new JsonObject().put("message", "system error").encode());
                }
            });
        });
        router.route("/api/cmd/1").handler((s) -> s.response().end("5555555"));

        router.get("/api/v").handler(this::apiVersion);
        String hostURI = String.format("https://localhost:%d", port);
        router.route("/api*/
/*").handler(this::dispatchRequests);
        router.route("*/
/*").handler(StaticHandler.create());
        vertx.createHttpServer()
                .requestHandler(router::accept)
                .listen(port, host, ar -> {
                    if (ar.succeeded()) {
                        publishApiGateway(host, port);
                        future.complete();
                        logger.info("API Gateway is running on port " + port);
                        publishGatewayLog("api_gateway_init_success:" + port);
                    } else {
                        future.fail(ar.cause());
                    }
                });

    }

    private void dispatchRequests(RoutingContext context) {
        int initialOffset = 5; // length of `/api/`
        // run with circuit breaker in order to deal with failure
        circuitBreaker.execute(future -> {
            getAllEndpoints().setHandler(ar -> {
                if (ar.succeeded()) {
                    List<Record> recordList = ar.result();
                    // get relative path and retrieve prefix to dispatch client
                    String path = context.request().uri();

                    if (path.length() <= initialOffset) {
                        notFound(context);
                        future.complete();
                        return;
                    }
                    String prefix = (path.substring(initialOffset)
                            .split("/"))[0];
                    // generate new relative path
                    String newPath = path.substring(initialOffset + prefix.length());
                    // get one relevant HTTP client, may not exist
                    Optional<HttpClient> client = recordList.stream()
                            .filter(record -> record.getMetadata().getString("api.name") != null)
                            .filter(record -> record.getMetadata().getString("api.name").equals(prefix))
                            .map(record -> (HttpClient) discovery.getReference(record).get())
                            .findAny(); // simple load balance

                    if (client.isPresent()) {
                        doDispatch(context, newPath, client.get(), future);
                    } else {
                        notFound(context);
                        future.complete();
                    }
                } else {
                    future.fail(ar.cause());
                }
            });
        }).setHandler(ar -> {
            if (ar.failed()) {
                badGateway(ar.cause(), context);
            }
        });
    }

    */
/**
     * Dispatch the request to the downstream REST layers.
     *
     * @param context routing context instance
     * @param path    relative path
     * @param client  relevant HTTP client
     *//*

    private void doDispatch(RoutingContext context, String path, HttpClient client, Future<Object> cbFuture) {
        HttpClientRequest toReq = client
                .request(context.request().method(), path, response -> {
                    response.bodyHandler(body -> {
                        if (response.statusCode() >= 500) { // api endpoint server error, circuit breaker should fail
                            cbFuture.fail(response.statusCode() + ": " + body.toString());
                        } else {
                            HttpServerResponse toRsp = context.response()
                                    .setStatusCode(response.statusCode());
                            response.headers().forEach(header -> {
                                toRsp.putHeader(header.getKey(), header.getValue());
                            });
                            // send response
                            toRsp.end(body);
                            cbFuture.complete();
                        }
                    });
                });
        // set headers
        context.request().headers().forEach(header -> {
            toReq.putHeader(header.getKey(), header.getValue());
        });
        if (context.user() != null) {
            toReq.putHeader("user-principal", context.user().principal().encode());
        }
        // send request
        if (context.getBody() == null) {
            toReq.end();
        } else {
            toReq.end(context.getBody());
        }
    }

    private void apiVersion(RoutingContext context) {
        context.response()
                .end(new JsonObject().put("version", "v1").encodePrettily());
    }

    */
/**
     * Get all REST endpoints from the service discovery infrastructure.
     *
     * @return async result
     *//*

    private Future<List<Record>> getAllEndpoints() {
        Future<List<Record>> future = Future.future();
        discovery.getRecords(record -> record.getType().equals(HttpEndpoint.TYPE),
                future.completer());
        return future;
    }


    */
/**
     * Send heart-beat check request to every REST node in every interval and await response.
     *
     * @return async result. If all nodes are active, the result will be assigned `true`, else the result will fail
     *//*

    private Future<Object> sendHeartBeatRequest() {
        final String HEARTBEAT_PATH = config().getString("heartbeat.path", "/health");
        return getAllEndpoints()
                .compose(records -> {
                    List<Future<JsonObject>> statusFutureList = records.stream()
                            .filter(record -> record.getMetadata().getString("api.name") != null)
                            .map(record -> { // for each client, send heart beat request
                                String apiName = record.getMetadata().getString("api.name");
                                HttpClient client = discovery.getReference(record).get();

                                Future<JsonObject> future = Future.future();
                                client.get(HEARTBEAT_PATH, response -> {
                                    future.complete(new JsonObject()
                                            .put("name", apiName)
                                            .put("status", healthStatus(response.statusCode()))
                                    );
                                })
                                        .exceptionHandler(future::fail)
                                        .end();
                                return future;
                            })
                            .collect(Collectors.toList());
                    return Functional.sequenceFuture(statusFutureList); // get all responses
                })
                .map(List::stream)
                .compose(statusList -> {
                    boolean notHealthy = statusList.anyMatch(status -> !status.getBoolean("status"));

                    if (notHealthy) {
                        String issues = statusList.filter(status -> !status.getBoolean("status"))
                                .map(status -> status.getString("name"))
                                .collect(Collectors.joining(", "));

                        String err = String.format("Heart beat check fail: %s", issues);
                        // publish log
                        publishGatewayLog(err);
                        return Future.failedFuture(new IllegalStateException(err));
                    } else {
                        // publish log
                        publishGatewayLog("api_gateway_heartbeat_check_success");
                        return Future.succeededFuture("OK");
                    }
                });
    }

    private boolean healthStatus(int code) {
        return code == 200;
    }

    // log methods

    private void publishGatewayLog(String info) {
        JsonObject message = new JsonObject()
                .put("info", info)
                .put("time", System.currentTimeMillis());
        publishLogEvent("gateway", message);
    }

    private void publishGatewayLog(JsonObject msg) {
        JsonObject message = msg.copy()
                .put("time", System.currentTimeMillis());
        publishLogEvent("gateway", message);
    }
}
*/
