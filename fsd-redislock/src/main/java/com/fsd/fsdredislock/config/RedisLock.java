package com.fsd.fsdredislock.config;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * created by lilei
 * since 2019/8/17
 **/
@Component
public class RedisLock {
    private StringRedisTemplate redisTemplate;

    /**
     * 加锁
     * @param key 被秒杀商品的id
     * @param value 当前线程操作时的 System.currentTimeMillis() + 2000，2000是超时时间，这个地方不需要去设置redis的expire，
     *              也不需要超时后手动去删除该key，因为会存在并发的线程都会去删除，造成上一个锁失效，结果都获得锁去执行，并发操作失败了就。
     * @return
     */
    public boolean lock(String key, String value) {
        //如果key值不存在，则返回 true，且设置 value
        if (redisTemplate.opsForValue().setIfAbsent(key, value)) {
            return true;
        }

        //获取key的值，判断是是否超时
        String curVal = redisTemplate.opsForValue().get(key);
        if (!StringUtils.isEmpty(curVal) && Long.parseLong(curVal) < System.currentTimeMillis()) {
            //获得之前的key值，同时设置当前的传入的value。这个地方可能几个线程同时过来，但是redis本身天然是单线程的，所以getAndSet方法还是会安全执行，
            //首先执行的线程，此时curVal当然和oldVal值相等，因为就是同一个值，之后该线程set了自己的value，后面的线程就取不到锁了
            String oldVal = redisTemplate.opsForValue().getAndSet(key, value);
            if(!StringUtils.isEmpty(oldVal) && oldVal.equals(curVal)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 解锁
     * @param key
     * @param value
     */
    public void unlock(String key, String value) {
        try {
            String curVal = redisTemplate.opsForValue().get(key);
            if (!StringUtils.isEmpty(curVal) && curVal.equals(value)) {
                redisTemplate.opsForValue().getOperations().delete(key);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
