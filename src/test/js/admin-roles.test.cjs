const {test}=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs');const vm=require('node:vm');
const html=fs.readFileSync('src/main/resources/templates/admin/roles.html','utf8');
const script=[...html.matchAll(/<script[^>]*>([\s\S]*?)<\/script>/g)].map(m=>m[1]).join('\n');
function fixture(){
 const elements=new Map(),calls=[],replies=[];
 const element=id=>{if(!elements.has(id))elements.set(id,{innerHTML:'',textContent:'',style:{},classList:{add(){},remove(){}}});return elements.get(id)};
 let confirmed=true;
 const context=vm.createContext({document:{cookie:'',addEventListener(){},getElementById:element,querySelectorAll(){return []}},
  confirm:()=>confirmed,setTimeout(){},console:{error(){}},
  fetch:async(url,options)=>{calls.push({url,options});const reply=replies.shift()||{success:true};return{ok:reply.ok??reply.success,json:async()=>reply}}});
 vm.runInContext(script,context);
 vm.runInContext('roles=[{roleId:1,roleName:"CUSTOMER",permissions:["movie:read"],mandatoryPermissions:["movie:read"],allowedPermissions:["movie:read","booking:create"]}];allPermissions=[{name:"movie:read"},{name:"booking:create"},{name:"account:manage"}];selectedRoleId=1;',context);
 return{element,calls,replies,run:code=>vm.runInContext(code,context),cancel:()=>confirmed=false};
}
test('mandatory permission cannot be removed and cancelled removal sends no request',async()=>{
 const f=fixture();await f.run('removePermission(1,"movie:read")');assert.equal(f.calls.length,0);
 f.cancel();await f.run('removePermission(1,"booking:create")');assert.equal(f.calls.length,0);
});
test('available permissions respect role scope and required controls are disabled',()=>{
 const f=fixture();f.run('renderPermissionPanel(roles[0])');const panel=f.element('panelBody').innerHTML;
 assert.match(panel,/booking:create/);assert.doesNotMatch(panel,/account:manage/);assert.match(panel,/disabled aria-disabled="true"/);
});
test('successful add refreshes saved role and failed update keeps existing permissions',async()=>{
 const f=fixture();f.replies.push({success:true},{success:true,data:[{roleId:1,roleName:'CUSTOMER',permissions:['movie:read','booking:create'],mandatoryPermissions:['movie:read'],allowedPermissions:['movie:read','booking:create']}]},{success:true,data:[{name:'movie:read'},{name:'booking:create'}]});
 await f.run('addPermission(1,"booking:create")');assert.equal(f.calls[0].url,'/api/admin/roles/1/permissions/add');
 assert.deepEqual(JSON.parse(f.calls[0].options.body),{permissionName:'booking:create'});
 assert.equal(f.run('roles[0].permissions.length'),2);
 f.replies.push({success:false,message:'Rejected'});await f.run('removePermission(1,"booking:create")');assert.equal(f.run('roles[0].permissions.length'),2);
});
test('failed data refresh does not replace role data with an empty list',async()=>{
 const f=fixture();f.replies.push({success:false,ok:false},{success:true,data:[]});await f.run('refreshRoleData(1)');
 assert.equal(f.run('roles.length'),1);
});
