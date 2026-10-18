Setup:
1. mongoimport --db cybershield --collection securityEvents --drop --jsonArray --file security_events.json

2. Copy .mongoshrc.js to your home directory, restart mongosh, then test:
     insertSecurityEvents("cybershield","securityEvents",5)
     findEventsByIP("cybershield","securityEvents","192.168.10.25")
     updateSeverity("cybershield","securityEvents","Brute Force Attack","Critical")
     removeLowSeverityEvents("cybershield","securityEvents")

3. Before Task 2 re-run the mongoimport (with --drop), then: mongosh cybershield task2_queries.js

4. Java app: 
     cd SOCApp
     mvn compile exec:java 
