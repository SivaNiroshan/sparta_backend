package com.sparta.UserService.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
@ConditionalOnProperty(name = "redis.enabled", havingValue = "true", matchIfMissing = true)
public class RedisConfig {

    private static final Logger logger = LoggerFactory.getLogger(RedisConfig.class);

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // Use String serializer for keys
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        
        // Use JSON serializer for values
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        
        // Enable transaction support
        template.setEnableTransactionSupport(false);
        
        template.afterPropertiesSet();
        
        // Test connection
        try {
            RedisConnectionFactory factory = template.getConnectionFactory();
            if (factory != null) {
                factory.getConnection().ping();
                logger.info("Redis connection established successfully");
            }
        } catch (Exception e) {
            logger.error("Failed to connect to Redis: {}", e.getMessage());
            logger.warn("Application will continue but OTP features may not work until Redis is available");
        }
        
        return template;
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}

