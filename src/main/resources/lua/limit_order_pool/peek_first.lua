---@type string
local poolName = ARGV[1]
---@type boolean true(buy) | false(sell)
local isBuy = ARGV[2] == 'buy'

local function peek_first()
    local mapName = poolName .. '::map'

    local prices
    if isBuy then
        prices = redis.call('ZREVRANGEBYSCORE', poolName, '+INF', '-INF', 'LIMIT', 0, 1)
    else
        prices = redis.call('ZRANGEBYSCORE', poolName, '-INF', '+INF', 'LIMIT', 0, 1)
    end
    if (prices[1] == nil) then
        return nil
    end
    local queueName = poolName .. '::queue::' .. prices[1]

    local orderIds = redis.call('LRANGE', queueName, 0, 0)
    local amount = redis.call('HGET', mapName, orderIds[1])
    return orderIds[1] .. ',' .. prices[1] .. ',' .. amount
end

return peek_first()
