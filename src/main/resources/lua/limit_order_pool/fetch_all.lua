---@type string
local poolName = ARGV[1]

---@param namespace string
local function fetch_all()
    local queueNamePrefix = poolName .. '::queue::'
    local mapName = poolName .. '::map'
    local resultSet = {}

    -- 遍历现有的所有 `price`
    local orderPool = redis.call('ZRANGE', poolName, 0, -1)
    for _, price in ipairs(orderPool) do
        local result = {}

        -- 遍历当前 `price` 的所有订单
        local orderIds = redis.call('LRANGE', queueNamePrefix .. price, 0, -1)
        for _, orderId in ipairs(orderIds) do
            local amount = redis.call('HGET', mapName, orderId)
            table.insert(result, orderId .. ',' .. price .. ',' .. amount)
        end
        table.insert(resultSet, result)
    end
    return resultSet
end

return fetch_all()
