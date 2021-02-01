package org.acme.dynamodb;

import java.util.Map;
import java.util.Objects;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class ShareData {

    private String serviceName;
    private Map<String, String> sharedData;

    public ShareData() {
    }

    public ShareData(String serviceName, Map<String, String> sharedData) {
	this.serviceName = serviceName;
	this.sharedData = sharedData;
    }

    public String getServiceName() {
	return serviceName;
    }

    public void setServiceName(String serviceName) {
	this.serviceName = serviceName;
    }

    public Map<String, String> getSharedData() {
	return sharedData;
    }

    public void setSharedData(Map<String, String> sharedData) {
	this.sharedData = sharedData;
    }

    @Override
    public int hashCode() {
	return Objects.hash(serviceName, sharedData);
    }

    @Override
    public boolean equals(Object obj) {
	if (this == obj)
	    return true;
	if (obj == null)
	    return false;
	if (getClass() != obj.getClass())
	    return false;
	ShareData other = (ShareData) obj;
	return Objects.equals(serviceName, other.serviceName) && Objects.equals(sharedData, other.sharedData);
    }

}