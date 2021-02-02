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

    static final String DYNAMO_TABLE_NAME = "sharedData";
    static final String DYNAMO_PRIMARY_KEY = "serviceName";
    static final String DYNAMO_QUERY_COLUMN = "sharedData";

    void startup(@Observes StartupEvent event) {
	dynamoDB = new DynamoDB(AmazonDynamoDBClientBuilder.standard()
		.withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(dataSourceUrl, "local")).build());
	if (!isTableExist(DYNAMO_TABLE_NAME)) {
	    createTable(DYNAMO_TABLE_NAME);
	    loadInitialData(DYNAMO_TABLE_NAME);
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
		    Arrays.asList(new KeySchemaElement(DYNAMO_PRIMARY_KEY, KeyType.HASH)),
		    Arrays.asList(new AttributeDefinition(DYNAMO_PRIMARY_KEY, ScalarAttributeType.S)),
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
	Item item = new Item().withPrimaryKey(DYNAMO_PRIMARY_KEY, serviceName).withMap(DYNAMO_QUERY_COLUMN, sharedData);
	table.putItem(item);
    }

    public List<Item> getSpecificData(Object... primaryKeyValues) {
	List<Item> allItems = new ArrayList<>();
	Map<String, KeysAndAttributes> unprocessed = null;
	do {
	    TableKeysAndAttributes keysAndAttributes = new TableKeysAndAttributes(DYNAMO_TABLE_NAME);
	    keysAndAttributes.addHashOnlyPrimaryKeys(DYNAMO_PRIMARY_KEY, primaryKeyValues);
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
	upsert(DYNAMO_TABLE_NAME, DYNAMO_PRIMARY_KEY, primaryKeyValue, DYNAMO_QUERY_COLUMN, queryColumnKey,
		queryColumnValue);
    }

    private void upsert(String tableName, String primaryKey, String primaryKeyValue, String updateColumn, String newKey,
	    String newValue) {
	Table table = dynamoDB.getTable(tableName);
	try {
	    UpdateItemSpec updateItemSpec = new UpdateItemSpec().withPrimaryKey(primaryKey, primaryKeyValue)
		    .withReturnValues(ReturnValue.ALL_NEW)
		    .withUpdateExpression("set #columnName." + newKey + " = :columnValue")
		    .withNameMap(new NameMap().with("#columnName", updateColumn))
		    .withValueMap(new ValueMap().with(":columnValue", newValue))
		    .withConditionExpression("attribute_exists(" + updateColumn + ")");

	    table.updateItem(updateItemSpec);
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