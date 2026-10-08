import {firebaseConfig} from './firebase-config.js';
import {initializeApp} from 'https://www.gstatic.com/firebasejs/12.19.0/firebase-app.js';
import {getAuth,GoogleAuthProvider,signInWithPopup,signOut,onAuthStateChanged} from 'https://www.gstatic.com/firebasejs/12.19.0/firebase-auth.js';
import {getFirestore,doc,collection,getDocFromServer,getDocsFromServer,query,orderBy,limit,setDoc,serverTimestamp,runTransaction,onSnapshot} from 'https://www.gstatic.com/firebasejs/12.19.0/firebase-firestore.js';
const $=id=>document.getElementById(id),app=initializeApp(firebaseConfig),auth=getAuth(app),db=getFirestore(app);
let authorized=false,epoch=0,roleOff,base='',classes=[],assets=[],editing=null,busy=false,kind='news';
const notice=text=>{$('notice').textContent=text};
const error=e=>e.message==='conflict'?'تغيّر الموضوع عند مسؤول آخر. أعد فتحه قبل الحفظ.':e.code==='permission-denied'?window.schoolError(e):(e.code?window.schoolError(e):e.message||'تعذرت العملية. أعد المحاولة.');
const canonical=x=>x===null||typeof x!=='object'?x:Array.isArray(x)?x.map(canonical):Object.fromEntries(Object.keys(x).sort().map(k=>[k,canonical(x[k])]));
const fingerprint=s=>s.exists()?JSON.stringify(canonical(s.data())):null;
const validURL=s=>{try{const u=new URL(s);return u.protocol==='https:'&&!u.username&&!u.password}catch{return false}};
function requireAdmin(){if(!authorized||!auth.currentUser)throw Error('سجّل الدخول بحساب الإدارة');}
function cleanAssets(){for(const a of assets)if(a.preview?.startsWith('blob:'))URL.revokeObjectURL(a.preview);assets=[];}
function hideEditor(){cleanAssets();editing=null;$('editor').hidden=true;}
function endpoint(value){if(!validURL(value))throw Error('رابط خدمة الرفع غير صالح');const u=new URL(value);if(u.pathname!=='/'||u.search||u.hash)throw Error('أدخل رابط Worker الأساسي فقط');return u.origin;}
async function mediaCall(path,body,mime='application/json') {
 requireAdmin();if(!base)throw Error('أكمل إعداد خدمة الرفع أولاً');const uid=auth.currentUser.uid;
 const token=await auth.currentUser.getIdToken();
 const r=await fetch(base+path,{method:'POST',headers:{Authorization:`Bearer ${token}`,'Content-Type':mime},body});
 const data=await r.json();if(!r.ok)throw Error(data.error||`HTTP ${r.status}`);
 if(!authorized||auth.currentUser?.uid!==uid)throw Error('تغير الحساب أثناء العملية');return data;
}
async function removeAsset(asset){if(asset.publicId)await mediaCall('/delete',JSON.stringify({publicId:asset.publicId,resourceType:asset.resourceType||'image'}));}
function renderAssets(){
 $('assets').replaceChildren();assets.forEach((a,index)=>{
  const card=document.createElement('div');card.className='asset';
  if(a.mime!=='application/pdf'&&a.resourceType!=='raw') {const image=document.createElement('img');image.src=a.preview||a.url;image.alt=`الصورة ${index+1}`;card.append(image);}
  const text=document.createElement('p');text.textContent=a.name||`${a.resourceType==='raw'?'PDF':'صورة'} ${index+1}`;card.append(text);
  for(const [title,action]of [['↑',()=>{if(index){[assets[index-1],assets[index]]=[assets[index],assets[index-1]];renderAssets()}}],['↓',()=>{if(index<assets.length-1){[assets[index+1],assets[index]]=[assets[index],assets[index+1]];renderAssets()}}],['حذف',()=>{const [removed]=assets.splice(index,1);if(removed.preview?.startsWith('blob:'))URL.revokeObjectURL(removed.preview);renderAssets()}]]){
   const button=document.createElement('button');button.type='button';button.textContent=title;button.onclick=action;card.append(button);
  }$('assets').append(card);
 });
}
for(const [lang,title]of [['ar','العربية'],['fr','Français'],['en','English']]){
 const details=document.createElement('details');details.open=lang==='ar';const summary=document.createElement('summary');summary.textContent=title;details.append(summary);
 for(const [field,label]of [['title','العنوان'],['summary','ملخص'],['body','التفاصيل']]){
  const wrapper=document.createElement('label');wrapper.textContent=`${label} — ${title}`;const input=document.createElement(field==='title'?'input':'textarea');input.id=`${field}_${lang}`;input.dir=lang==='ar'?'rtl':'ltr';input.maxLength=field==='body'?20000:field==='summary'?1000:180;input.required=lang==='ar'&&field==='title';wrapper.append(input);details.append(wrapper);
 }$('translations').append(details);
}
async function load(){
 requireAdmin();const current=epoch,selectedKind=kind;
 const result=await getDocsFromServer(query(collection(db,kind),orderBy('publishedAt','desc'),limit(100)));
 if(current!==epoch||selectedKind!==kind)return;
 $('records').replaceChildren();
 if(result.empty){$('records').textContent='لا توجد مواضيع بعد.';return;}
 for(const snap of result.docs){const data=snap.data(),article=document.createElement('article'),title=document.createElement('strong');title.textContent=typeof data.title==='string'?data.title:data.title?.ar||data.title?.fr||'';article.append(title);const count=document.createElement('small');count.textContent=`${(data.images||[]).length} صور${data.pdf?' · PDF':''}`;article.append(count);const state=document.createElement('p');state.textContent=data.status==='published'?'منشور':'مسودة';state.className='badge';article.append(state);const edit=document.createElement('button');edit.textContent='تعديل';edit.disabled=false;edit.onclick=()=>openEditor(snap).catch(e=>notice(error(e)));article.append(edit);$('records').append(article);}
}
async function openEditor(snap=null){
 if(busy)return;requireAdmin();hideEditor();const current=epoch;
 if(snap)snap=await getDocFromServer(snap.ref);
 if(current!==epoch)return;
 const data=snap?.data()||{};editing={snap,kind,old:[...(data.images||[]),...(data.pdf?[data.pdf]:[])]};
 for(const field of ['title','summary','body'])for(const lang of ['ar','fr','en'])$(field+'_'+lang).value=typeof data[field]==='string'?(lang==='ar'?data[field]:''):(data[field]?.[lang]||'');
 for(const field of ['category','status','sourceUrl','deadline'])$(field).value=data[field]||(field==='category'?'schools':field==='status'?'draft':'');
 $('class-fields').hidden=kind!=='timetables';$('category').parentElement.hidden=kind!=='guidance';
 $('classId').value=data.classId||classes[0]?.id||'';
 assets=(data.images||[]).map(a=>({...a}));if(data.pdf)assets.push({...data.pdf,resourceType:'raw'});
 $('files').value='';renderAssets();$('editor').hidden=false;$('editor').scrollIntoView({behavior:'smooth'});
}
$('files').onchange=async()=>{
 try{for(const file of $('files').files){
  if(file.size>8*1024*1024)throw Error('الحد الأقصى للملف 8 MB');
  if(!['image/jpeg','image/png','image/webp','application/pdf'].includes(file.type))throw Error('صيغة غير مدعومة');
  const pdf=file.type==='application/pdf';
  if(pdf&&(kind!=='timetables'||assets.some(a=>a.mime==='application/pdf'||a.resourceType==='raw')))throw Error('PDF واحد مسموح فقط لاستعمال الزمن');
  if(!pdf&&assets.filter(a=>a.mime!=='application/pdf'&&a.resourceType!=='raw').length>=20)throw Error('الحد الأقصى 20 صورة');
  assets.push({file,mime:file.type,name:file.name,preview:URL.createObjectURL(file)});
 }}catch(e){notice(error(e))}finally{$('files').value='';renderAssets()}
};
async function compress(file){
 if(file.type==='application/pdf')return file;
 const bitmap=await createImageBitmap(file);try{
 const scale=Math.min(1,2000/Math.max(bitmap.width,bitmap.height));const canvas=document.createElement('canvas');canvas.width=Math.max(1,Math.round(bitmap.width*scale));canvas.height=Math.max(1,Math.round(bitmap.height*scale));const ctx=canvas.getContext('2d');ctx.fillStyle='#fff';ctx.fillRect(0,0,canvas.width,canvas.height);ctx.drawImage(bitmap,0,0,canvas.width,canvas.height);
 return await new Promise((resolve,reject)=>canvas.toBlob(b=>b?resolve(b):reject(Error('تعذر ضغط الصورة')),'image/jpeg',.84));
 }finally{bitmap.close()}
}
$('form').onsubmit=async event=>{
 event.preventDefault();if(busy||!editing)return;
 const current=epoch,task=editing;const uploaded=[];let committed=false,commitAttempted=false,targetRef=null;
 try{
  requireAdmin();const data={};
  for(const field of ['title','summary','body'])data[field]=Object.fromEntries(['ar','fr','en'].map(lang=>[lang,$(field+'_'+lang).value.trim()]));
  data.status=$('status').value;data.category=$('category').value;data.sourceUrl=$('sourceUrl').value.trim();data.deadline=$('deadline').value;
  if(data.sourceUrl&&!validURL(data.sourceUrl))throw Error('المصدر يجب أن يكون HTTPS');
  if(task.kind==='timetables'){
   const selected=classes.find(c=>c.id===$('classId').value);if(!selected)throw Error('اختر القسم');
   Object.assign(data,{classId:selected.id,className:selected.name,academicYear:selected.academicYear});
  }
  if(!assets.length&&task.kind==='timetables')throw Error('أضف صورة أو PDF');
  busy=true;$('edit-fields').disabled=true;$('progress').hidden=false;$('progress').value=0;
  const snapshot=[...assets],saved=[];
  for(let i=0;i<snapshot.length;i++){
   let asset=snapshot[i];
   if(asset.file){const blob=await compress(asset.file);asset=await mediaCall('/upload?kind='+task.kind,blob,blob.type);uploaded.push(asset);}
   saved.push({url:asset.url,...(asset.publicId?{publicId:asset.publicId}:{}),resourceType:asset.resourceType||'image',...(asset.thumbnailUrl?{thumbnailUrl:asset.thumbnailUrl}:{})});
   $('progress').value=Math.round((i+1)/Math.max(1,snapshot.length)*90);
   if(current!==epoch)throw Error('انتهت الجلسة');
  }
  data.images=saved.filter(a=>a.resourceType!=='raw');data.pdf=saved.find(a=>a.resourceType==='raw')||null;
  data.updatedAt=serverTimestamp();data.updatedBy=auth.currentUser.uid;
  if(current!==epoch)throw Error('انتهت الجلسة');requireAdmin();
  const ref=task.snap?.ref||doc(collection(db,task.kind));
  targetRef=ref;commitAttempted=true;
  await runTransaction(db,async tx=>{
   const fresh=await tx.get(ref),old=task.snap;
   if(old){if(!fresh.exists()||fingerprint(fresh)!==fingerprint(old))throw Error('conflict');}
   else if(fresh.exists())throw Error('conflict');
   data.publishedAt=(data.status==='published' && fresh.data()?.status!=='published')?serverTimestamp():(fresh.data()?.publishedAt||serverTimestamp());
   tx.set(ref,data,{merge:true});
  });committed=true;
  const retained=new Set(saved.map(a=>a.publicId));let cleanupFailed=false;
  for(const old of task.old)if(old.publicId&&!retained.has(old.publicId))try{await removeAsset(old)}catch{cleanupFailed=true}
  if(current===epoch){hideEditor();notice(cleanupFailed?'تم الحفظ. بقيت ملفات قديمة تحتاج تنظيفاً من Cloudinary.':'تم الحفظ بنجاح.');await load();}
 }catch(e){
  // Only this attempt's uncommitted uploads are rolled back; existing live images stay intact.
  if(!committed) {
   let safeToRemove=!commitAttempted;
   if(commitAttempted&&targetRef)try{
    const check=await getDocFromServer(targetRef);
    const data=check.data()||{};const attached=[...(data.images||[]),...(data.pdf?[data.pdf]:[])];
    safeToRemove=!uploaded.some(a=>attached.some(b=>b.publicId===a.publicId));
   }catch{ /* Ambiguous commit: retain uploads rather than break a published record. */ }
   if(safeToRemove)for(const asset of uploaded)try{await removeAsset(asset)}catch{}
  }
  notice(error(e));
 }finally{busy=false;$('edit-fields').disabled=false;$('progress').hidden=true;}
};
$('signin').onclick=async()=>{try{const p=new GoogleAuthProvider();p.setCustomParameters({prompt:'select_account'});await signInWithPopup(auth,p)}catch(e){notice(error(e))}};
$('signout').onclick=()=>{if(!busy)signOut(auth)};
$('new').onclick=()=>openEditor().catch(e=>notice(error(e)));
$('refresh').onclick=()=>{if(!busy)load().catch(e=>notice(error(e)))};
$('cancel').onclick=hideEditor;
const requestedSection=new URLSearchParams(location.search).get('section');
if(['news','guidance','timetables'].includes(requestedSection)){kind=requestedSection;$('collection').value=kind;}
$('collection').onchange=()=>{if(busy){$('collection').value=kind;return;}kind=$('collection').value;hideEditor();load().catch(e=>notice(error(e)))};
$('save-endpoint').onclick=async()=>{try{requireAdmin();base=endpoint($('endpoint').value.trim());await setDoc(doc(db,'schoolConfig','media'),{uploadBaseUrl:base},{merge:true});notice('تم حفظ خدمة الرفع.')}catch(e){notice(error(e))}};

onAuthStateChanged(auth,async user=>{
 const current=++epoch;authorized=false;roleOff?.();hideEditor();$('admin').hidden=true;$('login').hidden=false;
 if(!user)return;
 try{
  const roleRef=doc(db,'roles',user.uid),role=await getDocFromServer(roleRef);
  if(current!==epoch)return;
  if(!role.exists()||role.data().active!==true||!['admin','director'].includes(role.data().role))throw Error('الحساب غير مخوّل للإدارة');
  authorized=true;roleOff=onSnapshot(roleRef,s=>{if(!s.exists()||s.data().active!==true||!['admin','director'].includes(s.data().role)){authorized=false;epoch++;hideEditor();$('admin').hidden=true;notice('تم إيقاف الصلاحية')}},()=>{authorized=false;epoch++;$('admin').hidden=true;hideEditor()});
  const [media,catalog]=await Promise.all([getDocFromServer(doc(db,'schoolConfig','media')),getDocsFromServer(collection(db,'classes'))]);
  if(current!==epoch||!authorized)return;
  base=media.exists()?endpoint(media.data().uploadBaseUrl):'';$('endpoint').value=base;
  classes=catalog.docs.map(d=>({id:d.id,...d.data()})).filter(c=>c.active===true);$('classId').replaceChildren();
  classes.forEach(c=>{const o=document.createElement('option');o.value=c.id;o.textContent=`${c.name} · ${c.academicYear}`;$('classId').append(o)});
  $('account').textContent=user.email;$('login').hidden=true;$('admin').hidden=false;notice('');await load();
 }catch(e){notice(error(e))}
});
