package com.salesforce.emp.connector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.salesforce.emp.connector.example.LoginExample;
import com.salesforce.emp.connector.object.SavedHTTPResponse;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.eclipse.jetty.util.ajax.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;

public class ApiActions {

    final static Logger log = LoggerFactory.getLogger(ApiActions.class);
    static final String BASE_URL = "https://api.onlinephotosubmission.com/api";

    public static String login(String username, String password) throws IOException {
        String json = "{\"username\": \"" + username + "\", \"password\": \"" + password + "\"}";
            SavedHTTPResponse response = post("/login", json, null);
            return getVariableFromJson(response.getContent(), "access_token");
    }

    private static SavedHTTPResponse post(String endpoint, String json, String authToken) throws IOException {
        CloseableHttpClient connection = HttpClients.createDefault();
        HttpPost request = new HttpPost(BASE_URL + endpoint);
        StringEntity body = null;
        body = new StringEntity(json);
        request.setEntity(body);
        request.setHeader("Accept", "application/json");
        request.setHeader("Content-Type", "application/json");
        if (authToken != null) request.setHeader("X-Auth-Token", authToken);
        CloseableHttpResponse response = connection.execute(request);
        SavedHTTPResponse savedResponse = new SavedHTTPResponse(EntityUtils.toString(response.getEntity()), response.getStatusLine().getStatusCode(), response.getStatusLine().getReasonPhrase());
        connection.close();
        return savedResponse;
    }

    private static String getVariableFromJson(String json, String variable) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode responseTree = objectMapper.readTree(json);
        return responseTree.findValue(variable).asText();
    }

    private static void loginToCloudcard(Map<String, String> arguments){
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost loginPost = new HttpPost("https://api.onlinephotosubmission.com/api/login");

        String loginJsonBody = "{\"username\": \"" + arguments.get("cloudcard_username") + "\", \"password\": \"" + arguments.get("cloudcard_password") + "\"}";
        StringEntity entity = null;
        try {
            entity = new StringEntity(loginJsonBody);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        loginPost.setEntity(entity);
        loginPost.setHeader("Accept", "application/json");
        loginPost.setHeader("Content-Type", "application/json");

        try {
            CloseableHttpResponse response = httpClient.execute(loginPost);
            String responseBody = EntityUtils.toString(response.getEntity());

            String accessToken = getVariableFromJson(responseBody, "access_token");

            if (responseBody.contains("EmptyInputStream")) responseBody = "";
            int responseCode = response.getStatusLine().getStatusCode();
            String responsePhrase = response.getStatusLine().getReasonPhrase();

            log.info("login request returned " + responseCode + " - " + responsePhrase + ", accessToken = " + accessToken);
            httpClient.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
