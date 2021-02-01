package org.acme.dynamodb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
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
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.KeysAndAttributes;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ReturnValue;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;

import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class SessionRepo {

    @ConfigProperty(name = "datasource.url")
    String dataSourceUrl;

    private DynamoDB dynamoDB;

    static final String tableName = "sharedData";
    static final String primaryKey = "serviceName";
    static final String queryColumn = "sharedData";

    void startup(@Observes StartupEvent event) {
	dynamoDB = new DynamoDB(AmazonDynamoDBClientBuilder.standard()
		.withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(dataSourceUrl, "local")).build());
	if (!isTableExist(tableName)) {
	    createTable(tableName);
	    loadInitialData(tableName);
	}
    }

    public boolean isTableExist(String tableName) {
	try {
	    dynamoDB.getTable(tableName).describe();
	    return true;
	} catch (com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException rnfe) {
	    return false;
	}
    }

    private void createTable(String tableName) {
	try {
	    Table table = dynamoDB.createTable(tableName,
		    Arrays.asList(new KeySchemaElement(primaryKey, KeyType.HASH)),
		    Arrays.asList(new AttributeDefinition(primaryKey, ScalarAttributeType.S)),
		    new ProvisionedThroughput(10L, 10L));
	    table.waitForActive();
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    private void loadInitialData(String tableName) {
	Table table = dynamoDB.getTable(tableName);
	String serviceName = "A";
	Map<String, String> sharedData = new HashMap<String, String>();
	sharedData.put("one", "1");
	sharedData.put("two", "2");
	sharedData.put("three", "3");
	Item item = new Item().withPrimaryKey(primaryKey, serviceName).withMap(queryColumn, sharedData);
	table.putItem(item);

    }

    public List<Item> getSpecificData(Object... primaryKeyValues) {
	List<Item> allItems = new ArrayList<>();
	Map<String, KeysAndAttributes> unprocessed = null;
	do {
	    TableKeysAndAttributes keysAndAttributes = new TableKeysAndAttributes(tableName);
	    keysAndAttributes.addHashOnlyPrimaryKeys(primaryKey, primaryKeyValues);
	    BatchGetItemOutcome outcome = dynamoDB.batchGetItem(keysAndAttributes);

	    for (String tn : outcome.getTableItems().keySet()) {
		List<Item> items = outcome.getTableItems().get(tn);
		allItems.addAll(items);
	    }

	    unprocessed = outcome.getUnprocessedKeys();

	    if (!unprocessed.isEmpty()) {
		outcome = dynamoDB.batchGetItemUnprocessed(unprocessed);
	    }
	} while (!unprocessed.isEmpty());
	return allItems;
    }

    public void upsert(String primaryKeyValue, String queryColumnKey, String queryColumnValue) {
	upsert(tableName, primaryKey, primaryKeyValue, queryColumn, queryColumnKey, queryColumnValue);
    }

    private void upsert(String tableName, String primaryKey, String primaryKeyValue, String updateColumn, String newKey,
	    String newValue) {

	// Configuration to connect to DynamoDB
	Table table = dynamoDB.getTable(tableName);
	try {
	    // Updates when map is already exist in the table
	    UpdateItemSpec updateItemSpec = new UpdateItemSpec().withPrimaryKey(primaryKey, primaryKeyValue)
		    .withReturnValues(ReturnValue.ALL_NEW)
		    .withUpdateExpression("set #columnName." + newKey + " = :columnValue")
		    .withNameMap(new NameMap().with("#columnName", updateColumn))
		    .withValueMap(new ValueMap().with(":columnValue", newValue))
		    .withConditionExpression("attribute_exists(" + updateColumn + ")");

	    table.updateItem(updateItemSpec);
	    // Add map column when it's not exist in the table
	} catch (ConditionalCheckFailedException e) {
	    HashMap<String, String> map = new HashMap<>();
	    map.put(newKey, newValue);
	    UpdateItemSpec updateItemSpec = new UpdateItemSpec().withPrimaryKey(primaryKey, primaryKeyValue)
		    .withReturnValues(ReturnValue.ALL_NEW).withUpdateExpression("set #columnName = :m")
		    .withNameMap(new NameMap().with("#columnName", updateColumn))
		    .withValueMap(new ValueMap().withMap(":m", map));

	    table.updateItem(updateItemSpec);
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
}
