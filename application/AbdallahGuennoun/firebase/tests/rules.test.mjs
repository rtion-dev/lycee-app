import fs from 'node:fs';
import {initializeTestEnvironment,assertFails,assertSucceeds} from '@firebase/rules-unit-testing';
import {doc,setDoc,getDoc,getDocs,collection,query,where,writeBatch,serverTimestamp,updateDoc,deleteDoc,runTransaction} from 'firebase/firestore';
const env=await initializeTestEnvironment({projectId:'demo-rtion-documents',firestore:{host:'127.0.0.1',port:8188,rules:fs.readFileSync(new URL('../firestore.rules',import.meta.url),'utf8')}});
let count=0;
const test=async(name,fn)=>{await fn();console.log('PASS '+(++count)+' '+name)};
const db=u=>env.authenticatedContext(u).firestore();
const director=db('director'),staff=db('staff'),student=db('student'),other=db('other'),pending=db('pending');
const type={name:'شهادة مدرسية',active:true,fields:['nameAr','nameLatin','birthDate','massarId','classId','ordinalNumber'],updatedBy:'director',updatedAt:serverTimestamp()};
async function create(database,id,overrides={},eventOverride={},audit=true){const r=doc(database,'documentRequests',id),e=doc(r,'events','created');const b=writeBatch(database);b.set(r,{uid:'student',typeId:'school',typeName:type.name,fields:type.fields,answers:{nameAr:'تلميذ تجريبي',nameLatin:'Test Student',birthDate:'2009-02-15',massarId:'A123456789',classId:'class',ordinalNumber:7},status:'submitted',note:'',revision:1,lastEventId:e.id,createdAt:serverTimestamp(),updatedAt:serverTimestamp(),...overrides});if(audit)b.set(e,{actorUid:'student',at:serverTimestamp(),from:'',to:'submitted',note:'',revision:1,...eventOverride});return b.commit();}
async function advance(database,id,from,to,rev,actor='staff',note=''){const r=doc(database,'documentRequests',id),e=doc(r,'events','event'+rev);const b=writeBatch(database);b.update(r,{status:to,note,revision:rev,lastEventId:e.id,updatedAt:serverTimestamp()});b.set(e,{actorUid:actor,at:serverTimestamp(),from,to,note,revision:rev});return b.commit();}
try{
await env.clearFirestore();
await env.withSecurityRulesDisabled(async c=>{const d=c.firestore();await Promise.all([
setDoc(doc(d,'roles','director'),{role:'director',active:true}),setDoc(doc(d,'users','student'),{status:'approved'}),setDoc(doc(d,'users','other'),{status:'approved'}),setDoc(doc(d,'users','pending'),{status:'pending'}),setDoc(doc(d,'classes','class'),{name:'TC1',active:true,academicYear:'2026-2027'}),setDoc(doc(d,'schoolConfig','catalog'),{academicYear:'2026-2027'})]);});
await test('director configures a dynamic document type',()=>assertSucceeds(setDoc(doc(director,'documentTypes','school'),type)));
await test('student cannot configure document types',()=>assertFails(setDoc(doc(student,'documentTypes','bad'),{...type,updatedBy:'student'})));
await test('reject unknown requested fields',()=>assertFails(setDoc(doc(director,'documentTypes','bad'),{...type,fields:['password']})));
await test('director delegates to account without general admin role',()=>assertSucceeds(setDoc(doc(director,'documentStaff','staff'),{active:true,label:'مسؤول الوثائق',updatedBy:'director',updatedAt:serverTimestamp()})));
await test('staff cannot grant permissions',()=>assertFails(setDoc(doc(staff,'documentStaff','other'),{active:true,label:'Other',updatedBy:'staff',updatedAt:serverTimestamp()})));
await test('student can read missing request for idempotent transaction',()=>assertSucceeds(getDoc(doc(student,'documentRequests','new'))));
await test('valid request and audit are accepted atomically',()=>assertSucceeds(create(student,'good')));
await test('request without audit is denied',()=>assertFails(create(student,'no-audit',{}, {},false)));
await test('forged audit actor is denied',()=>assertFails(create(student,'forged',{}, {actorUid:'director'})));
await test('impersonating another student is denied',()=>assertFails(create(other,'impersonate')));
await test('pending student cannot submit',()=>assertFails(create(pending,'pending',{uid:'pending'},{actorUid:'pending'})));
await test('missing required answers denied',()=>assertFails(create(student,'missing',{answers:{nameAr:'Test'}})));
await test('extra answer denied',()=>assertFails(create(student,'extra',{answers:{nameAr:'Test',secret:'x'}})));
await test('student queries own requests',()=>assertSucceeds(getDocs(query(collection(student,'documentRequests'),where('uid','==','student')))));
await test('student cannot read another request',()=>assertFails(getDoc(doc(other,'documentRequests','good'))));
await test('staff query works without roles document',()=>assertSucceeds(getDocs(query(collection(staff,'documentRequests'),where('status','==','submitted')))));
await test('student cannot change status',()=>assertFails(advance(student,'good','submitted','preparing',2,'student')));
await test('cannot skip stages',()=>assertFails(advance(staff,'good','submitted','delivered',2)));
await test('cannot update without event',()=>assertFails(updateDoc(doc(staff,'documentRequests','good'),{status:'preparing',revision:2,updatedAt:serverTimestamp()})));
await test('staff accepts and prepares',()=>assertSucceeds(advance(staff,'good','submitted','preparing',2)));
await test('staff marks ready',()=>assertSucceeds(advance(staff,'good','preparing','ready',3)));
await test('staff confirms physical delivery',()=>assertSucceeds(advance(staff,'good','ready','delivered',4)));
await test('terminal request cannot reopen',()=>assertFails(advance(staff,'good','delivered','submitted',5)));
await test('audit cannot be changed',()=>assertFails(updateDoc(doc(director,'documentRequests','good','events','created'),{note:'tampered'})));
await test('audit cannot be deleted',()=>assertFails(deleteDoc(doc(director,'documentRequests','good','events','created'))));
await test('student cannot rewrite answers',()=>assertFails(updateDoc(doc(student,'documentRequests','good'),{'answers.nameAr':'Changed'})));
await test('second request can be submitted',()=>assertSucceeds(create(student,'reject')));
await test('rejection requires reason',()=>assertFails(advance(staff,'reject','submitted','rejected',2)));
await test('rejection with reason accepted',()=>assertSucceeds(advance(staff,'reject','submitted','rejected',2,'staff','المعلومات غير مطابقة')));
await test('director revokes delegate',()=>assertSucceeds(updateDoc(doc(director,'documentStaff','staff'),{active:false,updatedAt:serverTimestamp()})));
await test('revoked delegate cannot read requests',()=>assertFails(getDoc(doc(staff,'documentRequests','good'))));
await test('document type can be disabled',()=>assertSucceeds(updateDoc(doc(director,'documentTypes','school'),{active:false,updatedAt:serverTimestamp()})));
await test('disabled type cannot receive new requests',()=>assertFails(create(student,'disabled')));
console.log('TOTAL '+count+' PASS');
}finally{await env.cleanup();}
