package org.acme.dynamodb;

public class Request {

    private String serviceName;
    private String key;

    public Request() {
    }

    public Request(String serviceName, String key) {
	this.serviceName = serviceName;
	this.key = key;
    }

    public String getServiceName() {
	return serviceName;
    }

    public void setServiceName(String serviceName) {
	this.serviceName = serviceName;
    }

    public String getKey() {
	return key;
    }

    public void setKey(String key) {
	this.key = key;
    }

}
