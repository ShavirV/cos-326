package za.ac.up.cos326;

import com.mongodb.client.*;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

/** All database access for the SOC application. */
public class MongoService implements AutoCloseable {

    private static final String DB_NAME = "cybershield";
    private static final String COL_NAME = "securityEvents";

    private MongoClient client;
    private MongoCollection<Document> collection;

    /** Question 3.1 - connect and verify the server is reachable. Throws if it cannot. */
    public void connect(String uri) {
        close();
        client = MongoClients.create(uri);
        // ping forces server selection, so a bad connection fails here instead of on first query
        client.getDatabase("admin").runCommand(new Document("ping", 1));
        collection = client.getDatabase(DB_NAME).getCollection(COL_NAME);
    }

    public boolean isConnected() {
        return collection != null;
    }

    /** Question 3.3 - all events for a source IP. */
    public List<Document> findBySourceIP(String sourceIP) {
        return collection.find(Filters.eq("sourceIP", sourceIP))
                .sort(Sorts.descending("timestamp"))
                .into(new ArrayList<>());
    }

    /** Question 3.4 - update severity of one event. Returns true if an event with that ID exists. */
    public boolean updateSeverity(String eventId, String newSeverity) {
        var result = collection.updateOne(Filters.eq("eventId", eventId),
                Updates.set("severity", newSeverity));
        return result.getMatchedCount() > 0;
    }

    /** Question 3.5 - number of events per severity, from an aggregation pipeline. */
    public List<Document> severitySummary() {
        return collection.aggregate(List.of(
                Aggregates.group("$severity", Accumulators.sum("numberOfEvents", 1)),
                Aggregates.sort(Sorts.descending("numberOfEvents"))
        )).into(new ArrayList<>());
    }

    @Override
    public void close() {
        if (client != null) {
            client.close();
            client = null;
            collection = null;
        }
    }
}
