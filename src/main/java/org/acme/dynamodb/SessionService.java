package org.acme.dynamodb;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.amazonaws.services.dynamodbv2.document.Item;

@ApplicationScoped
public class SessionService {

    private SessionRepo repo;

    public List<ShareData> findAll() {
	return getAll();
    }

    private List<ShareData> getAll() {
	return repo.getAllItems().stream().map(itemToShareDataConverter).collect(Collectors.toList());
    }

    private Function<Item, ShareData> itemToShareDataConverter = i -> new ShareData(i.getString(SessionRepo.primaryKey),
	    i.getMap(SessionRepo.queryColumn));

    public ShareData get(String name) {
	return null;
    }

    public void add(ShareData data) {
	data.getSharedData().forEach((key, value) -> {
	    repo.upsert(data.getServiceName(), key, value);
	});
	
    }

    @Inject
    public void setSessionRepo(SessionRepo repo) {
	this.repo = repo;
    }
}
