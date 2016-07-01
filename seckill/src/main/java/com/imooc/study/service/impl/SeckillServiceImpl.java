package com.imooc.study.service.impl;

import com.imooc.study.dao.SeckillDao;
import com.imooc.study.dao.SuccessKilledDao;
import com.imooc.study.dto.Exposer;
import com.imooc.study.dto.SeckillExecution;
import com.imooc.study.entity.Seckill;
import com.imooc.study.entity.SuccessKilled;
import com.imooc.study.enums.SeckillStatEnum;
import com.imooc.study.exception.RepeatKillException;
import com.imooc.study.exception.SeckillCloseException;
import com.imooc.study.exception.SeckillException;
import com.imooc.study.service.SeckillService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 *
 * Created by Wesley on 2016/5/21.
 */
@Service
public class SeckillServiceImpl implements SeckillService {

    private Log log = LogFactory.getLog(this.getClass());

    private final String salt = "/wesley_666(-).com";

    @Autowired
    SeckillDao seckillDao;

    @Autowired
    SuccessKilledDao successKilledDao;

    @Override
    public List<Seckill> getSeckillList() {
        return seckillDao.queryAll(0, 4);
    }

    @Override
    public Seckill getById(long seckillId) {
        return seckillDao.queryById(seckillId);
    }

    @Override
    public Exposer exportSeckillUrl(long seckillId) {
        Seckill seckill = seckillDao.queryById(seckillId);
        if(Objects.isNull(seckill))
            return new Exposer(seckillId, false);

        Date start = seckill.getStartTime();
        Date end = seckill.getEndTime();
        Date now = new Date();
        if(now.getTime() > end.getTime() || now.getTime() < start.getTime()){
            return new Exposer(false, seckillId, now.getTime(), start.getTime(), end.getTime());
        }

        return new Exposer(true, seckillId, signature(seckillId));
    }

    /**
     * MD5签名
     * @param seckillId
     * @return
     */
    public String signature(long seckillId){
        String sign = seckillId + salt;
        return DigestUtils.md5DigestAsHex(sign.getBytes());
    }

    @Override
    @Transactional
    /**
     * 使用注解控制事务的优点:
     * 1.开发团队达成一致约定,明确标注事务方法的编程风格.
     * 2.保证事务方法的执行时间尽可能短,不要穿插其他网络操作RPC/HTTP请求或者 剥离到事务方法外部.
     * 3.不是所有的方法都需要事务.如一些查询的service.只有一条修改操作的service.只读操作不需要事务控制
     */
    public SeckillExecution executeSeckill(long seckillId, long userPhone, String sign) throws SeckillException {
        if(StringUtils.isEmpty(sign) || !sign.equals(signature(seckillId))){
            throw  new SeckillException("签名错误");
        }
        //执行秒杀逻辑:1.减库存.2.记录购买行为
        Date now = new Date();
        try {
            int updateCount = seckillDao.reduceNumber(seckillId, now);
            if(updateCount <= 0) {
                throw new SeckillCloseException("秒杀已关闭");
            }

            int inserCount = successKilledDao.insertSuccessKilled(seckillId, userPhone);
            if(inserCount <= 0) {
                throw new RepeatKillException("重复秒杀");
            }

            SuccessKilled successKilled = successKilledDao.queryByIdWithSeckill(seckillId, userPhone);
            return new SeckillExecution(seckillId, SeckillStatEnum.SUCCESS, successKilled);
        }catch (SeckillCloseException e){
            throw e;
        }catch (RepeatKillException e) {
            throw e;
        }catch (Exception e){
            log.error(e.getMessage());
            throw new SeckillException("系统异常");
        }
    }
}
