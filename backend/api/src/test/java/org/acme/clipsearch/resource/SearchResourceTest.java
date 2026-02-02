package org.acme.clipsearch.resource;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.entity.StringEntity;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.io.IOException;

import static org.hamcrest.Matchers.is;

/**
 * Integration tests for the SearchResource REST API endpoint.
 * Tests the search functionality with mocked Elasticsearch backend.
 */
@QuarkusTest
public class SearchResourceTest {

    @InjectMock
    RestClient restClient;

    /**
     * Test the search API endpoint with a mock Elasticsearch response.
     * Verifies that the API correctly:
     * 1. Receives search queries
     * 2. Forwards them to Elasticsearch
     * 3. Transforms the response to the expected format
     * 4. Returns search results with proper HTTP status and JSON structure
     * 
     * @throws IOException if there's an error reading the mock response
     */
    @Test
    public void testSearchEndpoint() throws IOException {
        // Create mock Elasticsearch response JSON with search results
        String esResponse = "{\"hits\":{\"total\":{\"value\":1},\"hits\":[{\"_id\":\"123\",\"_source\":{\"filename\":\"test.txt\"}}]}}";
        
        // Mock the HTTP response from Elasticsearch
        Response mockResponse = Mockito.mock(Response.class);
        HttpEntity mockEntity = new StringEntity(esResponse);
        Mockito.when(mockResponse.getEntity()).thenReturn(mockEntity);
        
        // Configure the mock RestClient to return the mocked response
        Mockito.when(restClient.performRequest(ArgumentMatchers.any(Request.class)))
                .thenReturn(mockResponse);

        // Execute the search request and verify the response
        RestAssured.given()
          .when().get("/api/search?q=test")     // Send search query for "test"
          .then()
             .statusCode(200)                     // Verify HTTP 200 OK response
             .body("total", is(1))                // Verify total hit count is 1
             .body("items[0].id", is("123"))     // Verify document ID matches
             .body("items[0].filename", is("test.txt"));  // Verify filename matches
    }
}
