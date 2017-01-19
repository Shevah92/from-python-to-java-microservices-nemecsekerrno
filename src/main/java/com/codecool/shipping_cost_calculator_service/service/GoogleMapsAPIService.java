package com.codecool.shipping_cost_calculator_service.service;

import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by shevah on 10/01/17.
 * Responsible for communicating with the GoogleDistanceMatrixAPI
 */
public class GoogleMapsAPIService {
    /**
     * Link to the GoogleDistanceMatrixAPI
     */
    private static final String GoogleAPIURL= "https://maps.googleapis.com/maps/api/distancematrix/json";

    /**
     * APIKey to GoogleDistanceMatrixAPI
     */
    private static final String GoogleAPIKey = "AIzaSyBLun5iVfffgdRpSLtqmbsyYMrBRYfpG78";

    private static GoogleMapsAPIService INSTANCE;

    /**
     * As there is one GoogleDistanceMatrixAPI a singleton is used to generate the connection to it
     * @return
     */
    public static GoogleMapsAPIService getINSTANCE() {
        if (INSTANCE == null) {
            INSTANCE = new GoogleMapsAPIService();
        }
        return INSTANCE;
    }

    /**
     * Responsible for getting the data from GoogleDistanceMatrixAPI
     * @param origin The location of the store/warehouse
     * @param destination The location where the product needs to be delivered
     * @return The response JSOn from GoogleDistanceMatrixAPI
     * @throws IOException
     * @throws URISyntaxException
     */
    public String requestData(String origin, String destination) throws IOException, URISyntaxException {
        URIBuilder builder = new URIBuilder(GoogleAPIURL).addParameter("origins", origin)
                .addParameter("destinations", destination).addParameter("key", GoogleAPIKey);
        return execute(builder.build());
    }

    /**
     * Responsible for creating the request URL to the GoogleDistanceMatrixAPI
     * @param uri
     * @return
     * @throws IOException
     */
    private String execute(URI uri) throws IOException {
        return Request.Get(uri)
                .execute()
                .returnContent()
                .asString();
    }

}
