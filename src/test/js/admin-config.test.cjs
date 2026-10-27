const {test} = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const html = fs.readFileSync('src/main/resources/templates/admin/config.html', 'utf8');
const script = [...html.matchAll(/<script[^>]*>([\s\S]*?)<\/script>/g)].map(m => m[1]).join('\n');
function fixture() {
  const elements = new Map(); const events = {}; const calls = []; const replies = [];
  function element(id) {
    if (!elements.has(id)) elements.set(id, { id, value: '10', type: 'number', disabled: false, textContent: '', innerHTML: '', attrs: {},
      classList: {toggle() {}, remove() {}, add() {}}, setAttribute(k,v) {this.attrs[k]=v;}, checkValidity() {return true;}, focus() {}, querySelectorAll() {return [];} });
    return elements.get(id);
  }
  const context = vm.createContext({document: {cookie:'', addEventListener() {}, getElementById:element}, window: {addEventListener(name, fn) {events[name]=fn;}},
    fetch: async (url, options) => {calls.push({url, options}); const reply=replies.shift() || {success:true}; return {ok:reply.success, json:async()=>reply};},
    setTimeout() {}, console});
  vm.runInContext(script, context);
  vm.runInContext('settings = {VOUCHER:[{settingId:1, category:"VOUCHER", settingName:"Discount", valueType:"NUMBER", value:"10", defaultValue:"20", validationRules:"min:1,max:100"}]}; currentValues={1:"10"};', context);
  return {context,element,calls,replies,events, run: code=>vm.runInContext(code,context)};
}
test('editing does not auto-save; explicit save persists once and clears dirty state', async()=>{
  const f=fixture(); f.run('handleInputChange(1,"16")');
  assert.equal(f.calls.length,0); assert.equal(f.element('save-VOUCHER').disabled,false);
  await f.run('saveCategory("VOUCHER")');
  assert.equal(f.calls.length,1); assert.equal(f.calls[0].options.method,'PUT');
  assert.deepEqual(JSON.parse(f.calls[0].options.body),{value:'16'});
  assert.equal(f.run('dirtySettings("VOUCHER").length'),0);
  assert.equal(f.element('status-VOUCHER').textContent,'All changes saved');
  await f.run('saveCategory("VOUCHER")'); assert.equal(f.calls.length,1);
});
test('reset is a draft and discard restores the last saved value',()=>{
  const f=fixture(); f.run('resetToDefault(1)');
  assert.equal(f.calls.length,0); assert.equal(f.element('input-1').value,'20');
  assert.equal(f.run('dirtySettings("VOUCHER").length'),1);
  f.run('discardChanges("VOUCHER")'); assert.equal(f.element('input-1').value,'10');
  assert.equal(f.run('dirtySettings("VOUCHER").length'),0);
});
test('server rejection preserves draft and allows retry',async()=>{
  const f=fixture(); f.run('handleInputChange(1,"150")'); f.replies.push({success:false,message:'Maximum is 100.'});
  await f.run('saveCategory("VOUCHER")');
  assert.equal(f.run('dirtySettings("VOUCHER").length'),1);
  assert.equal(f.element('error-1').textContent,'Maximum is 100.');
  assert.equal(f.element('input-1').attrs['aria-invalid'],'true');
  assert.equal(f.element('save-VOUCHER').disabled,false);
  f.run('handleInputChange(1,"30")'); await f.run('saveCategory("VOUCHER")');
  assert.equal(f.run('settings.VOUCHER[0].value'),'30');
});
test('partial failure reports unsaved values without resending successful fields',async()=>{
  const f=fixture();f.run('settings.VOUCHER.push({settingId:2,category:"VOUCHER",value:"5",valueType:"NUMBER"}); currentValues[2]="6"; handleInputChange(1,"20");');
  f.replies.push({success:true},{success:false,message:'Cannot update'});
  await f.run('saveCategory("VOUCHER")');
  assert.equal(f.run('dirtySettings("VOUCHER").length'),1);
  await f.run('saveCategory("VOUCHER")');
  assert.equal(f.calls.length,3); assert.equal(f.calls[2].url,'/api/admin/config/settings/2');
});
test('checkbox edits use the same explicit save flow',async()=>{
  const f=fixture();f.run('settings.VOUCHER[0].valueType="BOOLEAN"; settings.VOUCHER[0].value="false"; currentValues[1]="false";');
  f.element('input-1').type='checkbox';f.run('handleInputChange(1,"true")');
  assert.equal(f.calls.length,0);await f.run('saveCategory("VOUCHER")');
  assert.deepEqual(JSON.parse(f.calls[0].options.body),{value:'true'});
});
test('navigation warns only while there are unsaved changes',async()=>{
  const f=fixture();let prevented=0;const event={preventDefault(){prevented++;}};
  f.events.beforeunload(event);assert.equal(prevented,0);
  f.run('handleInputChange(1,"30")');f.events.beforeunload(event);assert.equal(prevented,1);
  await f.run('saveCategory("VOUCHER")');f.events.beforeunload(event);assert.equal(prevented,1);
});

function ranksFixture() {
 const f=fixture(); f.run('membershipRanks=[{rankId:1,name:"SILVER",minPoints:0,maxPoints:499},{rankId:2,name:"GOLD",minPoints:500,maxPoints:1999},{rankId:3,name:"PLATINUM",minPoints:2000,maxPoints:null}];');return f;
}
test('rank validation rejects empty, negative, fractional, exponent and oversized numbers', async()=>{
 const f=ranksFixture();for(const value of ['', '-1', '1.5', '1e3', '2147483648']){
  f.run('handleRankInput(2,'+JSON.stringify(value)+')');await f.run('updateRankThreshold(2)');
  assert.equal(f.calls.length,0);assert.equal(f.element('save-rank-2').disabled,true);
 }
});
test('rank thresholds cannot overlap previous maximum or exceed their own maximum',()=>{
 const f=ranksFixture();assert.notEqual(f.run('rankValidationMessage(2,"499")'),'');
 assert.notEqual(f.run('rankValidationMessage(2,"2000")'),'');
 assert.equal(f.run('rankValidationMessage(2,"600")'),'');
 assert.equal(f.run('rankValidationMessage(3,"2500")'),'');
});
test('saving a rank preserves another rank draft and refreshes real data',async()=>{
 const f=ranksFixture();f.run('handleRankInput(2,"600");handleRankInput(3,"2500")');
 f.replies.push({success:true},{success:true,data:[{rankId:1,name:'SILVER',minPoints:0,maxPoints:499},{rankId:2,name:'GOLD',minPoints:600,maxPoints:1999},{rankId:3,name:'PLATINUM',minPoints:2000,maxPoints:null}]});
 await f.run('updateRankThreshold(2)');
 assert.deepEqual(JSON.parse(f.calls[0].options.body),{minPoints:600});
 assert.equal(f.run('rankInputs[2]'),undefined);assert.equal(f.run('rankInputs[3]'),'2500');
 assert.equal(f.element('rank-3').disabled,false);
});
test('rank server error preserves draft and discard returns to saved threshold',async()=>{
 const f=ranksFixture();f.run('handleRankInput(2,"600")');f.replies.push({success:false,message:'Threshold changed by another user'});
 await f.run('updateRankThreshold(2)');
 assert.equal(f.run('rankInputs[2]'),'600');assert.equal(f.element('rank-error-2').textContent,'Threshold changed by another user');
 f.run('discardRank(2)');assert.equal(f.run('rankInputs[2]'),undefined);assert.equal(f.element('rank-2').value,500);
});
