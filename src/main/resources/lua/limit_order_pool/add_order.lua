---@type string
local poolName = ARGV[1]
---@type boolean true(buy) | false(sell)
local isBuy = ARGV[2] == 'buy'
---@class Order
local order = {
    id = ARGV[3],
    price = ARGV[4],
    amount = ARGV[5],
}

local function add_order()
    local queueName = poolName .. '::queue::' .. order.price
    local mapName = poolName .. '::map'

    -- 新增到 `map`
    -- 如果此次操作覆盖了原来的挂单, 则直接返回
    if redis.call('HSET', mapName, order.id, order.amount) == 0 then
        return 0
    end
    -- 否则追加到 `当前 price` 的 `队列` 中
    redis.call('RPUSH', queueName, order.id)

    -- 如果不是首次创建 `order 队列`, 则直接返回
    if redis.call('LLEN', queueName) ~= 1 then
        return 1
    end
    -- 否则将 `order 队列` 添加到 `price 队列`
    redis.call('ZADD', poolName, order.price, order.price)
    return 2
end

return add_order()
