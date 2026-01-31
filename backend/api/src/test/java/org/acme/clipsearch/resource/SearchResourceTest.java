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

@QuarkusTest
public class SearchResourceTest {

    @InjectMock
    RestClient restClient;

    @Test
    public void testSearchEndpoint() throws IOException {
        // Mock Elasticsearch response
        String esResponse = "{\"hits\":{\"total\":{\"value\":1},\"hits\":[{\"_id\":\"123\",\"_source\":{\"filename\":\"test.txt\"}}]}}";
        
        Response mockResponse = Mockito.mock(Response.class);
        HttpEntity mockEntity = new StringEntity(esResponse);
        Mockito.when(mockResponse.getEntity()).thenReturn(mockEntity);
        
        Mockito.when(restClient.performRequest(ArgumentMatchers.any(Request.class)))
                .thenReturn(mockResponse);

        RestAssured.given()
          .when().get("/api/search?q=test")
          .then()
             .statusCode(200)
             .body("total", is(1))
             .body("items[0].id", is("123"))
             .body("items[0].filename", is("test.txt"));
    }
}
