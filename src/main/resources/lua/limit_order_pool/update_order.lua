---@type string
local poolName = ARGV[1]
---@class Order
local order = {
    id = ARGV[2],
    price = ARGV[3],
    amount = ARGV[4],
}

local function update_order()
    local mapName = poolName .. '::map'
    redis.call('HSET', mapName, order.id, order.amount)
end

return update_order()
