package com.gmail.lionelg3.nxtraces.rest;

import com.gmail.lionelg3.nxtraces.db.LogMessage;
import com.gmail.lionelg3.nxtraces.db.Repository;

import javax.json.*;
import javax.ws.rs.*;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;

/**
 * Collect mail traces from Unix systems
 *
 * @author Lionel G (lionelg3@gmail.com)
 *         Date: 18/12/15, Time: 16:43
 *         <p>
 *         cat src/test/resources/mail1.txt | curl --form mail=@- http://localhost:8080/api/sendmail
 *         <p>
 */
@Path("/query")
public class QueryService {

    private static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("HH:mm:ss dd/MM/yyyy");

    // ["equals", "from", "lionelg3@gmail.com"]
    // ["start with", "subject", "[SSH]"]
    // ["end with", "subject", "error"]
    // ["containts", "content", "update"]
    // ["on day", "2016,3,4"]
    // ["between days", "2016,3,4", "2016,3,5"]

    @GET
    public JsonObject load() {
        int y = LocalDate.now().getYear();
        int m = LocalDate.now().getMonthValue();
        int d = LocalDate.now().getDayOfMonth();
        d = 6;
        m = 3;
        y = 2016;
        JsonArray todayMessagesQuery = Json.createArrayBuilder()
                .add("on day")
                .add(y + "," + m + "," + d)
                .build();

        JsonArray queries = Json.createArrayBuilder()
                .add(todayMessagesQuery)
                .build();

        JsonObject jsonRequest = Json.createObjectBuilder()
                .add("max", 100)
                .add("queries", queries)
                .build();
        return search(jsonRequest);
    }

    @GET
    @Path("/load")
    public JsonObject get(@QueryParam("id") String id) {
        LogMessage logMessage = Repository.singleton().load(id);
        return fullMessageToJson(logMessage);
    }

    @POST
    public JsonObject search(JsonObject jsonRequest) {
        JsonObjectBuilder jsonResponseBuilder = Json.createObjectBuilder();
        JsonArrayBuilder jsonItemsBuilder = Json.createArrayBuilder();
        ArrayList<String[]> constraints = new ArrayList<>();

        int max = jsonRequest.getInt("max", 10);
        int first = jsonRequest.getInt("first", 0);

        JsonArray queries = jsonRequest.getJsonArray("queries");
        queries.forEach(qx -> {
            JsonArray jqx = (JsonArray) qx;
            String[] crit = new String[jqx.size()];
            for (int i = 0; i < jqx.size(); i++) {
                crit[i] = jqx.getString(i);
            }
            constraints.add(crit);
        });

        Repository.Result results = Repository.singleton().searchMessage(constraints, max, first);

        results.getList().forEach(o -> jsonItemsBuilder.add(shortMessageToJson((LogMessage) o)));
        jsonResponseBuilder.add("count", results.getCount());
        jsonResponseBuilder.add("results", jsonItemsBuilder);
        jsonResponseBuilder.add("request", jsonRequest);

        return jsonResponseBuilder.build();
    }

    private static JsonObject fullMessageToJson(LogMessage message) {
        return Json.createObjectBuilder()
                .add("content-type", message.getContentType() != null ? message.getContentType() : "")
                .add("content", message.getContent() != null ? message.getContent() : "")
                .add("from", message.getFrom() != null ? message.getFrom() : "")
                .add("messageid", message.getMessageID() != null ? message.getMessageID() : "")
                .add("recipients", message.getRecipients() != null ? message.getRecipients() : "")
                .add("size", message.getSize())
                .add("id", message.getFilename())
                .add("subject", message.getSubject() != null ? message.getSubject() : "")
                .add("date", message.getSentDate() != null ? DATE_FORMATTER.format(message.getSentDate()) : "")
                .build();
    }

    private static JsonObject shortMessageToJson(LogMessage message) {
        return Json.createObjectBuilder()
                .add("id", message.getFilename())
                .add("subject", message.getSubject() != null ? message.getSubject() : "")
                .add("date", message.getSentDate() != null ? DATE_FORMATTER.format(message.getSentDate()) : "")
                .build();
    }
}
