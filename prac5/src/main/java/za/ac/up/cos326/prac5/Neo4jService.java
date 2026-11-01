package za.ac.up.cos326.prac5;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.TransactionCallback;
import org.neo4j.driver.TransactionContext;
import org.neo4j.driver.exceptions.AuthenticationException;
import org.neo4j.driver.exceptions.ClientException;
import org.neo4j.driver.exceptions.Neo4jException;
import org.neo4j.driver.exceptions.ServiceUnavailableException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * All database access for the application (Task 4.1, 4.3 and 4.4).
 *
 * <ul>
 *   <li>One {@link Driver} is created per connection and closed in {@link #close()}.</li>
 *   <li>Every analysis opens a short-lived {@link Session} (try-with-resources) and runs
 *       inside a managed read transaction ({@code session.executeRead}), so the driver
 *       handles commit / rollback / retry.</li>
 *   <li>User input is ALWAYS passed as Cypher parameters ({@code $name}) - never concatenated
 *       into the query text.</li>
 *   <li>Results come from the database; nothing is hard-coded.</li>
 * </ul>
 */
public class Neo4jService implements AutoCloseable {

    public static final int TOP_N = 5;
    /** Upper bound for the breadth-first search (a literal: Cypher cannot parameterise path bounds). */
    private static final int MAX_PATH_HOPS = 15;
    private static final int MAX_NAME_LENGTH = 100;

    private Driver driver;
    private SessionConfig sessionConfig;
    private String database;

    // ------------------------------------------------------------------ 4.1 connectivity

    /**
     * Opens the driver, verifies the server is reachable and that the database exists.
     * Any failure closes the half-open driver and is re-thrown to the caller.
     */
    public void connect(String uri, String user, String password, String databaseName) {
        close();
        Driver candidate = GraphDatabase.driver(uri, AuthTokens.basic(user, password));
        try {
            candidate.verifyConnectivity();                       // server reachable + credentials OK
            SessionConfig cfg = SessionConfig.builder().withDatabase(databaseName).build();
            try (Session probe = candidate.session(cfg)) {        // database exists + readable
                probe.executeRead(tx -> tx.run("RETURN 1 AS ok").single().get("ok").asLong());
            }
            this.driver = candidate;
            this.sessionConfig = cfg;
            this.database = databaseName;
        } catch (RuntimeException e) {
            candidate.close();
            throw e;
        }
    }

    public boolean isConnected() {
        return driver != null;
    }

    public String getDatabase() {
        return database;
    }

    /** Closes the driver (and with it every pooled connection). Safe to call repeatedly. */
    @Override
    public void close() {
        if (driver != null) {
            try {
                driver.close();
            } finally {
                driver = null;
                sessionConfig = null;
            }
        }
    }

    // ------------------------------------------------------------------ 4.3 investigation functions

    /** 4.3a Network overview: number of people and FOLLOWS relationships. */
    public QueryResult networkOverview() {
        return read(tx -> {
            Result r = tx.run("""
                    MATCH (p:Person)
                    WITH count(p) AS totalPeople
                    OPTIONAL MATCH ()-[f:FOLLOWS]->()
                    RETURN totalPeople AS `Total people`, count(f) AS `Total FOLLOWS relationships`
                    """);
            QueryResult table = toResult(r, null);
            return new QueryResult(table.columns(), table.rows(),
                    "Network overview read from database '" + database + "'.");
        });
    }

    /** 4.3b Influential accounts: the five accounts with the most followers. */
    public QueryResult influentialAccounts() {
        return read(tx -> {
            Result r = tx.run("""
                    MATCH (:Person)-[:FOLLOWS]->(p:Person)
                    RETURN p.name AS Account, count(*) AS Followers
                    ORDER BY Followers DESC, Account ASC
                    LIMIT $limit
                    """, Map.of("limit", TOP_N));
            return toResult(r, "Top " + TOP_N + " accounts by number of followers.");
        });
    }

    /** Extra: the five accounts that follow the most other accounts. */
    public QueryResult mostActiveFollowers() {
        return read(tx -> {
            Result r = tx.run("""
                    MATCH (p:Person)-[:FOLLOWS]->(:Person)
                    RETURN p.name AS Account, count(*) AS `Accounts followed`
                    ORDER BY `Accounts followed` DESC, Account ASC
                    LIMIT $limit
                    """, Map.of("limit", TOP_N));
            return toResult(r, "Top " + TOP_N + " accounts by number of accounts they follow.");
        });
    }

    /**
     * 4.3c Connection investigation: shortest directed FOLLOWS path from {@code from} to {@code to}.
     * If no path exists (or both names are the same account) an explanatory message is returned.
     */
    public QueryResult shortestPath(String from, String to) {
        validateName(from, "first account");
        validateName(to, "second account");
        return read(tx -> {
            String a = resolveAccount(tx, from);
            String b = resolveAccount(tx, to);
            List<String> cols = List.of("Step", "Account");
            if (a.equals(b)) {
                return new QueryResult(cols, List.of(), "Both fields name the same account (" + a
                        + "). Enter two different accounts.");
            }
            Result r = tx.run("""
                    MATCH (a:Person {name: $from}), (b:Person {name: $to})
                    OPTIONAL MATCH p = shortestPath((a)-[:FOLLOWS*..%d]->(b))
                    WHERE a <> b
                    RETURN [n IN nodes(p) | n.name] AS route, length(p) AS hops
                    """.formatted(MAX_PATH_HOPS), Map.of("from", a, "to", b));
            Record rec = r.single();
            if (rec.get("route").isNull()) {
                return new QueryResult(cols, List.of(), "No path exists: " + a + " cannot reach " + b
                        + " by following FOLLOWS relationships (searched up to " + MAX_PATH_HOPS
                        + " hops; FOLLOWS is directed).");
            }
            List<Object> route = rec.get("route").asList();
            List<List<Object>> rows = new ArrayList<>();
            for (int i = 0; i < route.size(); i++) {
                rows.add(List.of((long) i, route.get(i)));
            }
            String chain = route.stream().map(String::valueOf).collect(Collectors.joining("  ->  "));
            return new QueryResult(cols, rows, "Shortest path (" + rec.get("hops").asLong() + " hops): " + chain);
        });
    }

    /** 4.3d Content investigation: posts of an account with hashtag, message and repost status. */
    public QueryResult postsOfAccount(String account) {
        validateName(account, "account");
        return read(tx -> {
            String name = resolveAccount(tx, account);
            Result r = tx.run("""
                    MATCH (p:Person {name: $name})-[:POSTED]->(po:Post)
                    OPTIONAL MATCH (po)<-[rp:REPOSTED]-(:Person)
                    RETURN po.id AS `Post ID`, po.hashtag AS Hashtag, po.message AS Message,
                           count(rp) > 0 AS Reposted, count(rp) AS `Times reposted`
                    ORDER BY `Post ID`
                    """, Map.of("name", name));
            QueryResult t = toResult(r, null);
            String msg = t.isEmpty()
                    ? name + " has not posted any content."
                    : t.rows().size() + " post(s) by " + name + ".";
            return new QueryResult(t.columns(), t.rows(), msg);
        });
    }

    /** Extra: people who follow the given account. */
    public QueryResult followersOf(String account) {
        validateName(account, "account");
        return read(tx -> {
            String name = resolveAccount(tx, account);
            Result r = tx.run("""
                    MATCH (f:Person)-[:FOLLOWS]->(:Person {name: $name})
                    RETURN f.name AS Follower
                    ORDER BY Follower
                    """, Map.of("name", name));
            QueryResult t = toResult(r, null);
            return new QueryResult(t.columns(), t.rows(), t.rows().size() + " follower(s) of " + name + ".");
        });
    }

    /** Extra: accounts directly connected (incoming or outgoing FOLLOWS) with the direction. */
    public QueryResult directConnections(String account) {
        validateName(account, "account");
        return read(tx -> {
            String name = resolveAccount(tx, account);
            Result r = tx.run("""
                    MATCH (a:Person {name: $name})-[r:FOLLOWS]-(o:Person)
                    RETURN o.name AS `Connected account`,
                           CASE WHEN startNode(r) = a THEN 'OUTGOING (account follows them)'
                                ELSE 'INCOMING (they follow account)' END AS Direction
                    ORDER BY Direction, `Connected account`
                    """, Map.of("name", name));
            QueryResult t = toResult(r, null);
            return new QueryResult(t.columns(), t.rows(),
                    t.rows().size() + " direct connection(s) of " + name + ".");
        });
    }

    /** Extra: accounts reachable from the given account in one or two hops. */
    public QueryResult reachableWithinTwoHops(String account) {
        validateName(account, "account");
        return read(tx -> {
            String name = resolveAccount(tx, account);
            Result r = tx.run("""
                    MATCH (s:Person {name: $name})
                    MATCH p = (s)-[:FOLLOWS*1..2]->(o:Person)
                    WHERE o <> s
                    RETURN o.name AS `Reachable account`, min(length(p)) AS `Path length`
                    ORDER BY `Path length`, `Reachable account`
                    """, Map.of("name", name));
            QueryResult t = toResult(r, null);
            return new QueryResult(t.columns(), t.rows(), t.rows().size() + " account(s) reachable from "
                    + name + " within two FOLLOWS hops.");
        });
    }

    // ------------------------------------------------------------------ 4.4 transactions / helpers

    /**
     * Runs {@code work} in a managed read transaction on a fresh session. The session is closed
     * automatically; the driver is left open for the next analysis.
     */
    private <T> T read(TransactionCallback<T> work) {
        if (driver == null) {
            throw new IllegalStateException("Not connected to the database.");
        }
        try (Session session = driver.session(sessionConfig)) {
            return session.executeRead(work);
        }
    }

    /** Looks up an account; if the user left off the leading '@' the '@' form is tried too. */
    private static String resolveAccount(TransactionContext tx, String input) {
        String name = input.trim();
        if (accountExists(tx, name)) {
            return name;
        }
        if (!name.startsWith("@") && accountExists(tx, "@" + name)) {
            return "@" + name;
        }
        throw new InvalidAccountException("Account '" + name + "' was not found in the database.");
    }

    private static boolean accountExists(TransactionContext tx, String name) {
        return tx.run("MATCH (p:Person {name: $name}) RETURN count(p) AS c", Map.of("name", name))
                .single().get("c").asLong() > 0;
    }

    /** Basic input validation (also used by the GUI before any query is sent). */
    public static void validateName(String value, String fieldLabel) {
        if (value == null || value.isBlank()) {
            throw new InvalidAccountException("Please enter the " + fieldLabel + " name.");
        }
        if (value.trim().length() > MAX_NAME_LENGTH) {
            throw new InvalidAccountException("The " + fieldLabel + " name is too long (maximum "
                    + MAX_NAME_LENGTH + " characters).");
        }
    }

    private static QueryResult toResult(Result result, String message) {
        List<String> columns = result.keys();
        List<List<Object>> rows = new ArrayList<>();
        while (result.hasNext()) {
            Record rec = result.next();
            List<Object> row = new ArrayList<>(columns.size());
            for (String col : columns) {
                row.add(rec.get(col).asObject());
            }
            rows.add(row);
        }
        return new QueryResult(columns, rows, message);
    }

    /** Turns any exception into a short, user-friendly sentence (4.1b, 4.4b). */
    public static String describe(Throwable t) {
        if (t instanceof InvalidAccountException) {
            return t.getMessage();
        }
        if (t instanceof AuthenticationException) {
            return "Authentication failed - check the username and password.";
        }
        if (t instanceof ServiceUnavailableException) {
            return "Cannot reach Neo4j at the given URI - is the DBMS started and the URI correct?";
        }
        if (t instanceof ClientException ce && ce.code() != null && ce.code().contains("DatabaseNotFound")) {
            return "The database does not exist - create it first (see README / Task1Queries.txt).";
        }
        if (t instanceof Neo4jException) {
            return "Database error: " + t.getMessage();
        }
        if (t instanceof IllegalStateException) {
            return t.getMessage();
        }
        if (t instanceof IllegalArgumentException) {
            return "Invalid connection settings: " + t.getMessage();
        }
        return "Unexpected error: " + t;
    }
}
