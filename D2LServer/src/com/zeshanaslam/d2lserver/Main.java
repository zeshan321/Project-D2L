package com.zeshanaslam.d2lserver;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.zeshanaslam.d2lhook.ContentObject;
import com.zeshanaslam.d2lhook.CourseObject;
import com.zeshanaslam.d2lhook.D2LHook;
import com.zeshanaslam.d2lhook.LockerObject;
import com.zeshanaslam.d2lserver.ServerUtils.ErrorType;


public class Main {
	
	public static  HashMap<String, DataObject> apiData = new HashMap<>();

	public static void main(String[] args) throws Exception {
		HttpServer server = HttpServer.create(new InetSocketAddress(8001), 0);
		server.createContext("/data", new LoingHandler());
		server.setExecutor(null); // creates a default executor
		server.start();
	}

	// http://localhost:8000/data?user=test&pass=testing&type=
	// type: content, course, locker
	// Options: courseid, lockerpreview
	static class LoingHandler implements HttpHandler {
		@Override
		public void handle(HttpExchange httpExchange) throws IOException {
			ServerUtils serverUtils = new ServerUtils();
			Map<String, String> params = serverUtils.queryToMap(httpExchange.getRequestURI().getQuery()); 
			
			// Params check
			if (!params.containsKey("user") || !params.containsKey("pass") || !params.containsKey("type")) {
				serverUtils.writeResponse(httpExchange, serverUtils.getError(ErrorType.Invaild));
				return;
			}
			
			String username = params.get("user"), password = params.get("pass");
			
			if (apiData.containsKey(username) && apiData.get(username).password.equals(password)) {
				getData(httpExchange, username, params);
			} else {
				D2LHook d2lHook = new D2LHook(username, password);
				d2lHook.loadPage();
				
				if (d2lHook.loginStatus()) {
					apiData.put(username, new DataObject(password, d2lHook));
					getData(httpExchange, username, params);
				} else {
					serverUtils.writeResponse(httpExchange, serverUtils.getError(ErrorType.Login));
				}
			}
		}
		
		public void getData(HttpExchange httpExchange, String user, Map<String, String> params) {
			ServerUtils serverUtils = new ServerUtils();
			List<String> sterilizedObject = new ArrayList<>();
			D2LHook d2lHook = apiData.get(user).d2lHook;
			
			switch (params.get("type")) {
			case "course":
				// Sterilize objects
				for (CourseObject courseObject: d2lHook.getCourses()) {
					sterilizedObject.add(courseObject.toString());
				}
				
				serverUtils.writeResponse(httpExchange, serverUtils.returnData(sterilizedObject));
			case "content":
				// Params check
				if (!params.containsKey("courseid")) {
					serverUtils.writeResponse(httpExchange, serverUtils.getError(ErrorType.Invaild));
					return;
				}
				
				// Sterilize objects
				sterilizedObject = new ArrayList<>();
				for (ContentObject contentObject: d2lHook.getCourseContent(params.get("courseid"))) {
					sterilizedObject.add(contentObject.toString());
				}
				
				serverUtils.writeResponse(httpExchange, serverUtils.returnData(sterilizedObject));
			case "locker":
				// Params check
				if (!params.containsKey("lockerpreview")) {
					serverUtils.writeResponse(httpExchange, serverUtils.getError(ErrorType.Invaild));
					return;
				}
				
				// Sterilize objects
				sterilizedObject = new ArrayList<>();
				for (LockerObject lockerObject: d2lHook.getLocker(Boolean.valueOf(params.get("lockerpreview")))) {
					sterilizedObject.add(lockerObject.toString());
				}
				
				serverUtils.writeResponse(httpExchange, serverUtils.returnData(sterilizedObject));
			}
		}
	}
}