package org.acme.dynamodb;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.document.BatchGetItemOutcome;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.TableKeysAndAttributes;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.KeysAndAttributes;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ReturnValue;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.services.dynamodbv2.model.TableDescription;

public class RepoLayer {

    static DynamoDB dynamoDB = null;

    public static void main(String... args) {
	dynamoDB = new DynamoDB(AmazonDynamoDBClientBuilder.standard().withEndpointConfiguration(
		new AwsClientBuilder.EndpointConfiguration("http://127.0.0.1:8000/", "local")).build());

	String tableName = "sharedData";
	if (!isTableExist(tableName, dynamoDB)) {
	    createTable(tableName, dynamoDB);
	    loadInitialData(tableName, dynamoDB);
	}
	String primaryKey = "serviceName";
	String primaryKeyValue = "A";
	String queryColumn = "sharedData";
	String queryColumnKey = "five";
	String queryColumnValue = String.valueOf(5);
	upsert(tableName, primaryKey, primaryKeyValue, queryColumn, queryColumnKey, queryColumnValue);
	getData(tableName, dynamoDB, primaryKey, primaryKeyValue);
	primaryKeyValue = "B";
	getData(tableName, dynamoDB, primaryKey, primaryKeyValue);
	getSpecificData(tableName, primaryKey, primaryKeyValue, queryColumn, queryColumnKey);

    }

    private static void getSpecificData(String tableName, String primaryKey, String primaryKeyValue, String queryColumn,
	    String queryColumnKey) {
	Map<String, KeysAndAttributes> unprocessed = null;
	do {
	    TableKeysAndAttributes forumTableKeysAndAttributes = new TableKeysAndAttributes(tableName);
	    forumTableKeysAndAttributes.addHashOnlyPrimaryKeys(primaryKey, primaryKeyValue, primaryKey, "A");
	    BatchGetItemOutcome outcome = dynamoDB.batchGetItem(forumTableKeysAndAttributes);

	    for (String tn : outcome.getTableItems().keySet()) {
		System.out.println("Items in table " + tn);
		List<Item> items = outcome.getTableItems().get(tn);
		for (Item item : items) {
		    System.out.print(item + " ");
		}
		System.out.println();
	    }

	    unprocessed = outcome.getUnprocessedKeys();

	    if (unprocessed.isEmpty()) {
		System.out.println("No unprocessed keys found");
	    } else {
		System.out.println("Retrieving the unprocessed keys");
		outcome = dynamoDB.batchGetItemUnprocessed(unprocessed);
	    }
	} while (!unprocessed.isEmpty());
    }

    private static void getData(String tableName, DynamoDB dynamoDB, String primaryKey, String primaryKeyValue) {
	Table table = dynamoDB.getTable(tableName);
	Item item = table.getItem(primaryKey, primaryKeyValue);
	System.out.println(item);
    }

    private static void loadInitialData(String tableName, DynamoDB dynamoDB) {
	Table table = dynamoDB.getTable(tableName);
	String serviceName = "A";
	Map<String, String> sharedData = new HashMap<String, String>();
	sharedData.put("one", "1");
	sharedData.put("two", "2");
	sharedData.put("three", "3");
	Item item = new Item().withPrimaryKey("serviceName", serviceName).withMap("sharedData", sharedData);
	table.putItem(item);

    }

    public static boolean upsert(String tableName, String primaryKey, String primaryKeyValue, String updateColumn,
	    String newKey, String newValue) {

	// Configuration to connect to DynamoDB
	Table table = dynamoDB.getTable(tableName);
	boolean insertAppendStatus = false;
	try {
	    // Updates when map is already exist in the table
	    UpdateItemSpec updateItemSpec = new UpdateItemSpec().withPrimaryKey(primaryKey, primaryKeyValue)
		    .withReturnValues(ReturnValue.ALL_NEW)
		    .withUpdateExpression("set #columnName." + newKey + " = :columnValue")
		    .withNameMap(new NameMap().with("#columnName", updateColumn))
		    .withValueMap(new ValueMap().with(":columnValue", newValue))
		    .withConditionExpression("attribute_exists(" + updateColumn + ")");

	    table.updateItem(updateItemSpec);
	    insertAppendStatus = true;
	    // Add map column when it's not exist in the table
	} catch (ConditionalCheckFailedException e) {
	    HashMap<String, String> map = new HashMap<>();
	    map.put(newKey, newValue);
	    UpdateItemSpec updateItemSpec = new UpdateItemSpec().withPrimaryKey(primaryKey, primaryKeyValue)
		    .withReturnValues(ReturnValue.ALL_NEW).withUpdateExpression("set #columnName = :m")
		    .withNameMap(new NameMap().with("#columnName", updateColumn))
		    .withValueMap(new ValueMap().withMap(":m", map));

	    table.updateItem(updateItemSpec);
	    insertAppendStatus = true;
	} catch (Exception e) {
	    e.printStackTrace();
	}
	return insertAppendStatus;
    }

    public static boolean isTableExist(String tableName, DynamoDB dynamoDB) {
	try {
	    TableDescription tableDescription = dynamoDB.getTable(tableName).describe();
	    System.out.println("Table description: " + tableDescription.getTableStatus());
	    System.out.println("Number of records :: " + tableDescription.getItemCount());
	    return true;
	} catch (com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException rnfe) {
	    System.out.println("Table does not exist");
	}
	return false;

    }

    private static void createTable(String tableName, DynamoDB dynamoDB) {

	try {
	    System.out.println("Attempting to create table; please wait...");
	    Table table = dynamoDB.createTable(tableName,
		    Arrays.asList(new KeySchemaElement("serviceName", KeyType.HASH)),
		    Arrays.asList(new AttributeDefinition("serviceName", ScalarAttributeType.S)),
		    new ProvisionedThroughput(10L, 10L));
	    table.waitForActive();
	    System.out.println("Success.  Table status: " + table.getDescription().getTableStatus());

	} catch (Exception e) {
	    System.err.println("Unable to create table: ");
	    System.err.println(e.getMessage());
	}
    }

    private static void two() {
	DynamoDB dynamoDb = new DynamoDB(AmazonDynamoDBClientBuilder.standard()
		.withCredentials(new EnvironmentVariableCredentialsProvider()).withEndpointConfiguration(
			new AwsClientBuilder.EndpointConfiguration("http://127.0.0.1:8000/", "local"))
		.build());
	String tn = "QuarkusFruits";
	String key = "Mango";
	TableKeysAndAttributes forumTableKeysAndAttributes = new TableKeysAndAttributes(tn);
	forumTableKeysAndAttributes.addHashOnlyPrimaryKeys(key);

	BatchGetItemOutcome outcome = dynamoDb.batchGetItem(forumTableKeysAndAttributes);

	for (String tableName : outcome.getTableItems().keySet()) {
	    System.out.println("Items in table " + tableName);
	    List<Item> items = outcome.getTableItems().get(tableName);
	    for (Item item : items) {
		System.out.println(item);
	    }
	}
    }

    private static void one() {
	EndpointConfiguration ep = new EndpointConfiguration("http://localhost:8000/", "us-east-2");
	AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().withEndpointConfiguration(ep).build();
	DynamoDB dynamoDB = new DynamoDB(client);

	String tn = "QuarkusFruits";
	String key = "fruitName";
	TableKeysAndAttributes forumTableKeysAndAttributes = new TableKeysAndAttributes(tn);
	forumTableKeysAndAttributes.addHashOnlyPrimaryKeys(key);

	BatchGetItemOutcome outcome = dynamoDB.batchGetItem(forumTableKeysAndAttributes);

	for (String tableName : outcome.getTableItems().keySet()) {
	    System.out.println("Items in table " + tableName);
	    List<Item> items = outcome.getTableItems().get(tableName);
	    for (Item item : items) {
		System.out.println(item);
	    }
	}
    }
}
