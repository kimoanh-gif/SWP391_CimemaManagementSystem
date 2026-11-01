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
 assert.deepEqual(JSON.parse(f.calls[0].options.body),{minPoints:600,maxPoints:1999,discountPercent:0,discountUsageLimit:0,minBookingAmount:0,maxDiscountAmount:0});
 assert.equal(f.run('rankInputs[2]'),undefined);assert.equal(f.run('rankInputs[3]'),'2500');
 assert.equal(f.element('rank-3').disabled,false);
});
test('rank server error preserves draft and discard returns to saved threshold',async()=>{
 const f=ranksFixture();f.run('handleRankInput(2,"600")');f.replies.push({success:false,message:'Threshold changed by another user'});
 await f.run('updateRankThreshold(2)');
 assert.equal(f.run('rankInputs[2]'),'600');assert.equal(f.element('rank-error-2').textContent,'Threshold changed by another user');
 f.run('discardRank(2)');assert.equal(f.run('rankInputs[2]'),undefined);assert.equal(f.element('rank-2').value,500);
});

test('maximum validation rejects invalid integers, inverted ranges, overlaps and unlimited lower ranks', async()=>{
 const f=ranksFixture();
 for(const max of ['-1','1.5','1e3','2147483648','499','2000','']) {
  f.run('handleRankMaxInput(2,'+JSON.stringify(max)+')');
  assert.equal(f.element('save-rank-2').disabled,true,max);
  await f.run('updateRankThreshold(2)');
 }
 assert.equal(f.calls.length,0);
 f.run('handleRankMaxInput(2,"1800")');assert.equal(f.element('save-rank-2').disabled,false);
 f.run('discardRank(2)');assert.equal(f.element('rank-max-2').value,1999);
 assert.equal(f.run('rankMaxInputs[2]'),undefined);
});

test('maximum-only edits save both bounds and highest rank can become unlimited',async()=>{
 const f=ranksFixture(); f.run('handleRankMaxInput(2,"1800")');
 f.replies.push({success:true},{success:true,data:[{rankId:1,minPoints:0,maxPoints:499},{rankId:2,minPoints:500,maxPoints:1800},{rankId:3,minPoints:2000,maxPoints:null}]});
 await f.run('updateRankThreshold(2)');
 assert.deepEqual(JSON.parse(f.calls[0].options.body),{minPoints:500,maxPoints:1800,discountPercent:0,discountUsageLimit:0,minBookingAmount:0,maxDiscountAmount:0});
 assert.equal(f.run('rankMaxInputs[2]'),undefined);
 f.run('handleRankMaxInput(3,"3000");handleRankMaxInput(3,"")');
 assert.equal(f.run('rankInputs[3]'),undefined);
 f.run('handleRankMaxInput(3,"3000")');
 f.replies.push({success:false,message:'Conflict'});await f.run('updateRankThreshold(3)');
 assert.equal(f.run('rankMaxInputs[3]'),'3000');
 f.run('membershipRanks[2].maxPoints=3000;rankMaxInputs={};rankInputs={};handleRankMaxInput(3,"")');
 f.replies.push({success:true},{success:true,data:[{rankId:1,minPoints:0,maxPoints:499},{rankId:2,minPoints:500,maxPoints:1800},{rankId:3,minPoints:2000,maxPoints:null}]});
 await f.run('updateRankThreshold(3)');
 assert.deepEqual(JSON.parse(f.calls.at(-2).options.body),{minPoints:2000,maxPoints:null,discountPercent:0,discountUsageLimit:0,minBookingAmount:0,maxDiscountAmount:0});
});

test('maximum discount amount rejects fractional input and saves whole VND through the existing API', async()=>{
 const f=fixture(); f.run('settings.VOUCHER[0].settingKey="VOUCHER_MAX_DISCOUNT_AMOUNT";');
 f.element('input-1').value='1.5'; f.run('handleInputChange(1,"1.5")');
 await f.run('saveCategory("VOUCHER")');assert.equal(f.calls.length,0);
 f.element('input-1').value='100000';f.run('handleInputChange(1,"100000")');
 await f.run('saveCategory("VOUCHER")');assert.equal(f.calls.length,1);
 assert.deepEqual(JSON.parse(f.calls[0].options.body),{value:'100000'});
});


test('membership editor exposes point bounds and all discount controls',()=>{
 const f=ranksFixture();f.run('renderMembershipRanks()');
 const html=f.element('membership-ranks').innerHTML;
 assert.match(html,/Minimum points/);assert.match(html,/Maximum points/);
 assert.equal((html.match(/<th scope="col">/g)||[]).length,8);
 for(const field of ['rank-discountPercent-','discountUsageLimit','minBookingAmount','maxDiscountAmount']) assert.equal(html.includes(field),true);
});

test('policy validation rejects malformed numbers and saves enabled discount limits together',async()=>{
 const f=ranksFixture();
 for(const [field,badValues] of Object.entries({discountPercent:['','-1','100.01','1.555','1e1','Infinity'],discountUsageLimit:['','-1','1.5','2147483648'],minBookingAmount:['','-1','1.5','1000000000000'],maxDiscountAmount:['','-1','1.5','1000000000000']})) {
  for(const value of badValues) {
   f.run('handleRankPolicyInput(2,'+JSON.stringify(field)+','+JSON.stringify(value)+')');
   assert.equal(f.element('save-rank-2').disabled,true,field+' '+value);
   await f.run('updateRankThreshold(2)');
  }
  f.run('discardRank(2)');
 }
 assert.equal(f.calls.length,0);
 f.run('handleRankPolicyInput(2,"discountUsageLimit","3")');assert.equal(f.element('save-rank-2').disabled,true);
 f.run('handleRankPolicyInput(2,"discountPercent","10");handleRankPolicyInput(2,"minBookingAmount","100000");handleRankPolicyInput(2,"maxDiscountAmount","50000")');
 assert.equal(f.element('save-rank-2').disabled,false);
 f.replies.push({success:false,message:'Conflict'});await f.run('updateRankThreshold(2)');
 assert.equal(f.run('rankPolicyInputs[2].discountUsageLimit'),'3');
 f.replies.push({success:true},{success:true,data:[{rankId:1,minPoints:0,maxPoints:499},{rankId:2,minPoints:500,maxPoints:1999,discountPercent:10,discountUsageLimit:3,minBookingAmount:100000,maxDiscountAmount:50000},{rankId:3,minPoints:2000,maxPoints:null}]});
 await f.run('updateRankThreshold(2)');
 assert.deepEqual(JSON.parse(f.calls.at(-2).options.body),{minPoints:500,maxPoints:1999,discountPercent:10,discountUsageLimit:3,minBookingAmount:100000,maxDiscountAmount:50000});
 assert.equal(f.run('rankPolicyInputs[2]'),undefined);
 assert.equal(f.element('rank-discountUsageLimit-2').disabled,false);
});

test('Standard min points 0 or 1 and minimum order 0 or 1 are valid',()=>{
 const f=ranksFixture();
 for(const points of ['0','1']) {
  for(const minimum of ['0','1']) {
   f.run('handleRankPolicyInput(1,"minBookingAmount",'+JSON.stringify(minimum)+')');
   assert.equal(f.run('rankValidationMessage(1,'+JSON.stringify(points)+')'),'');
  }
 }
 f.run('membershipRanks[0].minBookingAmount="0.00";rankPolicyInputs={};');
 assert.equal(f.run('rankValidationMessage(1,"1")'),'');
});

test('minimum order errors appear on the money field rather than min points',()=>{
 const f=ranksFixture();
 f.run('handleRankPolicyInput(1,"minBookingAmount","");handleRankInput(1,"1")');
 assert.equal(f.element('rank-error-1').textContent,'');
 assert.equal(f.element('rank-1').attrs['aria-invalid'],'false');
 assert.match(f.element('rank-minBookingAmount-error-1').textContent,/Minimum order/);
 assert.equal(f.element('rank-minBookingAmount-1').attrs['aria-invalid'],'true');
 f.run('handleRankPolicyInput(1,"minBookingAmount","0")');
 assert.equal(f.element('rank-minBookingAmount-error-1').textContent,'');
 assert.equal(f.element('save-rank-1').disabled,false);
});
