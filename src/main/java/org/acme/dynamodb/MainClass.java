package org.acme.dynamodb;

import java.util.List;

public class MainClass {
    public static void main(String[] args) {
	SessionRepo sr = new SessionRepo();
	sr.startup(null);
	SessionService ss = new SessionService();
	ss.setSessionRepo(sr);
	List<ShareData> l = ss.findAll();
	System.out.println(l);
    }
}
