# 撮合引擎

## 当前各挂单池的抽象模型:

```text
                      Limit Pool:      

                +----+-------+--------+
                | id | price | amount |
Sell Pool:      +----+-------+--------|
                |    |       |        |
                |    |       |        |
                          .            
                          .            
                          .            

                +----+-------+--------+
Buy Pool:       | id | price | amount |
                +----+-------+--------|
                |    |       |        |
                |    |       |        |
                          .            
                          .            
                          .            
```

## 新增挂单撮合顺序:

| 挂单类型 | 挂单撮合流程 | 挂单归属池 |
| ------- | -------- | --------- |
| 限价买单 (Limit Buy) | Limit Sell Pool | Limit Buy Pool |
| 限价卖单 (Limit Sell) | Limit Buy Pool | Limit Sell Pool |
| 市价买单 (Market Buy) | Limit Sell Pool | (cancel order) |
| 市价卖单 (Market Sell) | Limit Buy Pool | (cancel order) |

- **"挂单类型":** 新加入撮合引擎的挂单.
- **"挂单撮合流程":** 新挂单将根据自身**类型**匹配对应的挂单撮合流程, 以进行**撮合** .
- **"挂单归属池":** 当挂单在撮合流程执行结束后仍具备 **被撮合能力**, 则将挂单放置到与类型相匹配的挂单归属池.
  (在池中**等待**被下一个新挂单撮合)
