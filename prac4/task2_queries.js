// Run with:  mongosh cybershield task2_queries.js  after reimporting

use("cybershield");

// Query 2.1 - High and Critical events, newest first 
print(" Query 2.1 ");
db.securityEvents.find(
    { severity: { $in: ["High", "Critical"] } },
    { _id: 0, eventId: 1, timestamp: 1, sourceIP: 1, eventType: 1, severity: 1 }
).sort({ timestamp: -1 }).forEach(printjson);

// Query 2.2 - events with the "brute-force" tag 
print(" Query 2.2 ");
db.securityEvents.find(
    { tags: "brute-force" },
    { _id: 0, eventId: 1, eventType: 1, severity: 1, tags: 1 }
).forEach(printjson);

// Query 2.3 - at least one response action Pending 
print(" Query 2.3 ");
db.securityEvents.find(
    { responseActions: { $elemMatch: { status: "Pending" } } },
    { _id: 0, eventId: 1, eventType: 1, severity: 1, responseActions: 1 }
).forEach(printjson);

// Query 2.4 - count of events per severity, highest count first 
print(" Query 2.4 ");
db.securityEvents.aggregate([
    { $group: { _id: "$severity", numberOfEvents: { $sum: 1 } } },
    { $project: { _id: 0, severity: "$_id", numberOfEvents: 1 } },
    { $sort: { numberOfEvents: -1 } }
]).forEach(printjson);

// Query 2.5 - text search for "authentication" 
print(" Query 2.5 ");
db.securityEvents.createIndex({ description: "text" });
db.securityEvents.find(
    { $text: { $search: "authentication" } },
    { _id: 0, eventId: 1, eventType: 1, severity: 1, description: 1 }
).forEach(printjson);
