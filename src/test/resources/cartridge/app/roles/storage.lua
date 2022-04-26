local cartridge = require('cartridge')
local user_helper = require('app.utils.user_helper')
local cartridge_rpc = require('cartridge.rpc')

--- Grant permissions to call API methods.
--- This method is idempotent.
local function init_permissions()
    -- Grant user permissions to be able to execute CRUD functions
    local crud_function = {
        "crud.insert",
        "crud.insert_object",
        "crud.get",
        "crud.replace",
        "crud.replace_object",
        "crud.update",
        "crud.upsert",
        "crud.upsert_object",
        "crud.delete",
        "crud.select",
    }
    for _, fn in ipairs(crud_function) do
        user_helper.grant_func_execute(fn, user_helper.router_user)
    end
    return true
end

local function get_schema()
    for _, instance_uri in pairs(cartridge_rpc.get_candidates('app.roles.storage', { leader_only = true })) do
        return cartridge_rpc.call('app.roles.storage', 'get_schema', nil, { uri = instance_uri })
    end
end

local function init(opts) -- luacheck: no unused args
    -- if opts.is_master then
    -- end

    local httpd = assert(cartridge.service_get('httpd'), "Failed to get httpd service")
    httpd:route({method = 'GET', path = '/hello'}, function()
        return {body = 'Hello world!'}
    end)

    rawset(_G, 'ddl', { get_schema = get_schema })
    return true
end

local function stop()
    return true
end

local function validate_config(conf_new, conf_old) -- luacheck: no unused args
    return true
end

local function apply_config(conf, opts) -- luacheck: no unused args
    -- if opts.is_master then
    -- end
    return true
end

return {
    role_name = 'app.roles.storage',
    init = init,
    stop = stop,
    validate_config = validate_config,
    apply_config = apply_config,
    get_schema = require('ddl').get_schema,
    dependencies = {'cartridge.roles.crud-storage'},
    dependencies = {'cartridge.roles.vshard-storage'},
}
