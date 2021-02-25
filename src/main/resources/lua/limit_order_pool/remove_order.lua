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

local function remove_order()
    local queueName = poolName .. '::queue::' .. order.price
    local mapName = poolName .. '::map'

    -- 从 `map` 中删除 `挂单`, 如果 `挂单` 已经不存在, 则直接返回
    local deleted = redis.call('HDEL', mapName, order.id)
    if deleted ~= 1 then
        return 0
    end

    -- 从 `队列` 中移除 `挂单`
    redis.call('LREM', queueName, 1, order.id)

    -- 当 `队列` 在移除挂单后仍存在 `挂单`, 则直接返回
    local queueLen = redis.call('LLEN', queueName)
    if queueLen ~= 0 then
        return 1
    end

    -- 从 `挂单池` 中移除空的 `队列`
    redis.call('ZREM', poolName, order.price)
    return 2
end

return remove_order()
