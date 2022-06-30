import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class App {

    private static String _sessionID, _serviceUrl;

    public static void main(String[] args) throws Exception {
        if (args.length > 0) {
            JSONParser parser = new JSONParser();
            JSONObject jsonObj = (JSONObject) parser.parse(args[0]);
            _sessionID = (String) jsonObj.get("SessionId");
            _serviceUrl = (String) jsonObj.get("ServiceUrl");
            long taskUpdateScoredPoints = Long.parseLong((String) jsonObj.get("ScoredPoints"));
            String operation = (String) jsonObj.get("Operation");
            String storyId = (String) jsonObj.get("StoryId");
            long storyScoredPoints = 0, storyTargetPoints = 0, storyRemainingPoints = 0;
            if (storyId != null && storyId.length() > 0) {
                HashMap<String, Object> retrieveData = new HashMap<String, Object>();
                retrieveData.put("Columns", "ScoredPoints,TargetPoints");
                // JSONObject jsonFilter = getFilter("Planner_StoryId", 0, storyId);
                // retrieveData.put("Filter", jsonFilter);
                retrieveData.put("Planner_StoryId", storyId);
                JSONObject retrieveDataJson = new JSONObject(retrieveData);
                String request = retrieveDataJson.toJSONString();
                Reader reader = postData("/Planner_Story/RetrieveRecord", request, _sessionID);

                if (reader != null) {
                    JSONObject story = (JSONObject) parser.parse(reader);
                    Object objScoredPoints = story.get("ScoredPoints");
                    Object objTargetPoints = story.get("TargetPoints");
                    storyScoredPoints = objScoredPoints != null ? (long) objScoredPoints : 0;
                    storyTargetPoints = objTargetPoints != null ? (long) objTargetPoints : 0;
                    if (operation.equals("Create")) {
                        storyScoredPoints += taskUpdateScoredPoints;
                        storyRemainingPoints = storyTargetPoints - storyScoredPoints;
                    } else if (operation.equals("Delete")) {
                        storyScoredPoints -= taskUpdateScoredPoints;
                        storyRemainingPoints = storyTargetPoints - storyScoredPoints;
                    } else if (operation.equals("Update")) {
                        long oldTaskUpdateScoredPoints = Long.parseLong((String) jsonObj.get("OldScoredPoints"));
                        storyScoredPoints = storyScoredPoints - oldTaskUpdateScoredPoints + taskUpdateScoredPoints;
                        storyRemainingPoints = storyTargetPoints - storyScoredPoints;
                    }
                    HashMap<String, Object> updateData = new HashMap<>();
                    updateData.put("ScoredPoints", storyScoredPoints);
                    updateData.put("RemainingPoints", storyRemainingPoints);
                    updateData.put("Planner_Story" + "Id", storyId);
                    JSONObject updateRequest = new JSONObject(updateData);
                    postData("/Planner_Story/UpdateRecord", updateRequest.toJSONString(), _sessionID);
                }
            }
        } else {
            System.out.println("No Arguments");
        }
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
