const {test}=require('node:test');
const assert=require('node:assert/strict');
const vm=require('node:vm');
const fs=require('node:fs');
const html=fs.readFileSync('src/main/resources/templates/auth/login.html','utf8');
const script=[...html.matchAll(/<script[^>]*>([\s\S]*?)<\/script>/g)].map(m=>m[1]).join('\n');
function fixture(storage=new Map(),identifier='',blocked=false){
 const events={};
 function control(id,value=''){return {value,checked:false,textContent:'',type:'text',addEventListener(name,fn){(events[id+':'+name]??=[]).push(fn);}};}
 const nodes={identifier:control('identifier',identifier),rememberIdentifier:control('rememberIdentifier'),loginForm:control('loginForm'),rememberHelp:control('rememberHelp'),password:control('password','never-store-this-password')};
 vm.runInNewContext(script,{document:{getElementById:id=>nodes[id]||null,querySelector:()=>null},localStorage:{getItem(key){if(blocked)throw Error('Blocked');return storage.get(key)||null;},setItem(key,value){if(blocked)throw Error('Blocked');storage.set(key,value);},removeItem(key){if(blocked)throw Error('Blocked');storage.delete(key);}},setTimeout(){}});
 return {nodes,storage,emit(id,event){for(const callback of events[id+':'+event]||[])callback();}};
}
test('checking remember stores identifier immediately and restores on next visit',()=>{
 const first=fixture();first.nodes.identifier.value=' user@example.com ';first.nodes.rememberIdentifier.checked=true;first.emit('rememberIdentifier','change');
 assert.equal(first.storage.get('cinema-login-identifier'),'user@example.com');
 const next=fixture(first.storage);assert.equal(next.nodes.identifier.value,'user@example.com');assert.equal(next.nodes.rememberIdentifier.checked,true);
});
test('unchecking forgets identifier without requiring login submission',()=>{
 const f=fixture(new Map([['cinema-login-identifier','user@example.com']]));
 f.nodes.rememberIdentifier.checked=false;f.emit('rememberIdentifier','change');assert.equal(f.storage.has('cinema-login-identifier'),false);
});
test('does not overwrite identifier returned by server after failed login',()=>{
 const f=fixture(new Map([['cinema-login-identifier','old@example.com']]),'new@example.com');
 assert.equal(f.nodes.identifier.value,'new@example.com');
 f.emit('loginForm','submit');assert.equal(f.storage.get('cinema-login-identifier'),'new@example.com');
});
test('editing remembered identifier updates it and never stores a password',()=>{
 const f=fixture(new Map([['cinema-login-identifier','old@example.com']]));
 f.nodes.identifier.value='new@example.com';f.emit('identifier','input');f.emit('loginForm','submit');
 assert.deepEqual([...f.storage.entries()],[['cinema-login-identifier','new@example.com']]);
 assert.equal(f.nodes.password.value,'never-store-this-password');
});
test('blocked browser storage does not break login and provides an explanation',()=>{
 const f=fixture(new Map(),'user@example.com',true);f.nodes.rememberIdentifier.checked=true;
 assert.doesNotThrow(()=>f.emit('loginForm','submit'));
 assert.match(f.nodes.rememberHelp.textContent,/blocked local storage/);
});
test('form supports native browser credential autofill',()=>{
 assert.match(html,/id="loginForm"[^>]*autocomplete="on"/);
 assert.match(html,/id="identifier"[^>]*autocomplete="username"/);
 assert.match(html,/id="password"[^>]*autocomplete="current-password"/);
});
