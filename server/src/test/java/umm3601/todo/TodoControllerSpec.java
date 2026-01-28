package umm3601.todo;

// import static com.mongodb.client.model.Filters.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
// import static org.junit.jupiter.api.Assertions.assertNotEquals;
// import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

// import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
// import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
// import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
// import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
// import java.util.stream.Collectors;
// import java.util.stream.Collectors;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
// import org.mockito.ArgumentMatcher;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

// import com.fasterxml.jackson.core.JsonProcessingException;
// import com.fasterxml.jackson.databind.JsonMappingException;
import com.mongodb.MongoClientSettings;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import io.javalin.Javalin;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.http.NotFoundResponse;
import io.javalin.json.JavalinJackson;
// import io.javalin.validation.BodyValidator;
// import io.javalin.validation.Validation;
// import io.javalin.validation.ValidationError;
// import io.javalin.validation.ValidationException;
import io.javalin.validation.Validator;
// import io.javalin.validation.Validation;
// import umm3601.user.UserController;


public class TodoControllerSpec {

  private TodoController todoController;
  private ObjectId taskId;
  private static MongoClient mongoClient;
  private static MongoDatabase db;
  private static JavalinJackson javalinJackson = new JavalinJackson();

  @Mock
  private Context ctx;

  @Captor
  private ArgumentCaptor<ArrayList<Todo>> todoArrayListCaptor;

  @Captor
  private ArgumentCaptor<Todo> todoCaptor;

  @Captor
  private ArgumentCaptor<Map<String, String>> mapCaptor;

  @BeforeAll
  static void setupAll() {
    String mongoAddr = System.getenv().getOrDefault("MONGO_ADDR", "localhost");

    mongoClient = MongoClients.create(
      MongoClientSettings.builder()
            .applyToClusterSettings(builder -> builder.hosts(Arrays.asList(new ServerAddress(mongoAddr))))
            .build());
    db = mongoClient.getDatabase("test");
  }

  @AfterAll
  static void teardown() {
    db.drop();
    mongoClient.close();
  }

  @BeforeEach
  void setUpEach() throws IOException {
    MockitoAnnotations.openMocks(this);

    MongoCollection<Document> todoDocuments = db.getCollection("todos");
    todoDocuments.drop();
    List<Document> testTodos = new ArrayList<>();
    testTodos.add(
      new Document()
        .append("owner", "Mike")
        .append("body", "Wash shirts and pants")
        .append("status", false)
        .append("category", "chores"));

    testTodos.add(
      new Document()
        .append("owner", "Marty")
        .append("body", "Buy applesauce from Willie's")
        .append("status", false)
        .append("category", "groceries"));

    testTodos.add(
      new Document()
        .append("owner", "Katie")
        .append("body", "Calculus Assignment #3")
        .append("status", true)
        .append("category", "homework"));

    testTodos.add(
      new Document()
        .append("owner", "Katy")
        .append("body", "CSCI Lab #2")
        .append("status", false)
        .append("category", "homework"));

    taskId = new ObjectId();
    Document task = new Document()
      .append("_id", taskId)
      .append("owner", "Pam")
      .append("body", "This is a default task description")
      .append("status", false)
      .append("category", "chores");

    todoDocuments.insertMany(testTodos);
    todoDocuments.insertOne(task);

    todoController = new TodoController(db);
  }

 @Test
  void getTodoWithExistentId() throws IOException {
    String id = taskId.toHexString();
    when(ctx.pathParam("id")).thenReturn(id);

    todoController.getTodo(ctx);

    verify(ctx).json(todoCaptor.capture());
    verify(ctx).status(HttpStatus.OK);
    assertEquals("Pam", todoCaptor.getValue().owner);
    assertEquals(taskId.toHexString(), todoCaptor.getValue()._id);
  }

  @Test
  void getTodoWithBadId() throws IOException {
    when(ctx.pathParam("id")).thenReturn("bad");

    Throwable exception = assertThrows(BadRequestResponse.class, () -> {
      todoController.getTodo(ctx);
    });

    assertEquals("The requested todo id wasn't a legal Mongo Object ID.", exception.getMessage());
  }

  @Test
  void getTodoWithNonexistentId() throws IOException {
    String id = "588935f5c668650dc77df581";
    when(ctx.pathParam("id")).thenReturn(id);

    Throwable exception = assertThrows(NotFoundResponse.class, () -> {
      todoController.getTodo(ctx);
    });

    assertEquals("The requested todo was not found", exception.getMessage());
  }

@Test
void addRoutes() {
  Javalin mockServer = mock(Javalin.class);
  todoController.addRoutes(mockServer);
  verify(mockServer, Mockito.atLeast(1)).get(any(), any());
  //will need to update this when more routes are added -HH
}

@Test
void canGetAllTodos() throws IOException {
  Validator<Integer> validator = mock(Validator.class);
  when(ctx.queryParamAsClass("limit", Integer.class)).thenReturn(validator);

  when(validator.getOrDefault(0)).thenReturn(0); //avoids null pointer for limit query that may or may not exist

  todoController.getTodos(ctx);
  //filling queryParamMap

  verify(ctx).json(todoArrayListCaptor.capture());
  //capturing the Array list to refer to later when we check it with database
  verify(ctx).status(HttpStatus.OK);

  assertEquals(
    db.getCollection("todos").countDocuments(),
    todoArrayListCaptor.getValue().size());
  // checking if the database has the same number of todos as the captured list
}

/**
   * Confirm that if we process a request for users with age 37,
   * that all returned users have that age, and we get the correct
   * number of users.
   *
   * The structure of this test is:
   *
   *    - We create a `Map` for the request's `queryParams`, that
   *      contains a single entry, mapping the `AGE_KEY` to the
   *      target value ("37"). This "tells" our `UserController`
   *      that we want all the `User`s that have age 37.
   *    - We create a validator that confirms that the code
   *      we're testing calls `ctx.queryParamsAsClass("age", Integer.class)`,
   *      i.e., it asks for the value in the query param map
   *      associated with the key `"age"`, interpreted as an Integer.
   *      That call needs to return a value of type `Validator<Integer>`
   *      that will succeed and return the (integer) value `37` associated
   *      with the (`String`) parameter value `"37"`.
   *    - We then call `userController.getUsers(ctx)` to run the code
   *      being tested with the constructed context `ctx`.
   *    - We also use the `userListArrayCaptor` (defined above)
   *      to capture the `ArrayList<User>` that the code under test
   *      passes to `ctx.json(…)`. We can then confirm that the
   *      correct list of users (i.e., all the users with age 37)
   *      is passed in to be returned in the context.
   *    - Now we can use a variety of assertions to confirm that
   *      the code under test did the "right" thing:
   *       - Confirm that the list of users has length 2
   *       - Confirm that each user in the list has age 37
   *       - Confirm that their names are "Jamie" and "Pat"
   *
   * @throws IOException
   */
  @Test
  void canGetCompleteTodoStatus() throws IOException {

  String completeStatus = "complete";
  String completeStatusString = completeStatus.toString(); //this variable was unneeded lol

    Map<String, List<String>> queryParams = new HashMap<>();

    queryParams.put("status", Arrays.asList(new String[] {completeStatusString}));
  // When the code being tested calls `ctx.queryParamMap()` return the
  // the `queryParams` map we just built.
    when(ctx.queryParamMap()).thenReturn(queryParams);
  // When the code being tested calls `ctx.queryParam("status")` return the
  // `completeStatusString`.
    when(ctx.queryParam("status")).thenReturn(completeStatusString);

  // Create a validator that confirms that when we ask for the value associated with
  // You can actually put whatever you want here, because it's only used in the generation
  // of testing error reports, but using the actually key value will make those reports more informative.
  Validator<String> statusValidator = mock(Validator.class);
  when(ctx.queryParamAsClass("status", String.class)).thenReturn(statusValidator);
  when(statusValidator.check(any(), anyString())).thenReturn(statusValidator);
  when(statusValidator.get()).thenReturn(completeStatus);

  Validator<Integer> limitValidator = mock(Validator.class);
  when(ctx.queryParamAsClass("limit", Integer.class)).thenReturn(limitValidator);
  when(limitValidator.getOrDefault(0)).thenReturn(0);
    // When the code being tested calls `ctx.queryParamAsClass("status", Integer.class)`
    // we'll return the `Validator` we just constructed.

    todoController.getTodos(ctx);

    // Confirm that the code being tested calls `ctx.json(…)`, and capture whatever
    // is passed in as the argument when `ctx.json()` is called.
    verify(ctx).json(todoArrayListCaptor.capture());
    // Confirm that the code under test calls `ctx.status(HttpStatus.OK)` is called.
    verify(ctx).status(HttpStatus.OK);

    // Confirm that we get back one todo.

    assertEquals(1, todoArrayListCaptor.getValue().size());


    // Confirm that todos have desired status: complete
    for (Todo todo : todoArrayListCaptor.getValue()) {
      assertTrue(todo.status);

    }
  }

  @Test
  void canGetInCompleteTodoStatus() throws IOException {

  String incompleteStatus = "incomplete";

    Map<String, List<String>> queryParams = new HashMap<>();

    queryParams.put("status", Arrays.asList(new String[] {incompleteStatus}));

    when(ctx.queryParamMap()).thenReturn(queryParams);
    when(ctx.queryParam("status")).thenReturn(incompleteStatus);

  Validator<String> statusValidator = mock(Validator.class);
  when(ctx.queryParamAsClass("status", String.class)).thenReturn(statusValidator);
  when(statusValidator.check(any(), anyString())).thenReturn(statusValidator);
  when(statusValidator.get()).thenReturn(incompleteStatus);

  Validator<Integer> limitValidator = mock(Validator.class);
  when(ctx.queryParamAsClass("limit", Integer.class)).thenReturn(limitValidator);
  when(limitValidator.getOrDefault(0)).thenReturn(0);

    todoController.getTodos(ctx);

    verify(ctx).json(todoArrayListCaptor.capture());
    verify(ctx).status(HttpStatus.OK);

    // Confirm that todos have desired status: incomplete
    for (Todo todo : todoArrayListCaptor.getValue()) {
      assertFalse(todo.status);
    }
  }

  @Test
  void canGetInvalidTodoStatus() throws IOException {

  String invalidStatus = "complet";

    Map<String, List<String>> queryParams = new HashMap<>();

    queryParams.put("status", Arrays.asList(new String[] {invalidStatus}));
    when(ctx.queryParamMap()).thenReturn(queryParams);
    when(ctx.queryParam("status")).thenReturn(invalidStatus);

  Validator<String> statusValidator = mock(Validator.class);
  when(ctx.queryParamAsClass("status", String.class)).thenReturn(statusValidator);
  when(statusValidator.check(any(), anyString())).thenReturn(statusValidator);
  when(statusValidator.get()).thenReturn(invalidStatus);

  Validator<Integer> limitValidator = mock(Validator.class);
  when(ctx.queryParamAsClass("limit", Integer.class)).thenReturn(limitValidator);
  when(limitValidator.getOrDefault(0)).thenReturn(0);
    Exception thrown = assertThrows(IllegalArgumentException.class, () -> todoController.getTodos(ctx));

    assertEquals("Unexpected status: complet", thrown.getMessage());

    //We realize we missed this bit of coverage from the last lab and wanted to
    //test how the server would act if given an invalid input for its contents

    }

  @Test
  void testContains() {

  String searchString = " "; //used " " because all generated todos will have " " in the body

    Map<String, List<String>> queryParams = new HashMap<>();

    queryParams.put("contains", Arrays.asList(new String[] {searchString}));
  // When the code being tested calls `ctx.queryParamMap()` return the
  // the `queryParams` map we just built.
    when(ctx.queryParamMap()).thenReturn(queryParams);
  // When the code being tested calls `ctx.queryParam("status")` return the
  // `completeStatusString`.
    when(ctx.queryParam("contains")).thenReturn(searchString);


  // Create a validator that confirms that when we ask for the value associated with
  // You can actually put whatever you want here, because it's only used in the generation
  // of testing error reports, but using the actually key value will make those reports more informative.
  Validator<String> containsValidator = mock(Validator.class);
  when(ctx.queryParamAsClass("contains", String.class)).thenReturn(containsValidator);
  when(containsValidator.check(any(), anyString())).thenReturn(containsValidator);
  when(containsValidator.get()).thenReturn(searchString);

  Validator<Integer> limitValidator = mock(Validator.class);
  when(ctx.queryParamAsClass("limit", Integer.class)).thenReturn(limitValidator);
  when(limitValidator.getOrDefault(0)).thenReturn(0);

    todoController.getTodos(ctx);

    verify(ctx).json(todoArrayListCaptor.capture());
    verify(ctx).status(HttpStatus.OK);

    List<Todo> returned = todoArrayListCaptor.getValue();

    assertFalse(returned.isEmpty());
    }
  }


