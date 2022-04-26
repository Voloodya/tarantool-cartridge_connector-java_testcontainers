cartridge = require('cartridge')

replicasets = {{
    alias = "app-router",
    roles = {'vshard-router','crud-router','app.roles.router'},
    join_servers = {{uri = "localhost:3301"}}
},
{
    alias = "s1-storage",
    roles = {'vshard-storage','crud-storage','app.roles.storage'},
    join_servers = {{uri = "localhost:3302"}}
},{
    alias = "s1-replica",
    roles = {'vshard-storage','crud-storage','app.roles.storage'},
    join_servers = {{uri = "localhost:3303"}}
 }
}

return cartridge.admin_edit_topology({replicasets = replicasets})
