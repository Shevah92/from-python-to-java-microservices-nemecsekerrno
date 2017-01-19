package com.codecool.shipping_cost_calculator_service.controller;

import com.codecool.shipping_cost_calculator_service.model.ShippingOption;
import com.codecool.shipping_cost_calculator_service.service.GoogleMapsAPIService;
import org.json.JSONObject;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;

/**
 * Created by shevah on 10/01/17.
 * Responsible for what's happening when the server gets a request and sends a response
 */
public class ShippingCostCalculatorController {

    private final GoogleMapsAPIService apiService;

    public ShippingCostCalculatorController(GoogleMapsAPIService apiService) {
        this.apiService = apiService;
    }

    /**
     * Checks if the server is running
     * @param request
     * @param response
     * @return
     */
    public String status(Request request, Response response) {
        return "ok";
    }


    /**
     * Creates the response JSON with the shipping options that is sent back to the user
     * <p>Get's the two destination parameters out of the request, and uses the GoogleDistanceMatrixAPI
     * to get the data needed to generate the shipping options</p>
     * @param request
     * @param response
     * @return JSONObject containing the different shipping options
     * @throws IOException
     * @throws URISyntaxException
     */
    public JSONObject generateOptions(Request request, Response response) throws IOException, URISyntaxException {
        String originAddress = request.queryParams("origin");
        String destinationAddress = request.queryParams("destination");

        if (originAddress.trim().isEmpty() || destinationAddress.trim().isEmpty()) {
            return generateErrorJson(response, 400, "Origin or destination cannot be empty or whitespaces only");
        }

        String rawData = apiService.requestData(originAddress, destinationAddress);
        JSONObject extractedData = extractData(response, rawData);

        if (extractedData.has("error")) {
            return extractedData;
        }

        JSONObject allOptionsJSON = new JSONObject();
        for (String type : ShippingOption.TYPES) {
            allOptionsJSON.put(type, (new JSONObject(new ShippingOption(type, extractedData.getString("originFound"),
                    extractedData.getString("destinationFound"), extractedData.getInt("dist"),
                    extractedData.getInt("time")))));
        }
        return allOptionsJSON;
    }

    /**
     * Responsible for handling the different status messages sent back by the GoogleDistanceMatrixAPI
     * <p>Handles the sent back status messages
     * If the status is "OK" get the information out of the GoogleDistanceMatrixAPI response</p>
     * @param res
     * @param rawData
     * @return JSONObject, containing information about what went wrong, if the status is okay returns the data needed to calculate shipping options
     */
    public JSONObject extractData(Response res, String rawData) {
        JSONObject rawDataJSON = new JSONObject(rawData);
        String status = rawDataJSON.getString("status");
        JSONObject elements;
        String elementsStatus;

        switch (status){
            case "OVER_QUERY_LIMIT":
                return generateErrorJson(res, 429,
                        "The application has requested too many elements within the allowed time period." +
                                "The request should succeed if you try again after a reasonable amount of time.");
            case "REQUEST_DENIED":
                return generateErrorJson(res, 500,
                        "The service denied use of the Distance Matrix service by your web page.");
            case "UNKNOWN_ERROR":
                return generateErrorJson(res, 500,
                        "A Distance Matrix request could not be processed due to a server error." +
                                "The request may succeed if you try again.");
            case "OK":
                elements = rawDataJSON.getJSONArray("rows").getJSONObject(0).getJSONArray("elements").getJSONObject(0);
                elementsStatus = elements.getString("status");
                break;
            default:
                return generateErrorJson(res, 501, "Distance Matrix status message not implemented");
        }

        switch (elementsStatus) {
            case "NOT_FOUND":
                return generateErrorJson(res, 400,
                        "The origin and/or destination of this pairing could not be geocoded.");
            case "ZERO_RESULTS":
                return generateErrorJson(res, 400, "No route could be found between the origin and destination.");
            case "OK":
                Integer rawDistance = elements.getJSONObject("distance").getInt("value");
                Integer rawTime = elements.getJSONObject("duration").getInt("value");
                HashMap<String, Object> optionValues = new HashMap<>();
                optionValues.put("dist", rawDistance/1000.f);
                optionValues.put("time", rawTime/3600.f);
                optionValues.put("originFound", rawDataJSON.getJSONArray("origin_addresses").get(0));
                optionValues.put("destinationFound", rawDataJSON.getJSONArray("destination_addresses").get(0));
                return new JSONObject(optionValues);
            default:
                return generateErrorJson(res, 501, "Distance Matrix elements status message not implemented");
        }
    }


    private JSONObject generateErrorJson(Response response, int statusCode, String message) {
        response.status(statusCode);
        response.type("application/json");
        JSONObject errorJson = new JSONObject();
        errorJson.put("error", message);
        return errorJson;
    }
}