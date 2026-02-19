package com.sparta.UserService.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Provides a mock RedisTemplate for tests so that no local Redis is required.
 * Use with profile "test" and redis.enabled=false.
 */
@TestConfiguration
public class TestRedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        @SuppressWarnings("unchecked")
        RedisTemplate<String, Object> template = mock(RedisTemplate.class);
        ValueOperations<String, Object> valueOps = mock(ValueOperations.class);
        when(template.opsForValue()).thenReturn(valueOps);
        // No-op stubs: get returns null, set does nothing (void), delete returns true
        when(valueOps.get(anyString())).thenReturn(null);
        doNothing().when(valueOps).set(anyString(), any());
        doNothing().when(valueOps).set(anyString(), any(), anyLong(), any(java.util.concurrent.TimeUnit.class));
        // delete() returns Boolean, not void
        when(template.delete(anyString())).thenReturn(true);
        return template;
    }
}
