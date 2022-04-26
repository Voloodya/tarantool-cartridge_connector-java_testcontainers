local M = {}

M.router_user = 'router_user'
M.router_user_passw = 'secret'

local function create_users()
    local create_user = function(user, passw)
        box.schema.user.create(user, { password = passw, if_not_exists = true })
    end

    create_user(M.router_user, M.router_user_passw)
end
M.create_users = create_users

local function grant_space_access(space_name, user_name)
    box.schema.user.grant(user_name, 'read,write', 'space', space_name, { if_not_exists = true })
end
M.grant_space_access = grant_space_access

local function grant_func_execute(func_name, user_name)
    box.schema.func.create(func_name, { if_not_exists = true })
    box.schema.user.grant(user_name, 'execute', 'function', func_name, { if_not_exists = true })
end
M.grant_func_execute = grant_func_execute

local function grant_all_execute(user_name)
    box.schema.user.grant(user_name, 'execute', 'universe', nil, { if_not_exists = true })
end
M.grant_all_execute = grant_all_execute

local function revoke_func_execute(func_name, user_name)
    local exists = box.schema.func.exists(func_name)
    if exists then
        box.schema.user.revoke(user_name, 'execute', 'function', func_name, { if_exists = true })
        box.schema.func.drop(func_name, { if_exists = true })
    end
end
M.revoke_func_execute = revoke_func_execute

return M
