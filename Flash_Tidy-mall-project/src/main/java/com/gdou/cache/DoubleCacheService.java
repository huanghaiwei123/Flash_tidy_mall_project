package com.gdou.cache;

import com.github.benmanes.caffeine.cache.Cache;
import org.apache.juli.logging.LogFactory;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.annotation.Resource;
import javax.annotation.PreDestroy;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.Random;
import java.util.function.Supplier;

@Service
public class DoubleCacheService {
    @Resource(name="spuDetailCache")
    private Cache<String,String> cache;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private RedissonClient redissonClient;

    private static final String PREFIX = "goods:detail:";
    private static final long TTL = 10;  // Redis TTL 分钟
    private static final long DOUBLE_DELETE_DELAY_MILLIS = 500;

    private final ScheduledExecutorService delayedDeleteExecutor =
            Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
                @Override
                public Thread newThread(Runnable runnable) {
                    Thread thread = new Thread(runnable, "cache-delayed-delete");
                    thread.setDaemon(true);
                    return thread;
                }
            });


    /**
     * 读本地缓存  ->  redis -> 数据库
     * @param key
     * @param dbLoader
     * @return
     */
    public String get(String key, Supplier<String> dbLoader) {
        Random r = new Random();
//        本地缓存caffeine
        String local= cache.getIfPresent(key);
        if (local != null) {
            return local;
        }
//        redis
        Object remote= redisTemplate.opsForValue().get(PREFIX+key);
        if (remote != null) {
            cache.put(key, remote.toString()); //回填本地
            return remote.toString();
        }
//       都miss，就走数据库，redisson防击穿,保证同一时间只有一个线程查库
        RLock lock = redissonClient.getLock(PREFIX + key);
        try {
//            拿不到锁说明别人在查库，最多等一秒
            if(lock.tryLock(1, TimeUnit.SECONDS)) {
//            拿到锁之后再次查redis，防止上一个线程已经填好数据
                local = cache.getIfPresent(key);
                if (local != null) {
                    return local;
                }
                remote = redisTemplate.opsForValue().get(PREFIX + key);
                if (remote != null) {
                    cache.put(key, remote.toString());
                    return remote.toString();
                }
                String db = dbLoader.get();
                if (db != null) {
                    cache.put(key, db);
                    redisTemplate.opsForValue().set(PREFIX + key, db, TTL+r.nextInt(4), TimeUnit.MINUTES);
                    return db;
                } else {
//                db没有数据则缓存空值
                    cache.put(key, "");
                    redisTemplate.opsForValue().set(PREFIX + key, "", 10, TimeUnit.SECONDS);
                    return "";
                }
            }else{
                Thread.sleep(100);
                if(cache.getIfPresent(key)!=null){
                    return cache.getIfPresent(key);
                }else{
                    return get(key,dbLoader);
                }
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }finally{
//            只解锁当前线程拿到的锁
            if(lock.isHeldByCurrentThread()){
                lock.unlock();
            }
        }
    }

    /**
     * 延迟双删
     * @param key 缓存业务 key，不包含 goods:detail: 前缀
     */
    public void evictWithDelay(String key) {
        Runnable r =()->{
            try {
                evict(key);
            }catch (Exception e){
                LogFactory.getLog(DoubleCacheService.class).error("evictWithDelay",e);
            }
        };
//        等待事务提交
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    evict(key);
                }
            });
        } else {
            evict(key);
        }
//        这一步是为了有数据库主从复制准备的
        delayedDeleteExecutor.schedule(r, DOUBLE_DELETE_DELAY_MILLIS, TimeUnit.MILLISECONDS);
    }

    /**
     * 删除 Redis 和本地 Caffeine 两级缓存。
     */
    private void evict(String key) {
        redisTemplate.delete(PREFIX + key);
        cache.invalidate(key);
    }

    @PreDestroy
    public void shutdownDelayedDeleteExecutor() {
        delayedDeleteExecutor.shutdown();
    }
}
