package com.bcon.agcs.ciem.component.targetSpecific.azure;

import com.bcon.agcs.ciem.configuration.Config;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Configuration
@PropertySource("classpath:targets/azure.properties")
@Import(Config.class)
public class AzureConfig {

    @Value("${auth_server_url}")
    private String authServerUrl;
    @Value("${grant_type}")
    private String grantType;
    @Value("${client_id}")
    private String clientId;
    @Value("${client_secret}")
    private String clientSecret;
    @Value("${scope}")
    private String scope;




    @Bean("accessToken")
    public String getAccessToken() {
        try {
            var headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            var requestBody = new LinkedMultiValueMap<String, String>();
            requestBody.add("grant_type", grantType);
            requestBody.add("client_id", clientId);
            requestBody.add("client_secret", clientSecret);
            requestBody.add("scope", scope);

            var restTemplate = new RestTemplate();
            var request = new HttpEntity<>(requestBody, headers);

            var response = restTemplate
                    .exchange(authServerUrl, HttpMethod.POST, request, String.class);

            var responseJson = new JSONObject(response.getBody());
            return responseJson.optString("access_token");
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

}
