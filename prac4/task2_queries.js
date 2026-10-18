// COS326 Practical 4 2026 - Task 2 queries
// Run with:  mongosh cybershield task2_queries.js   (after re-importing security_events.json)
// Import:    mongoimport --db cybershield --collection securityEvents --drop --jsonArray --file security_events.json

use("cybershield");

// ---- Question 2.1 - High and Critical events, newest first ----
print("--- 2.1 ---");
db.securityEvents.find(
    { severity: { $in: ["High", "Critical"] } },
    { _id: 0, eventId: 1, timestamp: 1, sourceIP: 1, eventType: 1, severity: 1 }
).sort({ timestamp: -1 }).forEach(printjson);

// ---- Question 2.2 - events with the "brute-force" tag ----
print("--- 2.2 ---");
db.securityEvents.find(
    { tags: "brute-force" },
    { _id: 0, eventId: 1, eventType: 1, severity: 1, tags: 1 }
).forEach(printjson);

// ---- Question 2.3 - at least one response action Pending ----
print("--- 2.3 ---");
db.securityEvents.find(
    { responseActions: { $elemMatch: { status: "Pending" } } },
    { _id: 0, eventId: 1, eventType: 1, severity: 1, responseActions: 1 }
).forEach(printjson);

// ---- Question 2.4 - count of events per severity, highest count first ----
print("--- 2.4 ---");
db.securityEvents.aggregate([
    { $group: { _id: "$severity", numberOfEvents: { $sum: 1 } } },
    { $project: { _id: 0, severity: "$_id", numberOfEvents: 1 } },
    { $sort: { numberOfEvents: -1 } }
]).forEach(printjson);

// ---- Question 2.5 - text search for "authentication" ----
print("--- 2.5 ---");
db.securityEvents.createIndex({ description: "text" });
db.securityEvents.find(
    { $text: { $search: "authentication" } },
    { _id: 0, eventId: 1, eventType: 1, severity: 1, description: 1 }
).forEach(printjson);
