package com.imooc.miaosha.redis;

/**
 * @author Created by Wesley on 2018/6/20.
 */
public class GoodsKey extends BasePrefix {

    public static final GoodsKey GOODS_LIST = new GoodsKey(60, "goods_list");

    private GoodsKey(int expireSeconds, String prefix) {
        super(expireSeconds, prefix);
    }

}