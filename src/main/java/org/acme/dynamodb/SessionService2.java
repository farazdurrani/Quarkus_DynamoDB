package org.acme.dynamodb;
//package org.acme.dynamodb;
//
//import java.util.List;
//import java.util.stream.Collectors;
//
//import javax.enterprise.context.ApplicationScoped;
//import javax.inject.Inject;
//
//import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
//
//@ApplicationScoped
//public class SessionService extends AbstractService {
//
//    @Inject
//    DynamoDbClient dynamoDB;
//
//    public List<ShareData> findAll() {
//        return dynamoDB.scanPaginator(scanRequest()).items().stream()
//                .map(ShareData::from)
//                .collect(Collectors.toList());
//    }
//
//    public List<ShareData> add(ShareData fruit) {
//        dynamoDB.putItem(putRequest(fruit));
//        return findAll();
//    }
//
//    public ShareData get(String name) {
//        return ShareData.from(dynamoDB.getItem(getRequest(name)).item());
//    }
//}