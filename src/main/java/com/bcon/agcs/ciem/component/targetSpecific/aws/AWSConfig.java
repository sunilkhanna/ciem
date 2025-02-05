package com.bcon.agcs.ciem.component.targetSpecific.aws;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClientBuilder;
import com.bcon.agcs.ciem.configuration.Config;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

@Slf4j
@Configuration
@PropertySource("classpath:targets/aws.properties")
@Import(Config.class)
public class AWSConfig {
    @Value("${accessKey}")
    private String accessKey;
    @Value("${accessSecret}")
    private String accessSecret;
    @Value("${region}")
    private String region;

    @Bean
    public AmazonIdentityManagement getIAMClient() {
        return AmazonIdentityManagementClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, accessSecret)))
                .withRegion(region)
                .build();
    }
}
