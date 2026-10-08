const test=require('node:test'); const assert=require('node:assert/strict');const {publicationMessage:p}=require('./publication');
test('all publishing collections emit a generic alert',()=>{for(const kind of ['news','guidance','timetables'])assert.ok(p(kind,null,{status:'published'}));});
test('drafts edits and deletes never send',()=>{assert.equal(p('news',null,{status:'draft'}),null);assert.equal(p('news',{status:'published'},{status:'published'}),null);assert.equal(p('news',{status:'published'},null),null);});
test('publication from draft sends but private collections do not',()=>{assert.ok(p('guidance',{status:'draft'},{status:'published'}));assert.equal(p('users',null,{status:'published'}),null);});
test('student content is never included',()=>{assert.ok(!JSON.stringify(p('timetables',null,{status:'published',title:'PRIVATE',classId:'SECRET'})).includes('PRIVATE'));});
