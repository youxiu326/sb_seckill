package com.youxiu326.queue.redis;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import java.util.concurrent.*;


/**
 * redis实现发布订阅 https://www.cnblogs.com/youxiu326/p/10540755.html
 */
@Configuration
public class RedisSubListenerConfig {

    //初始化监听器
    @Bean
    RedisMessageListenerContainer container(RedisConnectionFactory connectionFactory,
                                            MessageListenerAdapter listenerAdapter) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(listenerAdapter, new PatternTopic("seckill"));
        /**
         * 使用 Redis 消息队列，出现超卖问题是因为RedisMessageListenerContainer 的默认使用线程池是SimpleAsyncTaskExecutor，
         * 每次消费都会创建一个线程来处理，这样就会有大量的新线程被创建。
         * 如果不定义线程池，每一次消费都会创建一个线程，如果业务层面不做限制，就会导致秒杀超卖
         * @link https://www.cnblogs.com/smallSevens/archive/2019/10/17/11691432.html
         */
        ThreadFactory factory = new ThreadFactoryBuilder()
                .setNameFormat("redis-listener-pool-%d").build();
        Executor executor = new ThreadPoolExecutor(
                1,
                1,
                5L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(1000),
                factory);
        container.setTaskExecutor(executor);
        return container;
    }
    //利用反射来创建监听到消息之后的执行方法
    @Bean
    MessageListenerAdapter listenerAdapter(RedisConsumer redisReceiver) {
        return new MessageListenerAdapter(redisReceiver, "receiveMessage");
    }
   //使用默认的工厂初始化redis操作模板
    @Bean
    StringRedisTemplate template(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }
}