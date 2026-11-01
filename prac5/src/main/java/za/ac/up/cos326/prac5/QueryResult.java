package za.ac.up.cos326.prac5;

import java.util.List;

/**
 * Plain, UI-independent holder for the outcome of one investigation.
 * Everything in here was read from Neo4j - nothing is hard-coded.
 *
 * @param columns column headings for the result table
 * @param rows    the result rows (values are Long, String, Boolean, List ... or null)
 * @param message a one-line summary / explanation shown under the table
 */
public record QueryResult(List<String> columns, List<List<Object>> rows, String message) {

    public boolean isEmpty() {
        return rows.isEmpty();
    }
}
