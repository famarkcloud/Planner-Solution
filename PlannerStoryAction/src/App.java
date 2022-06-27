import java.io.DataOutputStream;
import java.io.Reader;
import java.net.URL;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.io.IOException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class App {

    private static String _sessionID, _serviceUrl;

    public static void main(String[] args) throws Exception {
        if (args.length > 0) {
            JSONParser parser = new JSONParser();
            JSONObject jsonObj = (JSONObject) parser.parse(args[0]);
            _sessionID = (String) jsonObj.get("SessionId");
            _serviceUrl = (String) jsonObj.get("ServiceUrl");
            String projectID = (String) jsonObj.get("ProjectId");
            if (projectID != null && projectID.length() > 0) {
                updateParent("Planner_Project", "ProjectId", projectID);
            }
            String sprintID = (String) jsonObj.get("SprintId");
            if (sprintID != null && sprintID.length() > 0) {
                updateParent("Planner_Sprint", "SprintId", sprintID);
            }
            String oldProjectID = (String) jsonObj.get("OldProjectId");
            if (oldProjectID != null && oldProjectID.length() > 0 && oldProjectID != projectID) {
                updateParent("Planner_Project", "ProjectId", oldProjectID);
            }
            String oldSprintID = (String) jsonObj.get("OldSprintId");
            if (oldSprintID != null && oldSprintID.length() > 0 && oldSprintID != sprintID) {
                updateParent("Planner_Sprint", "SprintId", oldSprintID);
            }
        } else {
            System.out.println("No arguments.");
        }
    }

    private static void updateParent(String entityName, String fieldName, String parentID)
            throws IOException, ParseException {
        HashMap<String, Object> retrieveData = new HashMap<String, Object>();
        retrieveData.put("Columns", "TargetPoints, RemainingPoints, Status");
        HashMap<String, Object> conditionData = new HashMap<String, Object>();
        conditionData.put("FieldName", fieldName);
        conditionData.put("Operator", 0);
        JSONArray values = new JSONArray();
        values.add(parentID);
        conditionData.put("Values", values);
        JSONObject jsonCondition = new JSONObject(conditionData);
        JSONArray conditions = new JSONArray();
        conditions.add(jsonCondition);
        HashMap<String, Object> filterData = new HashMap<String, Object>();
        filterData.put("Conditions", conditions);
        JSONObject jsonFilter = new JSONObject(filterData);
        retrieveData.put("Filter", jsonFilter);
        JSONObject retrieveDataJson = new JSONObject(retrieveData);
        String request = retrieveDataJson.toJSONString();
        Reader reader = postData("/Planner_Story/RetrieveMultipleRecords", request,
                _sessionID);
        if (reader == null) {
            return;
        }
        JSONParser parser = new JSONParser();
        JSONArray array = (JSONArray) parser.parse(reader);
        long totalTargetPoints = 0, totalRemainingPoints = 0, totalStories = 0,
                remainingStories = 0;
        if (array != null) {
            totalStories = array.size();
            for (int i = 0; i < totalStories; i++) {
                JSONObject story = (JSONObject) array.get(i);
                Object objTargetPoints = story.get("TargetPoints");
                if (objTargetPoints != null) {
                    totalTargetPoints += (long) objTargetPoints;
                }
                Object objRemainingPoints = story.get("RemainingPoints");
                if (objRemainingPoints != null) {
                    totalRemainingPoints += (long) objRemainingPoints;
                }
                int status = Integer.parseInt((String) story.get("Status"));
                if (status == 1) {
                    remainingStories++;
                }
            }
        }
        HashMap<String, Object> updateData = new HashMap<>();
        updateData.put("TargetPoints", totalTargetPoints);
        updateData.put("RemainingPoints", totalRemainingPoints);
        updateData.put("ScoredPoints", totalTargetPoints - totalRemainingPoints);
        updateData.put("TotalStories", totalStories);
        updateData.put("RemainingStories", remainingStories);
        updateData.put("CompletedStories", totalStories - remainingStories);
        updateData.put(entityName + "Id", parentID);
        JSONObject updateRequest = new JSONObject(updateData);
        postData("/" + entityName + "/UpdateRecord", updateRequest.toJSONString(), _sessionID);
    }

    private static Reader postData(String urlSuffix, String body, String sessionID) throws IOException {
        URL url = new URL(_serviceUrl + "api.svc/api" + urlSuffix);
        URLConnection con = url.openConnection();
        HttpURLConnection http = (HttpURLConnection) con;
        http.setRequestMethod("POST");
        http.setDoOutput(true);
        byte[] out = body.getBytes(StandardCharsets.UTF_8);
        http.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        http.setRequestProperty("Content-Length", Integer.toString(out.length));
        if (sessionID != null && sessionID.length() > 0) {
            http.setRequestProperty("SessionId", sessionID);
        }
        try (DataOutputStream os = new DataOutputStream(http.getOutputStream())) {
            os.write(out);
        }
        String errorMessage = http.getHeaderField("ErrorMessage");
        if (errorMessage != null && errorMessage.length() > 0) {
            System.err.println(errorMessage);
            return null;
        }
        return new BufferedReader(new InputStreamReader(http.getInputStream(), "UTF-8"));
    }
}
