=====================================================================
COS326 Practical 5 2026 - Social Network Investigation (Neo4j + Java)
Student: u12345678            (replace with your student number)
=====================================================================

CONTENTS OF THIS ARCHIVE
  Task1Queries.txt   all Cypher queries for Tasks 1-3 (import, queries, paths, reports)
  src/               Java source (Swing GUI + Neo4j Java Driver)
  pom.xml            Maven build file (driver dependency + runnable jar)
  config.properties  default connection settings for the application
  screenshots/       screenshots of the import, queries and application
  README.txt         this file

REQUIREMENTS
  * Neo4j Desktop / Neo4j Enterprise 5.x, 2025.x or 2026.x  (see "Community Edition" below)
  * JDK 17 or newer
  * Apache Maven 3.8+  (or any IDE with Maven support: IntelliJ IDEA, Eclipse, VS Code)

---------------------------------------------------------------------
PART A - PREPARE THE DATABASE (Tasks 1-3)
---------------------------------------------------------------------
1. Start your Neo4j DBMS (Neo4j Desktop: "Start"; remember the password you set
   for the user "neo4j").

2. Put persons.csv and follows.csv into the DBMS *import* folder
   (Neo4j Desktop: DBMS "..." menu -> Open folder -> Import).
   LOAD CSV in Task1Queries.txt reads  file:///persons.csv  and  file:///follows.csv
   from that folder. (If you change the folder with server.directories.import - or
   dbms.directories.import on Neo4j 4.x - put the files there instead.)

3. Open Neo4j Browser and run the statements in Task1Queries.txt IN ORDER, one at
   a time:
      Q1.1 setup   CREATE DATABASE COS326Prac5   (run in the "system" database)
                   then  :use COS326Prac5  and create the constraints
      Q1.1a / Q1.1b   import persons and follows
      Q1.2 ...        verification        -> expect 8828 Person nodes, 4907 FOLLOWS
      Q2.x, Q3.x      queries  (Q2.3a/b create the Post nodes and POSTED / REPOSTED /
                      LIKED relationships that the Java application's "Content
                      investigation" reads - run them before testing that function)
   Lines starting with ':' (:use, :param) are Browser commands; ':param' is where
   the investigator "enters" account names for the path queries.

   Community Edition: it supports only ONE user database, so CREATE DATABASE fails.
   Use Neo4j Desktop (its DBMS allows several databases) or, if you must use Community,
   skip the CREATE DATABASE step, run everything in the default database "neo4j" and set
   neo4j.database=neo4j in config.properties / the GUI.

Data issues found and handled (so the import is correct, not just "runs")
  * CSV headers are ID,name (persons) and person,follows (follows). The upper-case
    column is read as row.ID and stored as the integer property id.
  * 5 malformed rows in persons.csv (IDs 271, 1514, 2405, 3741, 7286) contain stray
    quote characters and a trailing comma, and the same mangled names appear in
    follows.csv. The queries strip '"' and a trailing ',' so those accounts import as
    e.g.  271 / @all_stars_fade  and their relationships can be matched.
  * 63 rows of follows.csv name accounts that are not in persons.csv. Relationships must
    connect existing Person nodes, so these rows are skipped (no phantom nodes are
    created).  4970 rows - 63 skipped = 4907 FOLLOWS relationships.
  * 8 accounts follow themselves; kept (the reachability query excludes the start node).
  * The data only shows who follows whom; it does not establish that any account is
    malicious. Posts/likes/reposts are illustrative data created by Task 2.3.

---------------------------------------------------------------------
PART B - CONFIGURE AND RUN THE JAVA APPLICATION (Task 4)
---------------------------------------------------------------------
Configuration (any one of these):
  * Edit config.properties (uri, user, password, database), OR
  * just type the values into the "Database connection" panel when the app starts.
  Defaults: bolt://localhost:7687, user neo4j, database COS326Prac5.
  (config.properties is read from the directory the program is started in.)

Run - option 1, Maven:
      mvn compile exec:java
Run - option 2, runnable jar:
      mvn package
      java -jar target/prac5-1.0-jar-with-dependencies.jar
Run - option 3, IDE: open the folder as a Maven project and run
      za.ac.up.cos326.prac5.Main

Using the application
  1. Check URI / user / password / database and press "Connect". A green/red message
     says whether the connection succeeded (wrong password, DBMS stopped or missing
     database each give a specific message).
  2. Choose an analysis; the account-name fields enable only when needed:
        Network overview                 -> people + FOLLOWS counts            (4.3a)
        Influential accounts             -> top 5 by followers                 (4.3b)
        Connection investigation A -> B  -> shortest FOLLOWS path or a
                                            "No path exists" message           (4.3c)
        Content investigation (A)        -> posts: id, hashtag, message,
                                            reposted? (+ number of reposts)    (4.3d)
        extras: followers of A, direct connections of A, reachable in 1-2 hops,
                most active followers.
  3. Press "Run analysis" (or Enter in a name field). Results appear in the table;
     the line below shows a summary or a red error message. Column headers sort.
  4. Closing the window closes the driver (and any session) cleanly.

Things to try (expected results on the supplied data)
  Network overview ........ 8828 people, 4907 FOLLOWS
  Influential accounts .... @mileycyrus 30, @tommcfly 21, @ddlovato 17, @Kal_Penn 16, @Schofe 14
  Connection  @jowhit -> @DonnaMirfield : 5 hops
        @jowhit -> @nessie111 -> @olafsearson -> @Ste1987 -> @belladonna20 -> @DonnaMirfield
  Connection  @mileycyrus -> @jowhit  : "No path exists..."  (she follows nobody; FOLLOWS is directed)
  Content     @mileycyrus : P001 #Security, reposted 2 times  (after running Q2.3)
  Errors      empty field -> "Please enter ..."; "@nobody_here" -> "Account ... was not found"

---------------------------------------------------------------------
DESIGN NOTES (Task 4.4 and implementation requirements)
---------------------------------------------------------------------
  * Neo4jService is the only class that talks to Neo4j. Every analysis opens a
    Session in try-with-resources and runs inside a managed read transaction
    (session.executeRead), so the driver commits / retries / releases correctly.
  * All user input (account names) is passed as Cypher PARAMETERS ($from, $to, $name);
    never concatenated into query text. (The 15-hop search limit is a constant: Cypher
    cannot parameterise path bounds.)
  * No results are hard-coded; every table is built from the records returned.
  * Errors (bad URI, wrong password, missing database, unknown account, lost connection,
    Cypher errors) are caught and shown in the message line; the application keeps running.
  * Queries run on a SwingWorker thread so the GUI never freezes.
  * Account lookup is forgiving: "mileycyrus" is tried as "@mileycyrus" too.
