package com.hmall.item.config;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ElasticsearchConfig {

    @Bean
    public RestHighLevelClient restHighLevelClient() {
        RestClientBuilder clientBuilder = RestClient.builder(
                new HttpHost("192.168.85.129", 9200, "http")
        );

        return new RestHighLevelClient(clientBuilder);
    }
}
