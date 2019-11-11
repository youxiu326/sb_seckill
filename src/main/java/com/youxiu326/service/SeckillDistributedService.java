package com.youxiu326.service;

import com.youxiu326.common.result.JSONResult;

public interface SeckillDistributedService {


    /**
     * redis分布式锁秒杀
     * @param seckillId
     * @param userId
     * @return
     */
    JSONResult redisLockSeckil(long seckillId, long userId);

    /**
     * zookeeper分布式锁秒杀
     * @param seckillId
     * @param userId
     * @return
     */
    JSONResult zookeeperLockSeckil(long seckillId,long userId);

    /**
     * 删除秒杀售卖商品记录
     * @param seckillId
     * @return
     */
    void deleteSeckill(long seckillId);

    /**
     * 查询秒杀售卖商品
     * @param seckillId
     * @return
     */
    Long getSeckillCount(long seckillId);

}
