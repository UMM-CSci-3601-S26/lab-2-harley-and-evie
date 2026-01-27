package umm3601.todo;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
// import static com.mongodb.client.model.Filters.regex;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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

  private static final String STATUS_REGEX = "^(complete|incomplete)$";
 // static final String OWNER_KEY = "owner";
  // static final String STATUS_KEY = "status";
 // static final String BODY_KEY = "body";
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

  /**
   * Utility function to generate the md5 hash for a given string
   *
   * @param str the string to generate a md5 for
   */
  public String md5(String str) throws NoSuchAlgorithmException {
    MessageDigest md = MessageDigest.getInstance("MD5");
    byte[] hashInBytes = md.digest(str.toLowerCase().getBytes(StandardCharsets.UTF_8));

    StringBuilder result = new StringBuilder();
    for (byte b : hashInBytes) {
      result.append(String.format("%02x", b));
    }
    return result.toString();
  }

  /**
   * Sets up routes for the `todo` collection endpoints.
   * A TodoController instance handles the todo endpoints,
   * and the addRoutes method adds the routes to this controller.
   *
   * These endpoints are:
   *   - `GET /api/todos/`
   *       - Get a list of all todos
   *
   * GROUPS SHOULD CREATE THEIR OWN CONTROLLERS THAT IMPLEMENT THE
   * `Controller` INTERFACE FOR WHATEVER DATA THEY'RE WORKING WITH.
   * You'll then implement the `addRoutes` method for that controller,
   * which will set up the routes for that data. The `Server#setupRoutes`
   * method will then call `addRoutes` for each controller, which will
   * add the routes for that controller's data.
   *
   * @param server The Javalin server instance
   */
  @Override
  public void addRoutes(Javalin server) {
    // List users, filtered using query parameters
    server.get(API_TODOS, this::getTodos);

    server.get(API_TODO_BY_ID, this::getTodo);

    //no specific endpoint needed for queries

  }

  // // @param
  // // @return
  // private Bson constructSortingOrder(Context ctx) {
  //   // Sort the results. Use the `sortby` query param (default "name")
  //   // as the field to sort by, and the query param `sortorder` (default
  //   // "asc") to specify the sort order.
  //   String sortBy = Objects.requireNonNullElse(ctx.queryParam("sortby"), "owner");
  //   String sortOrder = Objects.requireNonNullElse(ctx.queryParam("sortorder"), "asc");
  //   Bson sortingOrder = sortOrder.equals("desc") ?  Sorts.descending(sortBy) : Sorts.ascending(sortBy);
  //   return sortingOrder;
  // }



  // // @param ctx
  // // @return

  private Bson constructFilter(Context ctx) {
    List<Bson> filters = new ArrayList<>(); // start with an empty list of filters


    if (ctx.queryParamMap().containsKey("status")) {
      String status = ctx.queryParamAsClass("status", String.class)
        .check(it -> it.matches(STATUS_REGEX), "User must have a legal status")
        .get();

        Boolean boolStatus = Boolean.valueOf(status);

      filters.add(eq("status", boolStatus));
    }

    Bson combinedFilter = filters.isEmpty() ? new Document() : and(filters);

    return combinedFilter;

    //else



    // if (ctx.queryParamMap().containsKey(COMPANY_KEY)) {
    //   Pattern pattern = Pattern.compile(Pattern.quote(ctx.queryParam(COMPANY_KEY)), Pattern.CASE_INSENSITIVE);
    //   filters.add(regex(COMPANY_KEY, pattern));
    // }
    // if (ctx.queryParamMap().containsKey(ROLE_KEY)) {
    //   String role = ctx.queryParamAsClass(ROLE_KEY, String.class)
    //     .check(it -> it.matches(ROLE_REGEX), "User must have a legal user role")
    //     .get();
    //   filters.add(eq(ROLE_KEY, role));
    // }

    // Combine the list of filters into a single filtering document.
    // Bson combinedFilter = filters.isEmpty() ? new Document() : and(filters);

    // return combinedFilter;
  }
}


