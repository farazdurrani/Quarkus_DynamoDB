package org.acme.dynamodb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.amazonaws.services.dynamodbv2.document.Item;

@ApplicationScoped
public class SessionService {

    private SessionRepo repo;

    @ConfigProperty(name = "share.allow")
    Optional<List<String>> allowed;
    @ConfigProperty(name = "share.notAllow")
    Optional<List<String>> disAllowed;

    public List<ResponseData> get(Request req) {
	String serviceName = req.getServiceName();
	List<Item> items = new ArrayList<>();
	if (disAllowed.orElse(Collections.emptyList()).contains(serviceName)) {
	    items = repo.getSpecificData(serviceName);
	} else {
	    items = repo.getSpecificData(allowed.orElse(Collections.emptyList()).toArray());
	}
	return convertToShareData(items, req.getKey());
    }

    private List<ResponseData> convertToShareData(List<Item> items, String key) {
	return items.stream().map(convertItemToShareData).map(sd -> {
	    if (sd.getSharedData().containsKey(key)) {
		return sd;
	    }
	    return null;
	}).filter(Objects::nonNull).map(sd -> new ResponseData(key, sd.getSharedData().get(key))).collect(Collectors.toList());
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
    
    private Function<Item, ShareData> convertItemToShareData = i -> new ShareData(i.getString(SessionRepo.primaryKey),
	    i.getMap(SessionRepo.queryColumn));

}
