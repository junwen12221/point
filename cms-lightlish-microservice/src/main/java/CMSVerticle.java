import cn.lightfish.common.RestAPIVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import sun.util.locale.StringTokenIterator;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Created by karak on 16-10-21.
 */
public class CMSVerticle extends RestAPIVerticle {

    public static void main(String[] args) {
        String input = "1+2+(3+9)";
        List<String> tokens = Arrays.asList(input.split(""));
        Iterator<String> it = tokens.iterator();

    }

    @Override
    public void start() throws Exception {

        JsonObject config = Vertx.currentContext().config();

        String uri = config.getString("mongo_uri");
        if (uri == null) {
            uri = "mongodb://localhost:27017";
        }
        String db = config.getString("mongo_db");
        if (db == null) {
            db = "test";
        }

        JsonObject mongoconfig = new JsonObject()
                .put("connection_string", uri)
                .put("db_name", db);

        MongoClient mongoClient = MongoClient.createShared(vertx, mongoconfig);

        JsonObject product1 = new JsonObject().put("itemId", "12345").put("name", "Cooler").put("price", "100.0");

        mongoClient.save("products", product1, id -> {
            System.out.println("Inserted id: " + id.result());

            mongoClient.find("products", new JsonObject().put("itemId", "12345"), res -> {
                System.out.println("Name is " + res.result().get(0).getString("name"));

                mongoClient.remove("products", new JsonObject().put("itemId", "12345"), rs -> {
                    if (rs.succeeded()) {
                        System.out.println("Product removed ");
                    }
                });

            });

        });

    }
}
