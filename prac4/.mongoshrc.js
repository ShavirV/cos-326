// COS326 Practical 4 2026 - Task 1: MongoDB shell functions
// Place this file in your home directory (~/.mongoshrc.js); mongosh loads it on start-up.

// ---------------------------------------------------------------
// Question 1.2 - Insert security events
// ---------------------------------------------------------------
function insertSecurityEvents(dbName, colName, count) {
    if (!Number.isInteger(count) || count <= 0) {
        print("Error: count must be a positive integer.");
        return;
    }
    var col = db.getSiblingDB(dbName).getCollection(colName);

    var eventTypes = [
        { type: "Brute Force Attack",     source: "Authentication Server", tags: ["authentication", "brute-force", "login"],
          desc: "Multiple failed authentication attempts detected." },
        { type: "Malware Detection",      source: "Endpoint Security",     tags: ["malware", "endpoint"],
          desc: "Malicious executable detected on a workstation." },
        { type: "Port Scan",              source: "Network IDS",           tags: ["network", "reconnaissance", "port-scan"],
          desc: "Repeated connection attempts across multiple ports." },
        { type: "Suspicious Login",       source: "Authentication Server", tags: ["authentication", "login"],
          desc: "Authentication from an unusual location detected." },
        { type: "Phishing Email",         source: "Email Gateway",         tags: ["phishing", "email"],
          desc: "Email containing a malicious link was delivered." },
        { type: "SQL Injection",          source: "Web Application Firewall", tags: ["web", "sql-injection", "attack"],
          desc: "SQL injection pattern detected in web request." }
    ];
    var severities = ["Low", "Medium", "High", "Critical"];
    var statuses   = ["Pending", "Completed"];

    function pick(arr) { return arr[Math.floor(Math.random() * arr.length)]; }
    function randIP(prefix) {
        return prefix + "." + (1 + Math.floor(Math.random() * 254)) + "." + (1 + Math.floor(Math.random() * 254));
    }

    // Unique IDs: continue after the highest existing numeric EVT id
    var next = 10001;
    var last = col.find({ eventId: /^EVT\d+$/ }, { eventId: 1 }).toArray();
    last.forEach(function (d) {
        var n = parseInt(d.eventId.substring(3), 10);
        if (n >= next) next = n + 1;
    });

    var docs = [];
    for (var i = 0; i < count; i++) {
        var t = pick(eventTypes);
        docs.push({
            eventId: "EVT" + (next + i),
            timestamp: new Date().toISOString().replace(/\.\d+Z$/, "Z"),
            sourceIP: randIP(pick(["192.168", "10.10", "172.16"])),
            destinationIP: randIP("10.0"),
            eventType: t.type,
            severity: pick(severities),
            description: t.desc,
            source: t.source,
            affectedSystems: [t.source],
            tags: t.tags,
            responseActions: [{ action: "Notify SOC Analyst", status: pick(statuses) }]
        });
    }

    var res = col.insertMany(docs);
    print("Inserted " + Object.keys(res.insertedIds).length + " security event document(s) into "
          + dbName + "." + colName + ".");
}

// ---------------------------------------------------------------
// Question 1.3 - Find events by source IP
// ---------------------------------------------------------------
function findEventsByIP(dbName, colName, sourceIP) {
    var col = db.getSiblingDB(dbName).getCollection(colName);
    var results = col.find({ sourceIP: sourceIP }).toArray();
    if (results.length === 0) {
        print("No security events found for source IP " + sourceIP + ".");
        return;
    }
    print(results.length + " security event(s) found for source IP " + sourceIP + ":");
    results.forEach(function (d) { printjson(d); });
}

// ---------------------------------------------------------------
// Question 1.4 - Update severity for an event type
// ---------------------------------------------------------------
function updateSeverity(dbName, colName, eventType, newSeverity) {
    var col = db.getSiblingDB(dbName).getCollection(colName);
    var res = col.updateMany({ eventType: eventType }, { $set: { severity: newSeverity } });
    print("Documents matched : " + res.matchedCount);
    print("Documents modified: " + res.modifiedCount);
}

// ---------------------------------------------------------------
// Question 1.5 - Remove low severity events
// ---------------------------------------------------------------
function removeLowSeverityEvents(dbName, colName) {
    var col = db.getSiblingDB(dbName).getCollection(colName);
    var res = col.deleteMany({ severity: "Low" });
    print("Documents removed: " + res.deletedCount);
}
