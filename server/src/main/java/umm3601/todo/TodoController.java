package umm3601.todo;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
// import static com.mongodb.client.model.Filters.regex;

// import java.nio.charset.StandardCharsets;
// import java.security.MessageDigest;
// import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
// import java.util.Map;
// import java.util.Objects;
// import java.util.regex.Pattern;

import org.bson.Document;
import org.bson.UuidRepresentation;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.mongojack.JacksonMongoCollection;

import com.mongodb.client.MongoDatabase;
// import com.mongodb.client.model.Sorts;
// import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.model.Filters;

import io.javalin.Javalin;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.http.NotFoundResponse;
import umm3601.Controller;

/**
 * Controller that manages requests for info about todos.
 */
public class TodoController implements Controller {

  private static final String API_TODOS = "/api/todos"; // Issue #1
  private static final String API_TODO_BY_ID = "/api/todos/{id}"; //Issue #2
  //Issue #3 doesn't need an API string because it's a query

  private static final String STATUS_REGEX = "^(complete|incomplete)$";
  private static final String BODY_REGEX = ".*";

 // static final String OWNER_KEY = "owner";
 // static final String CATEGORY_KEY = "category";
 // ^ needed for other issues -HH

  private final JacksonMongoCollection<Todo> todoCollection;

  /**
   * Construct a controller for todos.
   *
   *
   * @param database the database containing user data
   */
  public TodoController(MongoDatabase database) {
    todoCollection = JacksonMongoCollection.builder().build(
        database,
        "todos",
        Todo.class,
        UuidRepresentation.STANDARD);
  }

  /**
   * Set the JSON body of the response to be the single todo
   * specified by the `id` parameter in the request
   *
   * @param ctx a Javalin HTTP context
   */
  public void getTodo(Context ctx) {
    String id = ctx.pathParam("id");
    Todo todo;

    try {
      todo = todoCollection.find(eq("_id", new ObjectId(id))).first();
    } catch (IllegalArgumentException e) {
      throw new BadRequestResponse("The requested todo id wasn't a legal Mongo Object ID.");
    }
    if (todo == null) {
      throw new NotFoundResponse("The requested todo was not found");
    } else {
      ctx.json(todo);
      ctx.status(HttpStatus.OK);
    }
  }

  /**
   * Set the JSON body of the response to be a list of all the todos returned from the database
   * that match any requested filters and ordering
   *
   * @param ctx a Javalin HTTP context
   */
  public void getTodos(Context ctx) {
    Bson combinedFilter = constructFilter(ctx);
    // Bson sortingOrder = constructSortingOrder(ctx);

    int limit = ctx.queryParamAsClass("limit", Integer.class)
      .getOrDefault(0);
    //Creates a query parameter that can be used named "limit" which is an integer.
    //The default todo limit is '0' which means that there is no limit and all todos are displayed.

    ArrayList<Todo> matchingTodos = todoCollection
      .find(combinedFilter)
      // .sort()
      .limit(limit)
      .into(new ArrayList<>());

    // Set the JSON body of the response to be the list of todos returned by the database.
    // According to the Javalin documentation (https://javalin.io/documentation#context),
    // this calls result(jsonString), and also sets content type to json
    ctx.json(matchingTodos);

    // Explicitly set the context status to OK
    ctx.status(HttpStatus.OK);
 }


  @Override
  public void addRoutes(Javalin server) {
    // List users, filtered using query parameters
    server.get(API_TODOS, this::getTodos);

    server.get(API_TODO_BY_ID, this::getTodo);

    //no specific endpoint needed for queries

  }

  private Bson constructFilter(Context ctx) {
    List<Bson> filters = new ArrayList<>(); // start with an empty list of filters


    if (ctx.queryParamMap().containsKey("status")) {
      String status = ctx.queryParamAsClass("status", String.class)
        .check(it -> it.matches(STATUS_REGEX), "User must have a legal status")
        .get();

        Boolean boolStatus = switch (status) {
          case "complete" -> true;            //if status is "complete," then the boolean value returned is true
          case "incomplete" -> false;         //if the status is "incomplete," then the value returned is false
          default -> throw new IllegalArgumentException("Unexpected status: " + status);
        };  //in the case that the status is neither "compete" nor "incomplete,"
            // then and exception is thrown that stops execution
            // -Evie (I forgot to reference this early on, but I did have help from AI(Copilot) coming up
            //  with this portion of code)

     filters.add(eq("status", boolStatus));
    }

    if (ctx.queryParamMap().containsKey("contains")) {  //  ?contains= ___
      String wordCase = ctx.queryParamAsClass("contains", String.class)
        .check(it -> it.matches(BODY_REGEX), "Todo body must have contents")
        .get();

      filters.add(Filters.regex("body", wordCase)); //Filter needs to include the search, but not be only the search
    }
    Bson combinedFilter = filters.isEmpty() ? new Document() : and(filters);

    return combinedFilter;

  }

}
